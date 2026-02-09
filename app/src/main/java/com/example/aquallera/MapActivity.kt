package com.example.aquallera

import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.util.Log
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
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.locationcomponent.location

class MapActivity : AppCompatActivity() {

    private lateinit var waterStation: TextView
    private lateinit var viewDetails: Button
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        private const val TAG = "MapActivity"
    }

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

    private fun initializeViews() {
        waterStation = findViewById(R.id.waterStationName)
        viewDetails = findViewById(R.id.viewDetailsBtn)
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
                zoomToStation(station)
                selectedStation = station
                showSelectedStationInfo(station)
            }
        )

        recyclerViewStations.apply {
            layoutManager = LinearLayoutManager(this@MapActivity)
            adapter = stationAdapter
            // ✅ ENABLE scrolling - RecyclerView scrolls independently
            isNestedScrollingEnabled = true
            setHasFixedSize(false) // Allow dynamic height
        }
    }

    private fun getUserLocation() {
        try {
            if (hasLocationPermission()) {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    location?.let {
                        userLocation = it
                        stationAdapter.updateUserLocation(it)
                        Log.d(TAG, "User location: ${it.latitude}, ${it.longitude}")
                    } ?: run {
                        Log.w(TAG, "User location is null")
                    }
                }
            } else {
                Log.w(TAG, "Location permission not granted")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Location permission error: ${e.message}")
        }
    }

    private fun setupMap() {
        mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) { style ->
            Log.d(TAG, "Map style loaded successfully")

            addCustomIconsToStyle(style)

            val defaultLocation = Point.fromLngLat(120.5931, 16.4023)
            val cameraOptions = CameraOptions.Builder()
                .center(defaultLocation)
                .zoom(12.0)
                .build()
            mapView.getMapboxMap().setCamera(cameraOptions)

            if (!hasLocationPermission()) {
                requestLocationPermission()
            } else {
                enableLocationTracking()
            }

            fetchWaterStations()
        }
    }

    private fun addCustomIconsToStyle(style: Style) {
        try {
            val waterStationDrawable = ContextCompat.getDrawable(this, R.drawable.icons8_location_pin_30_1)
            waterStationDrawable?.let {
                val waterStationBitmap = drawableToBitmap(it)
                style.addImage("water_station_icon", waterStationBitmap, false)
                Log.d(TAG, "Water station icon added to style")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Icon error: ${e.message}", e)
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        return if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        } else {
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun addWaterStationMarker(station: WaterStation) {
        try {
            val lat = station.latitude
            val lng = station.longitude

            if (lat == 0.0 || lng == 0.0) {
                Log.w(TAG, "Skipping station ${station.stationName} - invalid coordinates")
                return
            }

            val stationLocation = Point.fromLngLat(lng, lat)

            val annotationApi = mapView.annotations
            val pointAnnotationManager = annotationApi.createPointAnnotationManager()

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(stationLocation)
                .withIconImage("water_station_icon")
                .withIconSize(1.0)
                .withTextField(station.stationName)
                .withTextSize(10.0)
                .withTextColor("#000000")
                .withTextHaloColor("#FFFFFF")
                .withTextHaloWidth(1.0)

            pointAnnotationManager.create(pointAnnotationOptions)

            pointAnnotationManager.addClickListener { annotation ->
                selectedStation = station
                showSelectedStationInfo(station)
                zoomToStation(station)
                true
            }

            Log.d(TAG, "Marker added for: ${station.stationName}")

        } catch (e: Exception) {
            Log.e(TAG, "Error adding marker for ${station.stationName}: ${e.message}", e)
        }
    }

    private fun zoomToStation(station: WaterStation) {
        val stationLocation = Point.fromLngLat(station.longitude, station.latitude)
        val cameraOptions = CameraOptions.Builder()
            .center(stationLocation)
            .zoom(15.0)
            .build()
        mapView.getMapboxMap().setCamera(cameraOptions)
    }

    private fun showSelectedStationInfo(station: WaterStation) {
        waterStation.text = station.stationName
        selectedStationInfo.visibility = View.VISIBLE
    }

    private fun fetchWaterStations() {
        try {
            Log.d(TAG, "🔄 Starting to fetch water stations...")

            val database = FirebaseConfig.getDatabaseReference()
            val waterStationsRef = database.child("waterStations")

            waterStationsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    Log.d(TAG, "📦 Firebase data received")
                    Log.d(TAG, "DataSnapshot exists: ${dataSnapshot.exists()}")
                    Log.d(TAG, "DataSnapshot children count: ${dataSnapshot.childrenCount}")

                    stationsList.clear()

                    if (dataSnapshot.exists()) {
                        for (stationSnapshot in dataSnapshot.children) {
                            try {
                                val stationId = stationSnapshot.key ?: ""
                                Log.d(TAG, "Processing station ID: $stationId")

                                val stationData = stationSnapshot.value
                                Log.d(TAG, "Station data: $stationData")

                                val station = stationSnapshot.getValue(WaterStation::class.java)

                                if (station != null) {
                                    val stationWithId = station.copy(id = stationId)

                                    Log.d(TAG, "✅ Station parsed: ${stationWithId.stationName}")
                                    Log.d(TAG, "   - Status: ${stationWithId.status}")
                                    Log.d(TAG, "   - Online: ${stationWithId.isOnline}")
                                    Log.d(TAG, "   - Lat: ${stationWithId.latitude}, Lng: ${stationWithId.longitude}")
                                    Log.d(TAG, "   - Address: ${stationWithId.address}")

                                    stationsList.add(stationWithId)
                                    addWaterStationMarker(stationWithId)
                                } else {
                                    Log.e(TAG, "❌ Failed to parse station $stationId")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "❌ Error parsing station: ${e.message}", e)
                                e.printStackTrace()
                            }
                        }

                        Log.d(TAG, "📊 Total stations loaded: ${stationsList.size}")

                        stationAdapter.updateStations(stationsList)

                        if (stationsList.isEmpty()) {
                            emptyState.visibility = View.VISIBLE
                            recyclerViewStations.visibility = View.GONE
                            Log.w(TAG, "⚠️ No stations to display")
                        } else {
                            emptyState.visibility = View.GONE
                            recyclerViewStations.visibility = View.VISIBLE
                            Log.d(TAG, "✅ Displaying ${stationsList.size} stations")
                        }
                    } else {
                        emptyState.visibility = View.VISIBLE
                        recyclerViewStations.visibility = View.GONE
                        Log.w(TAG, "⚠️ No stations found in Firebase")
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "❌ Firebase error: ${databaseError.message}")
                    Log.e(TAG, "Error code: ${databaseError.code}")
                    Log.e(TAG, "Error details: ${databaseError.details}")

                    emptyState.visibility = View.VISIBLE
                    recyclerViewStations.visibility = View.GONE

                    Toast.makeText(
                        this@MapActivity,
                        "Failed to load stations: ${databaseError.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            })

        } catch (e: Exception) {
            Log.e(TAG, "❌ Error fetching stations: ${e.message}", e)
            e.printStackTrace()

            emptyState.visibility = View.VISIBLE
            recyclerViewStations.visibility = View.GONE

            Toast.makeText(
                this,
                "Error loading stations: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun enableLocationTracking() {
        try {
            val locationComponentPlugin = mapView.location
            locationComponentPlugin.updateSettings {
                this.enabled = true
                this.pulsingEnabled = true
                this.pulsingColor = Color.BLUE
                this.pulsingMaxRadius = 20.0f
            }

            Log.d(TAG, "Real-time location tracking enabled")

        } catch (e: Exception) {
            Log.e(TAG, "Error enabling location tracking: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableLocationTracking()
                getUserLocation()
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupClickListeners() {
        btnHowToOrder.setOnClickListener {
            showHowToOrderDialog()
        }

        viewDetails.setOnClickListener {
            selectedStation?.let { station ->
                val intent = Intent(this, StoreDetailsActivity::class.java)
                intent.putExtra("WATER_STATION", station)
                startActivity(intent)
            } ?: run {
                Toast.makeText(this, "Please select a water station first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showHowToOrderDialog() {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_how_to_order)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnClose = dialog.findViewById<Button>(R.id.btnCloseDialog)
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        setActiveTab(navMap)

        navMap.setOnClickListener {
            setActiveTab(navMap)
        }

        navOrders.setOnClickListener {
            val intent = Intent(this, OrdersActivity::class.java)
            startActivity(intent)
            finish()
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun setActiveTab(activeTab: LinearLayout) {
        val allTabs = listOf(
            findViewById<LinearLayout>(R.id.navMap),
            findViewById<LinearLayout>(R.id.navOrder),
            findViewById<LinearLayout>(R.id.navProfile)
        )

        allTabs.forEach { tab ->
            val textView = when (tab.id) {
                R.id.navMap -> findViewById<TextView>(R.id.tvMap)
                R.id.navOrder -> findViewById<TextView>(R.id.tvOrder)
                R.id.navProfile -> findViewById<TextView>(R.id.tvProfile)
                else -> null
            }

            if (tab == activeTab) {
                textView?.setTextColor(Color.parseColor("#2196F3"))
                tab.background = ContextCompat.getDrawable(this, R.drawable.tab_active_border)
            } else {
                textView?.setTextColor(Color.parseColor("#757575"))
                tab.background = ContextCompat.getDrawable(this, R.drawable.tab_border_highlight)
            }
        }
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
}