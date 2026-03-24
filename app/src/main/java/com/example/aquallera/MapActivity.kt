package com.example.aquallera

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.CoordinateBounds
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MapActivity : AppCompatActivity() {

    private lateinit var waterStationName: TextView
    private lateinit var viewDetailsBtn: Button
    private lateinit var mapView: MapView
    private lateinit var btnHowToOrder: Button
    private lateinit var recyclerViewStations: RecyclerView
    private lateinit var selectedStationInfo: LinearLayout
    private lateinit var emptyState: LinearLayout

    private var selectedStation: WaterStation? = null
    private lateinit var stationAdapter: WaterStationAdapter
    private val stationsList = mutableListOf<WaterStation>()

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var userLocation: Location? = null

    private val markerDataList = mutableListOf<MarkerData>()
    private var currentZoom = MIN_ZOOM
    private var pointAnnotationManager: PointAnnotationManager? = null

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001

        private const val MIN_ZOOM = 11.0
        private const val MAX_ZOOM = 18.0

        private val BAGUIO_BOUNDS = CoordinateBounds(
            Point.fromLngLat(120.5200, 16.3600),
            Point.fromLngLat(120.6700, 16.4600)
        )
        private val BAGUIO_CENTER = Point.fromLngLat(120.5931, 16.4164)
    }

    data class MarkerData(
        val station: WaterStation,
        val annotationId: String
    )

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        initializeViews()
        setupRecyclerView()
        setupMap()
        setupClickListeners()
        setupBottomNavigation()
        getUserLocation()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    // -------------------------------------------------------------------------
    // Setup
    // -------------------------------------------------------------------------

    private fun initializeViews() {
        waterStationName = findViewById(R.id.waterStationName)
        viewDetailsBtn = findViewById(R.id.viewDetailsBtn)
        mapView = findViewById(R.id.mapView)
        btnHowToOrder = findViewById(R.id.btnHowToOrder)
        recyclerViewStations = findViewById(R.id.recyclerViewStations)
        selectedStationInfo = findViewById(R.id.selectedStationInfo)
        emptyState = findViewById(R.id.emptyState)

        selectedStationInfo.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        stationAdapter = WaterStationAdapter(
            stations = stationsList,
            userLocation = userLocation,
            onViewOnMapClick = { station ->
                selectedStation = station
                zoomToStation(station)
                showSelectedStationInfo(station)
            }
        )

        recyclerViewStations.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = stationAdapter
            isNestedScrollingEnabled = true
            setHasFixedSize(false)
        }
    }

    private fun setupMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            addCustomIconsToStyle(style)

            mapView.getMapboxMap().setBounds(
                CameraBoundsOptions.Builder()
                    .bounds(BAGUIO_BOUNDS)
                    .minZoom(MIN_ZOOM)
                    .maxZoom(MAX_ZOOM)
                    .build()
            )

            mapView.getMapboxMap().setCamera(
                CameraOptions.Builder()
                    .center(BAGUIO_CENTER)
                    .zoom(MIN_ZOOM)
                    .build()
            )

            mapView.getMapboxMap().addOnCameraChangeListener {
                val newZoom = mapView.getMapboxMap().cameraState.zoom
                if (kotlin.math.abs(newZoom - currentZoom) > 0.5) {
                    currentZoom = newZoom
                    updateMarkerSizes()
                }
            }

            if (hasLocationPermission()) {
                enableLocationTracking()
            } else {
                requestLocationPermission()
            }

            fetchWaterStations()
        }
    }

    private fun setupClickListeners() {
        btnHowToOrder.setOnClickListener { showHowToOrderDialog() }

        viewDetailsBtn.setOnClickListener {
            val station = selectedStation
            if (station != null) {
                val intent = Intent(this, StoreDetailsActivity::class.java)
                    .putExtra("WATER_STATION", station)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a water station first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setActiveTab(navMap)

        navMap.setOnClickListener { setActiveTab(navMap) }

        navOrders.setOnClickListener {
            startActivity(Intent(this, OrdersActivity::class.java))
            finish()
        }

        navProfile.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
            finish()
        }
    }

    // -------------------------------------------------------------------------
    // Location
    // -------------------------------------------------------------------------

    private fun getUserLocation() {
        if (!hasLocationPermission()) return
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    userLocation = location
                    stationAdapter.updateUserLocation(location)
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableLocationTracking() {
        try {
            mapView.location.updateSettings {
                enabled = true
                pulsingEnabled = false
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Could not enable location tracking", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
                enableLocationTracking()
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Map markers
    // -------------------------------------------------------------------------

    private fun iconSizeForZoom(zoom: Double) = when {
        zoom < 10 -> 0.5
        zoom < 12 -> 0.8
        zoom < 14 -> 1.0
        zoom < 16 -> 1.2
        else -> 1.5
    }

    private fun textSizeForZoom(zoom: Double) = when {
        zoom < 11 -> 0.0
        zoom < 13 -> 8.0
        zoom < 15 -> 10.0
        else -> 12.0
    }

    private fun addCustomIconsToStyle(style: Style) {
        val drawable = ContextCompat.getDrawable(this, R.drawable.ic_water_drop) ?: return
        val bitmap = drawableToBitmap(drawable, 96, 96)
        style.addImage("water_station_icon", bitmap, false)
    }

    private fun drawableToBitmap(drawable: Drawable, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, width, height)
        drawable.draw(canvas)
        return bitmap
    }

    private fun getOrCreateAnnotationManager(): PointAnnotationManager {
        return pointAnnotationManager ?: mapView.annotations
            .createPointAnnotationManager()
            .also { manager ->
                pointAnnotationManager = manager
                manager.addClickListener { clicked ->
                    val markerData = markerDataList.find { it.annotationId == clicked.id.toString() }
                    markerData?.let {
                        selectedStation = it.station
                        showSelectedStationInfo(it.station)
                        zoomToStation(it.station)
                    }
                    true
                }
            }
    }

    private fun addWaterStationMarker(station: WaterStation) {
        if (station.latitude == 0.0 || station.longitude == 0.0) return

        val manager = getOrCreateAnnotationManager()
        val annotation = manager.create(
            PointAnnotationOptions()
                .withPoint(Point.fromLngLat(station.longitude, station.latitude))
                .withIconImage("water_station_icon")
                .withIconSize(iconSizeForZoom(currentZoom))
                .withTextField(station.stationName)
                .withTextSize(textSizeForZoom(currentZoom))
                .withTextColor("#424242")
                .withTextHaloColor("#00000000")
                .withTextHaloWidth(0.0)
                .withTextOffset(listOf(0.0, -3.5))
        )

        markerDataList.add(MarkerData(station, annotation.id.toString()))
    }

    private fun updateMarkerSizes() {
        val manager = pointAnnotationManager ?: return
        val iconSize = iconSizeForZoom(currentZoom)
        val textSize = textSizeForZoom(currentZoom)

        manager.annotations.forEach { annotation ->
            annotation.iconSize = iconSize
            annotation.textSize = textSize
        }
        manager.update(manager.annotations)
    }

    private fun zoomToStation(station: WaterStation) {
        mapView.getMapboxMap().setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(station.longitude, station.latitude))
                .zoom(15.0)
                .build()
        )
    }

    private fun showSelectedStationInfo(station: WaterStation) {
        waterStationName.text = station.stationName
        selectedStationInfo.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------------
    // Firebase
    // -------------------------------------------------------------------------

    private fun fetchWaterStations() {
        val waterStationsRef = FirebaseConfig.getDatabaseReference().child("waterStations")

        waterStationsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                stationsList.clear()
                markerDataList.clear()
                pointAnnotationManager?.deleteAll()
                pointAnnotationManager = null

                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val id = child.key ?: continue
                        val station = child.getValue(WaterStation::class.java)?.copy(id = id) ?: continue
                        stationsList.add(station)
                        addWaterStationMarker(station)
                    }
                    updateMarkerSizes()
                }

                val hasStations = stationsList.isNotEmpty()
                emptyState.visibility = if (hasStations) View.GONE else View.VISIBLE
                recyclerViewStations.visibility = if (hasStations) View.VISIBLE else View.GONE

                stationAdapter.updateStations(stationsList)
            }

            override fun onCancelled(error: DatabaseError) {
                emptyState.visibility = View.VISIBLE
                recyclerViewStations.visibility = View.GONE
                Toast.makeText(
                    this@MapActivity,
                    "Failed to load stations: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private fun showHowToOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_how_to_order)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.findViewById<Button>(R.id.btnCloseDialog).setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val tabs = mapOf(
            R.id.navMap to R.id.tvMap,
            R.id.navOrder to R.id.tvOrder,
            R.id.navProfile to R.id.tvProfile
        )

        tabs.forEach { (tabId, textId) ->
            val tab = findViewById<LinearLayout>(tabId)
            val text = findViewById<TextView>(textId)
            val isActive = tab == activeTab

            text.setTextColor(Color.parseColor(if (isActive) "#2196F3" else "#757575"))
            tab.background = ContextCompat.getDrawable(
                this,
                if (isActive) R.drawable.tab_active_border else R.drawable.tab_border_highlight
            )
        }
    }
}