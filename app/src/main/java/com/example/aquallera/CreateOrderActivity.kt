package com.example.aquallera

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.text.SimpleDateFormat
import java.util.*

class CreateOrderActivity : AppCompatActivity() {

    // UI Elements
    private lateinit var tvWaterStationName: TextView
    private lateinit var tvSelectedDate: TextView
    private lateinit var tvSelectedTime: TextView
    private lateinit var calendarView: CalendarView
    private lateinit var rgOrderType: RadioGroup
    private lateinit var rbDelivery: RadioButton
    private lateinit var rbPickup: RadioButton
    private lateinit var btnGetLocation: Button
    private lateinit var tvLocation: TextView
    private lateinit var tvCoordinatesDisplay: TextView
    private lateinit var etManualAddress: EditText
    private lateinit var etAdditionalDetails: EditText
    private lateinit var btnProceed: Button

    // Water quantity inputs
    private lateinit var etGallonPure: EditText
    private lateinit var etLiterSpring: EditText
    private lateinit var etGallonMineral: EditText

    // Water price displays
    private lateinit var tvPureWaterPrice: TextView
    private lateinit var tvSpringWaterPrice: TextView
    private lateinit var tvMineralWaterPrice: TextView
    private lateinit var tvDeliveryFee: TextView

    // Quantity control buttons
    private lateinit var btnIncreaseGallonPure: Button
    private lateinit var btnDecreaseGallonPure: Button
    private lateinit var btnIncreaseLiterSpring: Button
    private lateinit var btnDecreaseLiterSpring: Button
    private lateinit var btnIncreaseGallonMineral: Button
    private lateinit var btnDecreaseGallonMineral: Button

    // Data
    private var currentStation: WaterStation? = null
    private var selectedDate: String = ""
    private var selectedTime: String = ""
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var userAddress: String = ""
    private var manualAddress: String = ""

    // Prices from station
    private var pureWaterPrice: Double = 0.0
    private var springWaterPrice: Double = 0.0
    private var mineralWaterPrice: Double = 0.0
    private var deliveryFee: Double = 0.0

    // Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_order)

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get station data from intent
        currentStation = intent.getSerializableExtra("WATER_STATION") as? WaterStation

        initializeViews()
        setupClickListeners()
        setupQuantityControls()
        setupDateAndTime()
        loadStationPrices()
        updateWaterStationName()
        setupBottomNavigation()
        updateLocationFieldsVisibility() // Initialize visibility state
    }

    private fun initializeViews() {
        // Basic views
        tvWaterStationName = findViewById(R.id.tvWaterStationName)
        tvSelectedDate = findViewById(R.id.tvSelectedDate)
        tvSelectedTime = findViewById(R.id.tvSelectedTime)
        calendarView = findViewById(R.id.calendarView)
        rgOrderType = findViewById(R.id.rgOrderType)
        rbDelivery = findViewById(R.id.rbDelivery)
        rbPickup = findViewById(R.id.rbPickup)
        btnGetLocation = findViewById(R.id.getLocation)
        tvLocation = findViewById(R.id.tvLocation)
        tvCoordinatesDisplay = findViewById(R.id.tvCoordinatesDisplay)
        etManualAddress = findViewById(R.id.etManualAddress)
        etAdditionalDetails = findViewById(R.id.additionalDetailsInput)
        btnProceed = findViewById(R.id.proceedButton)

        // Water quantity inputs
        etGallonPure = findViewById(R.id.etGallonPure)
        etLiterSpring = findViewById(R.id.etLiterSpring)
        etGallonMineral = findViewById(R.id.etGallonMineral)

        // Price displays
        tvPureWaterPrice = findViewById(R.id.tvPureWaterPrice)
        tvSpringWaterPrice = findViewById(R.id.tvSpringWaterPrice)
        tvMineralWaterPrice = findViewById(R.id.tvMineralWaterPrice)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)

        // Quantity control buttons
        btnIncreaseGallonPure = findViewById(R.id.btnIncreaseGallonPure)
        btnDecreaseGallonPure = findViewById(R.id.btnDecreaseGallonPure)
        btnIncreaseLiterSpring = findViewById(R.id.btnIncreaseLiterSpring)
        btnDecreaseLiterSpring = findViewById(R.id.btnDecreaseLiterSpring)
        btnIncreaseGallonMineral = findViewById(R.id.btnIncreaseGallonMineral)
        btnDecreaseGallonMineral = findViewById(R.id.btnDecreaseGallonMineral)
    }

    private fun loadStationPrices() {
        currentStation?.let { station ->
            // Get prices from station (use 0.0 if null)
            pureWaterPrice = station.pricing_gallon_pure ?: 0.0
            springWaterPrice = station.pricing_liter_spring ?: 0.0
            mineralWaterPrice = station.pricing_gallon_mineral ?: 0.0
            deliveryFee = station.pricing_delivery_fee ?: 0.0

            // Update price displays
            tvPureWaterPrice.text = "Price: ₱${String.format("%.2f", pureWaterPrice)}/gallon"
            tvSpringWaterPrice.text = "Price: ₱${String.format("%.2f", springWaterPrice)}/liter"
            tvMineralWaterPrice.text = "Price: ₱${String.format("%.2f", mineralWaterPrice)}/gallon"

            // Update delivery fee display
            updateDeliveryFeeVisibility()
        }
    }

    private fun updateWaterStationName() {
        currentStation?.let { station ->
            tvWaterStationName.text = station.stationName
        }
    }

    private fun setupDateAndTime() {
        // Set current date as default
        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        selectedDate = dateFormat.format(calendar.time)
        tvSelectedDate.text = selectedDate

        // Set current time + 1 minute as default to avoid past time
        calendar.add(Calendar.MINUTE, 1)
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        selectedTime = timeFormat.format(calendar.time)
        tvSelectedTime.text = selectedTime

        // Date picker dialog when date text is clicked
        tvSelectedDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
                selectedCalendar.set(Calendar.HOUR_OF_DAY, 0)
                selectedCalendar.set(Calendar.MINUTE, 0)
                selectedCalendar.set(Calendar.SECOND, 0)
                selectedCalendar.set(Calendar.MILLISECOND, 0)

                val todayCalendar = Calendar.getInstance()
                todayCalendar.set(Calendar.HOUR_OF_DAY, 0)
                todayCalendar.set(Calendar.MINUTE, 0)
                todayCalendar.set(Calendar.SECOND, 0)
                todayCalendar.set(Calendar.MILLISECOND, 0)

                val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)
                tvSelectedDate.text = selectedDate

                // If selected date is today, validate the current time
                if (selectedCalendar.timeInMillis == todayCalendar.timeInMillis) {
                    validateSelectedTimeAgainstCurrentTime()
                } else {
                    // If future date, set to a reasonable default time (e.g., 9:00 AM)
                    val defaultTime = Calendar.getInstance()
                    defaultTime.set(Calendar.HOUR_OF_DAY, 9)
                    defaultTime.set(Calendar.MINUTE, 0)
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                    selectedTime = timeFormat.format(defaultTime.time)
                    tvSelectedTime.text = selectedTime
                }
            },
            year,
            month,
            day
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = calendar.timeInMillis

        datePickerDialog.show()
    }

    // Helper function to validate time when date changes
    private fun validateSelectedTimeAgainstCurrentTime() {
        val currentTime = Calendar.getInstance()
        val selectedTimeCalendar = Calendar.getInstance()

        // Parse selected time
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        try {
            selectedTimeCalendar.time = timeFormat.parse(selectedTime) ?: return

            // Set the date to today
            val today = Calendar.getInstance()
            selectedTimeCalendar.set(Calendar.YEAR, today.get(Calendar.YEAR))
            selectedTimeCalendar.set(Calendar.MONTH, today.get(Calendar.MONTH))
            selectedTimeCalendar.set(Calendar.DAY_OF_MONTH, today.get(Calendar.DAY_OF_MONTH))

            // If selected time is in the past, update to current time + 1 minute
            if (selectedTimeCalendar.timeInMillis <= currentTime.timeInMillis) {
                val suggestionHour = currentTime.get(Calendar.HOUR_OF_DAY)
                var suggestionMinute = currentTime.get(Calendar.MINUTE) + 1

                if (suggestionMinute >= 60) {
                    suggestionMinute = 59 // Cap at 59 if we hit hour boundary
                }

                val amPm = if (suggestionHour < 12) "AM" else "PM"
                val displayHour = if (suggestionHour > 12) suggestionHour - 12 else if (suggestionHour == 0) 12 else suggestionHour
                selectedTime = String.format("%02d:%02d %s", displayHour, suggestionMinute, amPm)
                tvSelectedTime.text = selectedTime

                Toast.makeText(
                    this,
                    "Time updated to current time + 1 minute",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // UPDATED showTimePicker with validation
    fun showTimePicker(view: android.view.View) {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)

        // Parse selected date
        val selectedDateCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())
        try {
            selectedDateCalendar.time = dateFormat.parse(selectedDate) ?: Calendar.getInstance().time
        } catch (e: Exception) {
            // If parsing fails, use current date
            selectedDateCalendar.time = Calendar.getInstance().time
        }

        val timePickerDialog = TimePickerDialog(
            this,
            TimePickerDialog.OnTimeSetListener { _, selectedHour, selectedMinute ->
                // Check if selected date is today
                val today = Calendar.getInstance()
                val isToday = selectedDateCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        selectedDateCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)

                // If it's today, validate that time is not in the past
                if (isToday) {
                    if (selectedHour < currentHour ||
                        (selectedHour == currentHour && selectedMinute <= currentMinute)) {

                        Toast.makeText(
                            this,
                            "Cannot select a time in the past. Please select a future time.",
                            Toast.LENGTH_LONG
                        ).show()

                        // Set to current time + 1 minute as suggestion
                        val suggestionHour = currentHour
                        val suggestionMinute = currentMinute + 1

                        if (suggestionMinute >= 60) {
                            // Handle minute overflow
                            val newHour = suggestionHour + 1
                            val newMinute = suggestionMinute - 60
                            if (newHour < 24) {
                                val amPm = if (newHour < 12) "AM" else "PM"
                                val displayHour = if (newHour > 12) newHour - 12 else if (newHour == 0) 12 else newHour
                                selectedTime = String.format("%02d:%02d %s", displayHour, newMinute, amPm)
                                tvSelectedTime.text = selectedTime

                                Toast.makeText(
                                    this,
                                    "Time set to ${String.format("%02d:%02d", displayHour, newMinute)} $amPm",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        } else {
                            val amPm = if (suggestionHour < 12) "AM" else "PM"
                            val displayHour = if (suggestionHour > 12) suggestionHour - 12 else if (suggestionHour == 0) 12 else suggestionHour
                            selectedTime = String.format("%02d:%02d %s", displayHour, suggestionMinute, amPm)
                            tvSelectedTime.text = selectedTime

                            Toast.makeText(
                                this,
                                "Time set to ${String.format("%02d:%02d", displayHour, suggestionMinute)} $amPm",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        return@OnTimeSetListener
                    }
                }

                // If validation passes, set the selected time
                val amPm = if (selectedHour < 12) "AM" else "PM"
                val displayHour = if (selectedHour > 12) selectedHour - 12 else if (selectedHour == 0) 12 else selectedHour
                selectedTime = String.format("%02d:%02d %s", displayHour, selectedMinute, amPm)
                tvSelectedTime.text = selectedTime
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            false
        )

        timePickerDialog.show()
    }

    private fun setupClickListeners() {
        // Order type change listener
        rgOrderType.setOnCheckedChangeListener { _, checkedId ->
            updateDeliveryFeeVisibility()
            updateLocationFieldsVisibility()
        }

        // Get location button
        btnGetLocation.setOnClickListener {
            getCurrentLocation()
        }

        // Proceed button
        btnProceed.setOnClickListener {
            if (validateOrder()) {
                navigateToOrderConfirmation()
            }
        }
    }

    private fun updateLocationFieldsVisibility() {
        // Show/hide location-related fields based on order type
        val isDelivery = rbDelivery.isChecked

        // These fields are only relevant for delivery
        val visibility = if (isDelivery) android.view.View.VISIBLE else android.view.View.GONE

        btnGetLocation.visibility = visibility
        etManualAddress.visibility = visibility
        findViewById<TextView>(R.id.cbUseCoordinatesForDelivery).visibility = visibility

        tvLocation.visibility = if (isDelivery && tvLocation.text != "Location not selected") visibility else android.view.View.GONE
        tvCoordinatesDisplay.visibility = if (isDelivery && tvCoordinatesDisplay.text != "Coordinates will appear here") visibility else android.view.View.GONE

        // If pickup is selected, clear location data
        if (!isDelivery) {
            userLatitude = 0.0
            userLongitude = 0.0
            userAddress = ""
            manualAddress = ""
        }
    }

    private fun setupQuantityControls() {
        // Setup + and - buttons for Pure Water
        btnIncreaseGallonPure.setOnClickListener {
            val current = etGallonPure.text.toString().toIntOrNull() ?: 0
            etGallonPure.setText((current + 1).toString())
        }

        btnDecreaseGallonPure.setOnClickListener {
            val current = etGallonPure.text.toString().toIntOrNull() ?: 0
            if (current > 0) {
                etGallonPure.setText((current - 1).toString())
            }
        }

        // Setup + and - buttons for Spring Water
        btnIncreaseLiterSpring.setOnClickListener {
            val current = etLiterSpring.text.toString().toIntOrNull() ?: 0
            etLiterSpring.setText((current + 1).toString())
        }

        btnDecreaseLiterSpring.setOnClickListener {
            val current = etLiterSpring.text.toString().toIntOrNull() ?: 0
            if (current > 0) {
                etLiterSpring.setText((current - 1).toString())
            }
        }

        // Setup + and - buttons for Mineral Water
        btnIncreaseGallonMineral.setOnClickListener {
            val current = etGallonMineral.text.toString().toIntOrNull() ?: 0
            etGallonMineral.setText((current + 1).toString())
        }

        btnDecreaseGallonMineral.setOnClickListener {
            val current = etGallonMineral.text.toString().toIntOrNull() ?: 0
            if (current > 0) {
                etGallonMineral.setText((current - 1).toString())
            }
        }

        // Setup text watchers to prevent negative numbers
        setupTextWatcher(etGallonPure)
        setupTextWatcher(etLiterSpring)
        setupTextWatcher(etGallonMineral)
    }

    private fun setupTextWatcher(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                s?.toString()?.let { text ->
                    if (text.isNotEmpty()) {
                        val value = text.toIntOrNull()
                        if (value == null || value < 0) {
                            editText.setText("0")
                        }
                    }
                }
            }
        })
    }

    private fun updateDeliveryFeeVisibility() {
        if (rbDelivery.isChecked && deliveryFee > 0) {
            tvDeliveryFee.text = "Delivery Fee: ₱${String.format("%.2f", deliveryFee)}"
            tvDeliveryFee.visibility = android.view.View.VISIBLE
        } else {
            tvDeliveryFee.visibility = android.view.View.GONE
        }
    }

    private fun getCurrentLocation() {
        if (!rbDelivery.isChecked) {
            Toast.makeText(this, "Location is only needed for delivery orders", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    location?.let {
                        userLatitude = it.latitude
                        userLongitude = it.longitude

                        // Format coordinates
                        val coordinates = "Lat: ${String.format("%.6f", userLatitude)}, Lng: ${String.format("%.6f", userLongitude)}"

                        // Update both displays
                        userAddress = coordinates
                        tvLocation.text = coordinates
                        tvLocation.visibility = android.view.View.VISIBLE

                        tvCoordinatesDisplay.text = coordinates
                        tvCoordinatesDisplay.visibility = android.view.View.VISIBLE

                        Toast.makeText(this, "Location detected successfully!", Toast.LENGTH_SHORT).show()

                        // Check if within delivery range if delivery is selected
                        if (rbDelivery.isChecked) {
                            checkDeliveryRange()
                        }
                    } ?: run {
                        Toast.makeText(this, "Unable to get location. Please enable GPS and try again.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to get location: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Location permission required. Please enable in settings.", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkDeliveryRange() {
        currentStation?.let { station ->
            if (station.latitude != 0.0 && station.longitude != 0.0 && userLatitude != 0.0 && userLongitude != 0.0) {
                val isWithinRange = station.isWithinDeliveryRange(userLatitude, userLongitude)
                if (!isWithinRange) {
                    Toast.makeText(this,
                        "⚠️ You are outside the delivery range (${station.deliveryRadius}km). " +
                                "You may still place an order but delivery might take longer or incur additional fees.",
                        Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(this, "✅ You are within delivery range!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun calculateOrderTotal(): OrderTotal {
        val pureWaterQty = etGallonPure.text.toString().toIntOrNull() ?: 0
        val springWaterQty = etLiterSpring.text.toString().toIntOrNull() ?: 0
        val mineralWaterQty = etGallonMineral.text.toString().toIntOrNull() ?: 0

        val pureWaterTotal = pureWaterQty * pureWaterPrice
        val springWaterTotal = springWaterQty * springWaterPrice
        val mineralWaterTotal = mineralWaterQty * mineralWaterPrice

        val waterSubtotal = pureWaterTotal + springWaterTotal + mineralWaterTotal

        // Add delivery fee if delivery is selected
        val totalDeliveryFee = if (rbDelivery.isChecked) deliveryFee else 0.0
        val grandTotal = waterSubtotal + totalDeliveryFee

        Log.d("ORDER_DEBUG", "Station ID: ${currentStation?.id}")
        Log.d("ORDER_DEBUG", "Station Name: ${currentStation?.stationName}")

        return OrderTotal(
            pureWaterQty = pureWaterQty,
            pureWaterPrice = pureWaterPrice,
            pureWaterTotal = pureWaterTotal,
            springWaterQty = springWaterQty,
            springWaterPrice = springWaterPrice,
            springWaterTotal = springWaterTotal,
            mineralWaterQty = mineralWaterQty,
            mineralWaterPrice = mineralWaterPrice,
            mineralWaterTotal = mineralWaterTotal,
            waterSubtotal = waterSubtotal,
            deliveryFee = totalDeliveryFee,
            grandTotal = grandTotal
        )
    }

    private fun validateOrder(): Boolean {
        // Check if any water is selected
        val pureWaterQty = etGallonPure.text.toString().toIntOrNull() ?: 0
        val springWaterQty = etLiterSpring.text.toString().toIntOrNull() ?: 0
        val mineralWaterQty = etGallonMineral.text.toString().toIntOrNull() ?: 0

        if (pureWaterQty == 0 && springWaterQty == 0 && mineralWaterQty == 0) {
            Toast.makeText(this, "Please select at least one type of water", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate date and time - prevent past orders
        if (!validateOrderDateTime()) {
            return false
        }

        // Validate delivery-specific fields
        if (rbDelivery.isChecked) {
            // Get manual address
            manualAddress = etManualAddress.text.toString().trim()

            // Check if either GPS location OR manual address is provided
            val hasGpsLocation = userLatitude != 0.0 && userLongitude != 0.0
            val hasManualAddress = manualAddress.isNotEmpty()

            if (!hasGpsLocation && !hasManualAddress) {
                Toast.makeText(this,
                    "Please either:\n• Get your GPS location, OR\n• Enter your complete address manually",
                    Toast.LENGTH_LONG).show()
                return false
            }

            // Check delivery range if GPS location is available
            if (hasGpsLocation) {
                currentStation?.let { station ->
                    if (!station.isWithinDeliveryRange(userLatitude, userLongitude)) {
                        Toast.makeText(this,
                            "⚠️ You are outside the delivery range (${station.deliveryRadius}km). " +
                                    "Please select pickup or choose another station.",
                            Toast.LENGTH_LONG).show()
                        return false
                    }
                }
            }
        }

        return true
    }

    // New function to validate order date and time
    private fun validateOrderDateTime(): Boolean {
        val now = Calendar.getInstance()

        // Parse selected date
        val selectedDateCalendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

        try {
            selectedDateCalendar.time = dateFormat.parse(selectedDate) ?: return false
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid date selected", Toast.LENGTH_SHORT).show()
            return false
        }

        // Parse selected time
        val selectedDateTimeCalendar = Calendar.getInstance()
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

        try {
            selectedDateTimeCalendar.time = timeFormat.parse(selectedTime) ?: return false

            // Combine date and time
            selectedDateTimeCalendar.set(
                selectedDateCalendar.get(Calendar.YEAR),
                selectedDateCalendar.get(Calendar.MONTH),
                selectedDateCalendar.get(Calendar.DAY_OF_MONTH),
                selectedDateTimeCalendar.get(Calendar.HOUR_OF_DAY),
                selectedDateTimeCalendar.get(Calendar.MINUTE)
            )
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid time selected", Toast.LENGTH_SHORT).show()
            return false
        }

        // Check if selected date/time is in the past
        if (selectedDateTimeCalendar.timeInMillis <= now.timeInMillis) {
            Toast.makeText(
                this,
                "Cannot place order for a past date or time. Please select a future time.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        return true
    }

    private fun navigateToOrderConfirmation() {
        val orderTotal = calculateOrderTotal()
        manualAddress = etManualAddress.text.toString().trim()

        val intent = Intent(this, OrderConfirmationActivity::class.java)
        intent.putExtra("WATER_STATION", currentStation)
        intent.putExtra("ORDER_DATE", selectedDate)
        intent.putExtra("ORDER_TIME", selectedTime)
        intent.putExtra("ORDER_TYPE", if (rbDelivery.isChecked) "Delivery" else "Pickup")
        intent.putExtra("USER_LATITUDE", userLatitude)
        intent.putExtra("USER_LONGITUDE", userLongitude)
        intent.putExtra("USER_ADDRESS", userAddress)
        intent.putExtra("MANUAL_ADDRESS", manualAddress)
        intent.putExtra("ADDITIONAL_DETAILS", etAdditionalDetails.text.toString())
        intent.putExtra("ORDER_TOTAL", orderTotal)

        // Pass water quantities
        intent.putExtra("PURE_WATER_QTY", orderTotal.pureWaterQty)
        intent.putExtra("SPRING_WATER_QTY", orderTotal.springWaterQty)
        intent.putExtra("MINERAL_WATER_QTY", orderTotal.mineralWaterQty)

        startActivity(intent)
    }

    private fun setupBottomNavigation() {
        val navMap = findViewById<LinearLayout>(R.id.navMap)
        val navOrders = findViewById<LinearLayout>(R.id.navOrder)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        // Set Map as active
        setActiveTab(navMap)
        navMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
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

    // Data class for order total calculation
    data class OrderTotal(
        val pureWaterQty: Int,
        val pureWaterPrice: Double,
        val pureWaterTotal: Double,
        val springWaterQty: Int,
        val springWaterPrice: Double,
        val springWaterTotal: Double,
        val mineralWaterQty: Int,
        val mineralWaterPrice: Double,
        val mineralWaterTotal: Double,
        val waterSubtotal: Double,
        val deliveryFee: Double,
        val grandTotal: Double
    ) : java.io.Serializable
}