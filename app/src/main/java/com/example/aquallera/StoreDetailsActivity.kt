package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class StoreDetailsActivity : AppCompatActivity() {

    private lateinit var tvStoreName: TextView
    private lateinit var tvAddress: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvHours: TextView
    private lateinit var tvServices: TextView
    private lateinit var tvPrices: TextView
    private lateinit var btnOrder: Button
    private lateinit var btnReturn: Button
    private lateinit var pricesCard: CardView

    private lateinit var deliveryHoursSection: LinearLayout
    private lateinit var tvDeliveryHoursLabel: TextView
    private lateinit var deliveryTimesContainer: LinearLayout

    private var currentStation: WaterStation? = null

    // Keys that should always appear first, in this order
    private val HOURS_KEY_ORDER = listOf("open", "opening", "start", "close", "closing", "end")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_details)

        initializeViews()
        setupClickListeners()

        val station = intent.getSerializableExtra("WATER_STATION") as? WaterStation
        if (station != null) {
            currentStation = station
            displayStationDetails(station)
        } else {
            Toast.makeText(this, "Error loading station details", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeViews() {
        tvStoreName = findViewById(R.id.tvStoreName)
        tvAddress = findViewById(R.id.tvAddress)
        tvPhone = findViewById(R.id.tvPhone)
        tvHours = findViewById(R.id.tvHours)
        tvServices = findViewById(R.id.tvServices)
        tvPrices = findViewById(R.id.tvPrices)
        btnOrder = findViewById(R.id.btnOrder)
        btnReturn = findViewById(R.id.btnReturn)
        pricesCard = findViewById(R.id.pricesCard)

        deliveryHoursSection = findViewById(R.id.deliveryHoursSection)
        tvDeliveryHoursLabel = findViewById(R.id.tvDeliveryHoursLabel)
        deliveryTimesContainer = findViewById(R.id.deliveryTimesContainer)
    }

    private fun displayStationDetails(station: WaterStation) {
        tvStoreName.text = station.stationName

        val addressParts = listOfNotNull(
            station.address.takeIf { it.isNotEmpty() },
            station.city.takeIf { it.isNotEmpty() },
            station.state.takeIf { it.isNotEmpty() },
            station.zipCode.takeIf { it.isNotEmpty() }
        )
        tvAddress.text = addressParts.joinToString(", ")

        tvPhone.text = station.phone

        // Sort entries so "open" always comes before "close", regardless of
        // how Firebase returns them (HashMap has no guaranteed order).
        val hoursText = if (station.businessHours.isNotEmpty()) {
            station.businessHours.entries
                .sortedWith(compareBy { entry ->
                    val keyLower = entry.key.lowercase()
                    val index = HOURS_KEY_ORDER.indexOfFirst { keyLower.contains(it) }
                    if (index >= 0) index else Int.MAX_VALUE
                })
                .joinToString("\n") { (day, time) -> "$day: $time" }
        } else {
            "Opens 7AM - 7PM"
        }
        tvHours.text = hoursText

        displayDeliveryHours(station)

        val radius = station.getDeliveryRadiusInt()
        tvServices.text = if (radius > 0) {
            "Accepts Delivery (${radius}km radius) and Pick up"
        } else {
            "Accepts Pick up only"
        }

        displayPrices(station)
    }

    private fun displayDeliveryHours(station: WaterStation) {
        val hasDelivery = station.serviceTypes.contains("delivery")
        val deliveryHours = station.deliveryHours

        if (hasDelivery && !deliveryHours.isNullOrEmpty()) {
            deliveryHoursSection.visibility = View.VISIBLE
            deliveryTimesContainer.removeAllViews()

            deliveryHours.forEach { time ->
                val timeTextView = TextView(this).apply {
                    text = formatTime(time)
                    textSize = 14f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                deliveryTimesContainer.addView(timeTextView)
            }
        } else {
            deliveryHoursSection.visibility = View.GONE
        }
    }

    private fun formatTime(time: String): String {
        return try {
            val parts = time.split(":")
            if (parts.size == 2) {
                val hour = parts[0].toInt()
                val minute = parts[1]
                when {
                    hour == 0 -> "12:$minute AM"
                    hour < 12 -> "$hour:$minute AM"
                    hour == 12 -> "12:$minute PM"
                    else -> "${hour - 12}:$minute PM"
                }
            } else {
                time
            }
        } catch (e: Exception) {
            time
        }
    }

    private fun displayPrices(station: WaterStation) {
        val priceList = mutableListOf<String>()

        station.pricing_gallon_pure?.let { priceList.add("Pure Water: ₱${String.format("%.2f", it)}/gallon") }
        station.pricing_liter_spring?.let { priceList.add("Spring Water: ₱${String.format("%.2f", it)}/liter") }
        station.pricing_gallon_mineral?.let { priceList.add("Mineral Water: ₱${String.format("%.2f", it)}/gallon") }
        station.pricing_delivery_fee?.let { priceList.add("Delivery Fee: ₱${String.format("%.2f", it)}") }

        if (priceList.isNotEmpty()) {
            pricesCard.visibility = View.VISIBLE
            tvPrices.text = priceList.joinToString("\n")
        } else {
            pricesCard.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnOrder.setOnClickListener {
            currentStation?.let { navigateToCreateOrder(it) }
        }

        btnReturn.setOnClickListener {
            startActivity(Intent(this, MapActivity::class.java))
            finish()
        }
    }

    private fun navigateToCreateOrder(station: WaterStation) {
        try {
            val intent = Intent(this, CreateOrderActivity::class.java)
                .putExtra("WATER_STATION", station)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open order page", Toast.LENGTH_SHORT).show()
        }
    }
}