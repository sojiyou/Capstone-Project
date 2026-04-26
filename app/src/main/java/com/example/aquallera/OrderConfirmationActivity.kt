package com.example.aquallera

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class OrderConfirmationActivity : AppCompatActivity() {

    private var transactionFee: Double = 10.0

    private lateinit var tvWaterStationName: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderTime: TextView
    private lateinit var tvOrderType: TextView
    private lateinit var tvDeliveryAddress: TextView

    // Order Items
    private lateinit var tvPureWaterDetails: TextView
    private lateinit var tvSpringWaterDetails: TextView
    private lateinit var tvMineralWaterDetails: TextView

    // Summary
    private lateinit var tvSubtotal: TextView
    private lateinit var tvDeliveryFee: TextView
    private lateinit var tvTransactionFee: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvReferenceNumber: TextView
    private lateinit var tvAdditionalDetails: TextView
    private lateinit var tvPaymentMethod: TextView

    // Layout wrappers
    private lateinit var layoutDeliveryFee: LinearLayout
    private lateinit var layoutTransactionFee: LinearLayout

    // Buttons
    private lateinit var btnConfirmOrder: Button
    private lateinit var btnEditOrder: Button

    // Data
    private var currentStation: WaterStation? = null
    private lateinit var orderTotal: CreateOrderActivity.OrderTotal
    private var orderDate: String = ""
    private var orderTime: String = ""
    private var orderType: String = ""
    private var userAddress: String = ""
    private var manualAddress: String = ""  // NEW: Store manual address separately
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var additionalDetails: String = ""
    private var referenceNumber: String = ""
    private var customerPhone: String = ""  // Received directly from CreateOrderActivity

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        try {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance(
                "https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app"
            )

            getIntentData()
            initializeViews()
            setupClickListeners()
            displayOrderDetails()

        } catch (e: Exception) {
            Toast.makeText(this, "App Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun getIntentData() {
        try {
            currentStation = intent.getSerializableExtra("WATER_STATION") as? WaterStation
            orderTotal = intent.getSerializableExtra("ORDER_TOTAL") as CreateOrderActivity.OrderTotal
            orderDate = intent.getStringExtra("ORDER_DATE") ?: ""
            orderTime = intent.getStringExtra("ORDER_TIME") ?: ""
            orderType = intent.getStringExtra("ORDER_TYPE") ?: ""
            userAddress = intent.getStringExtra("USER_ADDRESS") ?: ""
            manualAddress = intent.getStringExtra("MANUAL_ADDRESS") ?: ""  // NEW: Get manual address
            userLatitude = intent.getDoubleExtra("USER_LATITUDE", 0.0)
            userLongitude = intent.getDoubleExtra("USER_LONGITUDE", 0.0)
            additionalDetails = intent.getStringExtra("ADDITIONAL_DETAILS") ?: ""
            customerPhone = intent.getStringExtra("CONTACT_NUMBER") ?: ""  // NEW - from intent
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {
        tvWaterStationName = findViewById(R.id.tvWaterStationName)
        tvCustomerName = findViewById(R.id.tvCustomerName)
        tvOrderDate = findViewById(R.id.tvOrderDate)
        tvOrderTime = findViewById(R.id.tvOrderTime)
        tvOrderType = findViewById(R.id.tvOrderType)
        tvDeliveryAddress = findViewById(R.id.tvDeliveryAddress)

        tvPureWaterDetails = findViewById(R.id.tvPureWaterDetails)
        tvSpringWaterDetails = findViewById(R.id.tvSpringWaterDetails)
        tvMineralWaterDetails = findViewById(R.id.tvMineralWaterDetails)

        tvSubtotal = findViewById(R.id.tvSubtotal)
        tvDeliveryFee = findViewById(R.id.tvDeliveryFee)
        tvTransactionFee = findViewById(R.id.tvTransactionFee)
        tvGrandTotal = findViewById(R.id.tvGrandTotal)
        tvReferenceNumber = findViewById(R.id.tvReferenceNumber)
        tvAdditionalDetails = findViewById(R.id.tvAdditionalDetails)
        tvPaymentMethod = findViewById(R.id.tvPaymentMethod)

        layoutDeliveryFee = findViewById(R.id.layoutDeliveryFee)
        layoutTransactionFee = findViewById(R.id.layoutTransactionFee)

        btnConfirmOrder = findViewById(R.id.btnConfirmOrder)
        btnEditOrder = findViewById(R.id.btnEditOrder)
    }

    private fun setupClickListeners() {
        btnConfirmOrder.setOnClickListener {
            saveOrderToFirebase()  // generateReferenceNumber is now called inside saveOrderToFirebase
        }

        btnEditOrder.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun displayOrderDetails() {
        try {
            tvWaterStationName.text = currentStation?.stationName ?: "Water Station"
            val currentUser = auth.currentUser
            val customerName = currentUser?.displayName ?: "Customer"
            tvCustomerName.text = "Name: $customerName"

            tvOrderDate.text = "Date: $orderDate"
            tvOrderTime.text = "Time: $orderTime"
            tvOrderType.text = "Type: $orderType"

            // MODIFIED: Show manual address if available, otherwise use coordinates but hide coordinates
            if (orderType == "Delivery") {
                val displayAddress = if (manualAddress.isNotEmpty()) {
                    manualAddress  // Show the manually entered address
                } else if (userAddress.isNotEmpty() && !userAddress.contains("Lat:")) {
                    userAddress  // Show address if it's not just coordinates
                } else {
                    "Address will be provided during delivery"  // Default message when only coordinates exist
                }

                tvDeliveryAddress.text = "Address: $displayAddress"
                tvDeliveryAddress.visibility = android.view.View.VISIBLE
            } else {
                tvDeliveryAddress.visibility = android.view.View.GONE
            }

            displayOrderItems()

            tvSubtotal.text = "₱${String.format("%.2f", orderTotal.waterSubtotal)}"
            tvTransactionFee.text = "₱${String.format("%.2f", transactionFee)}"
            layoutTransactionFee.visibility = android.view.View.VISIBLE

            if (orderTotal.deliveryFee > 0 && orderType == "Delivery") {
                tvDeliveryFee.text = "₱${String.format("%.2f", orderTotal.deliveryFee)}"
                layoutDeliveryFee.visibility = android.view.View.VISIBLE
            } else {
                layoutDeliveryFee.visibility = android.view.View.GONE
            }

            val grandTotal = orderTotal.waterSubtotal +
                    (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                    transactionFee
            tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"

            // Reference number placeholder until Firebase transaction returns
            tvReferenceNumber.text = "Generating..."

            if (additionalDetails.isNotEmpty()) {
                tvAdditionalDetails.text = "Notes: $additionalDetails"
                tvAdditionalDetails.visibility = android.view.View.VISIBLE
            } else {
                tvAdditionalDetails.visibility = android.view.View.GONE
            }

            tvPaymentMethod.text = "Payment: Cash on Delivery"

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun displayOrderItems() {
        if (orderTotal.pureWaterQty > 0) {
            tvPureWaterDetails.text =
                "${orderTotal.pureWaterQty} gallon(s) Pure Water: ₱${String.format("%.2f", orderTotal.pureWaterTotal)}"
            tvPureWaterDetails.visibility = android.view.View.VISIBLE
        } else tvPureWaterDetails.visibility = android.view.View.GONE

        if (orderTotal.springWaterQty > 0) {
            tvSpringWaterDetails.text =
                "${orderTotal.springWaterQty} liter(s) Spring Water: ₱${String.format("%.2f", orderTotal.springWaterTotal)}"
            tvSpringWaterDetails.visibility = android.view.View.VISIBLE
        } else tvSpringWaterDetails.visibility = android.view.View.GONE

        if (orderTotal.mineralWaterQty > 0) {
            tvMineralWaterDetails.text =
                "${orderTotal.mineralWaterQty} gallon(s) Mineral Water: ₱${String.format("%.2f", orderTotal.mineralWaterTotal)}"
            tvMineralWaterDetails.visibility = android.view.View.VISIBLE
        } else tvMineralWaterDetails.visibility = android.view.View.GONE
    }

    private fun generateReferenceNumber(callback: (String) -> Unit) {
        val dateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
        val counterRef = database.reference.child("orderCounter").child(dateStr)

        // Get station name prefix (first 3 letters, uppercase, letters only)
        val stationPrefix = currentStation?.stationName
            ?.filter { it.isLetter() }
            ?.take(3)
            ?.uppercase()
            ?: "WTR"

        counterRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(currentData: MutableData): Transaction.Result {
                var currentCount = currentData.getValue(Int::class.java) ?: 0
                currentCount++
                currentData.value = currentCount
                return Transaction.success(currentData)
            }

            override fun onComplete(
                error: DatabaseError?,
                committed: Boolean,
                currentData: DataSnapshot?
            ) {
                if (error != null) {
                    Log.e("OrderConfirmation", "Failed to generate reference: ${error.message}")
                    callback("$stationPrefix-${dateStr}-0000")
                } else {
                    val count = currentData?.getValue(Int::class.java) ?: 0
                    val counterString = count.toString().padStart(4, '0')
                    val orderId = "$stationPrefix-$dateStr-$counterString"
                    callback(orderId)
                }
            }
        })
    }

    private fun createOrderObject(customerId: String, orderId: String, customerPhone: String): Order {
        val currentUser = auth.currentUser
        val customerName = currentUser?.displayName ?: "Customer"

        val grandTotal = orderTotal.waterSubtotal +
                (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                transactionFee

        // MODIFIED: Use manual address if available for display, otherwise use coordinates for backend
        val displayAddress = if (manualAddress.isNotEmpty()) {
            manualAddress
        } else {
            userAddress
        }

        return Order(
            orderId = orderId,
            stationId = currentStation?.id ?: "",
            stationName = currentStation?.stationName ?: "",
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,
            orderType = orderType,
            date = orderDate,
            time = orderTime,
            pureWaterQty = orderTotal.pureWaterQty,
            springWaterQty = orderTotal.springWaterQty,
            mineralWaterQty = orderTotal.mineralWaterQty,
            pureWaterPrice = orderTotal.pureWaterPrice,
            springWaterPrice = orderTotal.springWaterPrice,
            mineralWaterPrice = orderTotal.mineralWaterPrice,
            pureWaterTotal = orderTotal.pureWaterTotal,
            springWaterTotal = orderTotal.springWaterTotal,
            mineralWaterTotal = orderTotal.mineralWaterTotal,
            waterSubtotal = orderTotal.waterSubtotal,
            deliveryFee = if (orderType == "Delivery") orderTotal.deliveryFee else 0.0,
            transactionFee = transactionFee,
            grandTotal = grandTotal,
            locationDetails = displayAddress,  // Store the user-friendly address
            deliveryAddress = if (orderType == "Delivery") displayAddress else "",
            deliveryLatitude = if (orderType == "Delivery") userLatitude else 0.0,
            deliveryLongitude = if (orderType == "Delivery") userLongitude else 0.0,
            additionalDetails = additionalDetails,
            paymentMethod = "Cash on Delivery",
            status = "Pending",
            createdAt = System.currentTimeMillis(),
            referenceNumber = referenceNumber
        )
    }

    @SuppressLint("SetTextI18n")
    private fun saveOrderToFirebase() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Please login to continue", Toast.LENGTH_SHORT).show()
            return
        }

        btnConfirmOrder.isEnabled = false
        btnConfirmOrder.text = "Processing..."

        try {
            // Generate orderId using the new format (e.g., AQU-20260427-0042)
            generateReferenceNumber { orderId ->
                referenceNumber = orderId

                // Use the generated orderId as the Firebase key
                val order = createOrderObject(currentUser.uid, orderId, customerPhone)

                database.reference.child("orders").child(orderId)
                    .setValue(order)
                    .addOnSuccessListener { navigateToOrderSuccess(order) }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to save order: ${e.message}", Toast.LENGTH_SHORT).show()
                        btnConfirmOrder.isEnabled = true
                        btnConfirmOrder.text = "Confirm Order"
                    }
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            btnConfirmOrder.isEnabled = true
            btnConfirmOrder.text = "Confirm Order"
        }
    }

    private fun navigateToOrderSuccess(order: Order) {
        try {
            val intent = Intent(this, OrderSuccessActivity::class.java)
            intent.putExtra("ORDER_ID", order.orderId)
            intent.putExtra("REFERENCE_NUMBER", order.referenceNumber)
            intent.putExtra("STATION_NAME", order.stationName)
            intent.putExtra("CUSTOMER_NAME", order.customerName)
            intent.putExtra("ORDER_DATE", order.date)
            intent.putExtra("ORDER_TIME", order.time)
            intent.putExtra("ORDER_TYPE", order.orderType)
            intent.putExtra("GRAND_TOTAL", order.grandTotal)
            intent.putExtra("TRANSACTION_FEE", order.transactionFee)
            intent.putExtra("ORDER_STATUS", order.status)

            intent.putExtra("PURE_WATER_QTY", order.pureWaterQty)
            intent.putExtra("SPRING_WATER_QTY", order.springWaterQty)
            intent.putExtra("MINERAL_WATER_QTY", order.mineralWaterQty)
            intent.putExtra("PURE_WATER_PRICE", order.pureWaterPrice)
            intent.putExtra("SPRING_WATER_PRICE", order.springWaterPrice)
            intent.putExtra("MINERAL_WATER_PRICE", order.mineralWaterPrice)
            intent.putExtra("PURE_WATER_TOTAL", order.pureWaterTotal)
            intent.putExtra("SPRING_WATER_TOTAL", order.springWaterTotal)
            intent.putExtra("MINERAL_WATER_TOTAL", order.mineralWaterTotal)
            intent.putExtra("WATER_SUBTOTAL", order.waterSubtotal)
            intent.putExtra("DELIVERY_FEE", order.deliveryFee)

            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Navigation error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}