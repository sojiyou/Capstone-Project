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

    private var referenceCounter: Int = 0
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
    private var userLatitude: Double = 0.0
    private var userLongitude: Double = 0.0
    private var additionalDetails: String = ""
    private var referenceNumber: String = ""

    // Firebase
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_confirmation)

        try {
            // Initialize Firebase Auth
            auth = FirebaseAuth.getInstance()

            auth.currentUser?.uid?.let {
                userId -> debugUserData(userId)
            }

            // Get data from intent
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
            userLatitude = intent.getDoubleExtra("USER_LATITUDE", 0.0)
            userLongitude = intent.getDoubleExtra("USER_LONGITUDE", 0.0)
            additionalDetails = intent.getStringExtra("ADDITIONAL_DETAILS") ?: ""

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun initializeViews() {

        try {
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

            // Layout wrappers
            layoutDeliveryFee = findViewById(R.id.layoutDeliveryFee)
            layoutTransactionFee = findViewById(R.id.layoutTransactionFee)

            btnConfirmOrder = findViewById(R.id.btnConfirmOrder)
            btnEditOrder = findViewById(R.id.btnEditOrder)

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupClickListeners() {
        btnConfirmOrder.setOnClickListener {
            saveOrderToFirebase()
        }

        btnEditOrder.setOnClickListener {
            finish()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun displayOrderDetails() {
        try {
            // Station name
            tvWaterStationName.text = currentStation?.stationName ?: "Water Station"

            // Customer name (from Firebase Auth)
            val currentUser = auth.currentUser
            val customerName = currentUser?.displayName ?: "Customer"
            tvCustomerName.text = "Name: $customerName"

            // Date and time
            tvOrderDate.text = "Date: $orderDate"
            tvOrderTime.text = "Time: $orderTime"
            tvOrderType.text = "Type: $orderType"

            // Delivery address (only show if delivery)
            if (orderType == "Delivery" && userAddress.isNotEmpty()) {
                tvDeliveryAddress.text = "Address: $userAddress"
                tvDeliveryAddress.visibility = android.view.View.VISIBLE
            } else {
                tvDeliveryAddress.visibility = android.view.View.GONE
            }

            // Display order items with calculations
            displayOrderItems()

            // Display summary
            tvSubtotal.text = "₱${String.format("%.2f", orderTotal.waterSubtotal)}"

            // Always show transaction fee
            tvTransactionFee.text = "₱${String.format("%.2f", transactionFee)}"
            layoutTransactionFee.visibility = android.view.View.VISIBLE

            // Show delivery fee only for delivery orders
            if (orderTotal.deliveryFee > 0 && orderType == "Delivery") {
                tvDeliveryFee.text = "₱${String.format("%.2f", orderTotal.deliveryFee)}"
                layoutDeliveryFee.visibility = android.view.View.VISIBLE
            } else {
                layoutDeliveryFee.visibility = android.view.View.GONE
            }

            // Calculate grand total: subtotal + delivery fee + transaction fee
            val grandTotal = orderTotal.waterSubtotal +
                    (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                    transactionFee
            tvGrandTotal.text = "₱${String.format("%.2f", grandTotal)}"

            // Generate reference number
            referenceNumber = generateReferenceNumber()
            tvReferenceNumber.text = "Reference #: $referenceNumber"

            // Additional details
            if (additionalDetails.isNotEmpty()) {
                tvAdditionalDetails.text = "Notes: $additionalDetails"
                tvAdditionalDetails.visibility = android.view.View.VISIBLE
            } else {
                tvAdditionalDetails.visibility = android.view.View.GONE
            }

            // Payment method
            tvPaymentMethod.text = "Payment: Cash on Delivery"

        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("SetTextI18n", "DefaultLocale")
    private fun displayOrderItems() {
        // Pure Water
        if (orderTotal.pureWaterQty > 0) {
            tvPureWaterDetails.text = "${orderTotal.pureWaterQty} gallon(s) Pure Water: ₱${String.format("%.2f", orderTotal.pureWaterTotal)}"
            tvPureWaterDetails.visibility = android.view.View.VISIBLE
        } else {
            tvPureWaterDetails.visibility = android.view.View.GONE
        }

        // Spring Water
        if (orderTotal.springWaterQty > 0) {
            tvSpringWaterDetails.text = "${orderTotal.springWaterQty} liter(s) Spring Water: ₱${String.format("%.2f", orderTotal.springWaterTotal)}"
            tvSpringWaterDetails.visibility = android.view.View.VISIBLE
        } else {
            tvSpringWaterDetails.visibility = android.view.View.GONE
        }

        // Mineral Water
        if (orderTotal.mineralWaterQty > 0) {
            tvMineralWaterDetails.text = "${orderTotal.mineralWaterQty} gallon(s) Mineral Water: ₱${String.format("%.2f", orderTotal.mineralWaterTotal)}"
            tvMineralWaterDetails.visibility = android.view.View.VISIBLE
        } else {
            tvMineralWaterDetails.visibility = android.view.View.GONE
        }
    }

    private fun generateReferenceNumber(): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())

        synchronized(this) {
            referenceCounter++
        }

        // Format counter to at least 3 digits (e.g., 001, 012, 123)
        val counterString = referenceCounter.toString().padStart(3, '0')

        return "$date-$counterString"
    }

    private fun getCustomerPhoneFromDatabase(userId: String, callback: (String) -> Unit) {
        try {
            val database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")
            val userRef = database.reference.child("users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Try different possible field names
                        val phone = when {
                            snapshot.hasChild("phone") -> snapshot.child("phone").getValue(String::class.java)
                            snapshot.hasChild("phoneNumber") -> snapshot.child("phoneNumber").getValue(String::class.java)
                            snapshot.hasChild("contactNumber") -> snapshot.child("contactNumber").getValue(String::class.java)
                            snapshot.hasChild("number") -> snapshot.child("number").getValue(String::class.java)
                            else -> null
                        } ?: ""

                        Log.d("OrderConfirmation", "Fetched phone: $phone")
                        callback(phone)
                    } else {
                        Log.d("OrderConfirmation", "User profile not found")
                        callback("")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("OrderConfirmation", "Failed to fetch phone: ${error.message}")
                    callback("")
                }
            })
        } catch (e: Exception) {
            Log.e("OrderConfirmation", "Error fetching phone: ${e.message}")
            callback("")
        }
    }

    private fun createOrderObject(customerId: String, orderId: String, customerPhone: String): Order {
        val currentUser = auth.currentUser
        val customerName = currentUser?.displayName ?: "Customer"

        // Calculate grand total correctly
        val grandTotal = orderTotal.waterSubtotal +
                (if (orderType == "Delivery") orderTotal.deliveryFee else 0.0) +
                transactionFee

        return Order(
            orderId = orderId,
            stationId = currentStation?.id ?: "",  // ✅ Use station's ID, not customer's ID
            stationName = currentStation?.stationName ?: "",
            customerId = customerId,
            customerName = customerName,
            customerPhone = customerPhone,  // Use the fetched phone
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
            locationDetails = userAddress,
            deliveryAddress = if (orderType == "Delivery") userAddress else "",
            deliveryLatitude = if (orderType == "Delivery") userLatitude else 0.0,
            deliveryLongitude = if (orderType == "Delivery") userLongitude else 0.0,
            additionalDetails = additionalDetails,
            paymentMethod = "Cash on Delivery",
            status = "Pending",
            createdAt = System.currentTimeMillis(),
            referenceNumber = referenceNumber
        )
    }

    private fun debugUserData(userId: String) {
        val database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")
        val userRef = database.reference.child("users").child(userId)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("UserDebug", "User snapshot exists: ${snapshot.exists()}")
                if (snapshot.exists()) {
                    Log.d("UserDebug", "User data: ${snapshot.value}")
                    // Log all child keys
                    for (child in snapshot.children) {
                        Log.d("UserDebug", "Field: ${child.key} = ${child.value}")
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("UserDebug", "Error: ${error.message}")
            }
        })
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
            // Fetch user phone from database first
            getCustomerPhoneFromDatabase(currentUser.uid) { customerPhone ->
                try {
                    // Generate order ID
                    val database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")
                    val orderId = database.reference.child("orders").push().key
                        ?: System.currentTimeMillis().toString()

                    // Create order with phone number
                    val order = createOrderObject(currentUser.uid, orderId, customerPhone)

                    val orderRef = database.reference.child("orders").child(orderId)

                    // Save to Firebase
                    orderRef.setValue(order)
                        .addOnSuccessListener {
                            // Navigate to success activity
                            navigateToOrderSuccess(order)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save order: ${e.message}", Toast.LENGTH_SHORT).show()
                            btnConfirmOrder.isEnabled = true
                            btnConfirmOrder.text = "Confirm Order"
                        }

                } catch (e: Exception) {
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
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

            // Pass water details
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