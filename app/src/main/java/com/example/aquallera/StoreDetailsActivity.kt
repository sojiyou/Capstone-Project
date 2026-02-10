package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.util.Log
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

    // NEW: Delivery hours views
    private lateinit var deliveryHoursSection: LinearLayout
    private lateinit var tvDeliveryHoursLabel: TextView
    private lateinit var deliveryTimesContainer: LinearLayout

    private var currentStation: WaterStation? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_store_details)

        initializeViews()
        setupClickListeners()

        // Get the water station data from intent
        val station = intent.getSerializableExtra("WATER_STATION") as? WaterStation
        station?.let {
            currentStation = it

            // ✅ DEBUG LOGS
            Log.d("ORDER_DEBUG", "StoreDetails Station ID: ${it.id}")
            Log.d("ORDER_DEBUG", "StoreDetails Station Name: ${it.stationName}")

            displayStationDetails(it)
        } ?: run {
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

        // NEW: Initialize delivery hours views
        deliveryHoursSection = findViewById(R.id.deliveryHoursSection)
        tvDeliveryHoursLabel = findViewById(R.id.tvDeliveryHoursLabel)
        deliveryTimesContainer = findViewById(R.id.deliveryTimesContainer)
    }

    private fun displayStationDetails(station: WaterStation) {
        // Set basic information
        tvStoreName.text = station.stationName

        // Build address safely
        val addressParts = listOfNotNull(
            station.address.takeIf { it.isNotEmpty() },
            station.city.takeIf { it.isNotEmpty() },
            station.state.takeIf { it.isNotEmpty() },
            station.zipCode.takeIf { it.isNotEmpty() }
        )
        tvAddress.text = addressParts.joinToString(", ")

        tvPhone.text = station.phone

        // Set business hours
        val hoursText = if (station.businessHours.isNotEmpty()) {
            station.businessHours.entries.joinToString("\n") { (day, time) ->
                "$day: $time"
            }
        } else {
            "Opens 7AM - 7PM"
        }
        tvHours.text = hoursText

        // NEW: Display delivery hours
        displayDeliveryHours(station)

        // Set services
        val radius = station.getDeliveryRadiusInt()
        val servicesText = if (radius > 0) {
            "Accepts Delivery (${radius}km radius) and Pick up"
        } else {
            "Accepts Pick up only"
        }
        tvServices.text = servicesText

        // Display prices
        displayPrices(station)
    }

    // NEW: Function to display delivery hours
    private fun displayDeliveryHours(station: WaterStation) {
        // Check if station has delivery service and delivery hours
        val hasDelivery = station.serviceTypes.contains("delivery")
        val deliveryHours = station.deliveryHours

        if (hasDelivery && deliveryHours != null && deliveryHours.isNotEmpty()) {
            // Show the delivery hours section
            deliveryHoursSection.visibility = View.VISIBLE

            // Clear any existing views in the container
            deliveryTimesContainer.removeAllViews()

            // Add each delivery time as a TextView
            deliveryHours.forEach { time ->
                val timeTextView = TextView(this).apply {
                    text = formatTime(time)
                    textSize = 14f
                    setTextColor(resources.getColor(android.R.color.darker_gray, null))
                    setPadding(0, 4, 0, 4)
                }
                deliveryTimesContainer.addView(timeTextView)
            }

            Log.d("DELIVERY_HOURS", "Displaying ${deliveryHours.size} delivery times")
        } else {
            // Hide the delivery hours section if no delivery service or no hours set
            deliveryHoursSection.visibility = View.GONE
            Log.d("DELIVERY_HOURS", "No delivery hours to display")
        }
    }

    // NEW: Helper function to format time (convert 24hr to 12hr format)
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
                time // Return original if format is unexpected
            }
        } catch (e: Exception) {
            Log.e("DELIVERY_HOURS", "Error formatting time: $time", e)
            time // Return original on error
        }
    }

    private fun displayPrices(station: WaterStation) {
        val priceList = mutableListOf<String>()

        // Add each price if it exists
        station.pricing_gallon_pure?.let { price ->
            priceList.add("Pure Water: ₱${String.format("%.2f", price)}/gallon")
        }

        station.pricing_liter_spring?.let { price ->
            priceList.add("Spring Water: ₱${String.format("%.2f", price)}/liter")
        }

        station.pricing_gallon_mineral?.let { price ->
            priceList.add("Mineral Water: ₱${String.format("%.2f", price)}/gallon")
        }

        station.pricing_delivery_fee?.let { fee ->
            priceList.add("Delivery Fee: ₱${String.format("%.2f", fee)}")
        }

        // Check if there are any prices to display
        if (priceList.isNotEmpty()) {
            // Show prices card
            pricesCard.visibility = View.VISIBLE
            tvPrices.text = priceList.joinToString("\n")
        } else {
            // Hide prices card if no prices are set
            pricesCard.visibility = View.GONE
        }
    }

    private fun setupClickListeners() {
        btnOrder.setOnClickListener {
            currentStation?.let { station ->
                navigateToCreateOrder(station)
            }
        }

        btnReturn.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun navigateToCreateOrder(station: WaterStation) {
        try {
            val intent = Intent(this, CreateOrderActivity::class.java)
            intent.putExtra("WATER_STATION", station)
            startActivity(intent)
            // Don't finish here so user can return to details
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open order page", Toast.LENGTH_SHORT).show()
        }
    }
}