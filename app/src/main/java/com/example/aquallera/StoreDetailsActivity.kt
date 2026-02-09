package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
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
            pricesCard.visibility = android.view.View.VISIBLE
            tvPrices.text = priceList.joinToString("\n")
        } else {
            // Hide prices card if no prices are set
            pricesCard.visibility = android.view.View.GONE
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