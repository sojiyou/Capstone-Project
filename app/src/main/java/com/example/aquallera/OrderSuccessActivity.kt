package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class OrderSuccessActivity : AppCompatActivity() {

    private lateinit var tvHeader: TextView
    private lateinit var tvStationName: TextView
    private lateinit var tvCustomerName: TextView
    private lateinit var tvOrderDate: TextView
    private lateinit var tvOrderTime: TextView
    private lateinit var tvOrderType: TextView
    private lateinit var tvTransactionFee: TextView
    private lateinit var tvGrandTotal: TextView
    private lateinit var tvReferenceNumber: TextView
    private lateinit var tvOrderStatus: TextView
    private lateinit var btnContinue: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        try {
            initializeViews()
            setupClickListeners()
            displayOrderDetails()
        } catch (e: Exception) {
            e.printStackTrace()
            // If something fails, show a simple success message
            tvHeader.text = "Order Successful!"
            finish()
        }
    }

    private fun initializeViews() {
        // Initialize ONLY the views that exist in your layout
        tvHeader = findViewById(R.id.tvHeader)
        btnContinue = findViewById(R.id.btnContinue)

        // Try to initialize optional views - wrap in try-catch
        try {
            tvStationName = findViewById(R.id.tvStationName)
            tvCustomerName = findViewById(R.id.tvCustomerName)
            tvOrderDate = findViewById(R.id.tvOrderDate)
            tvOrderTime = findViewById(R.id.tvOrderTime)
            tvOrderType = findViewById(R.id.tvOrderType)
            tvTransactionFee = findViewById(R.id.tvTransactionFee)
            tvGrandTotal = findViewById(R.id.tvGrandTotal)
            tvReferenceNumber = findViewById(R.id.tvReferenceNumber)
            tvOrderStatus = findViewById(R.id.tvOrderStatus)
        } catch (e: Exception) {
            // Some views might not exist - that's okay
        }
    }

    private fun setupClickListeners() {
        btnContinue.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun displayOrderDetails() {
        tvHeader.text = "Order Successful!"

        // Safely try to display details if views exist
        try {
            val stationName = intent.getStringExtra("STATION_NAME") ?: "Water Station"
            val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Customer"
            val orderDate = intent.getStringExtra("ORDER_DATE") ?: ""
            val orderTime = intent.getStringExtra("ORDER_TIME") ?: ""
            val orderType = intent.getStringExtra("ORDER_TYPE") ?: "Pickup"
            val grandTotal = intent.getDoubleExtra("GRAND_TOTAL", 0.0)
            val referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: ""
            val orderStatus = intent.getStringExtra("ORDER_STATUS") ?: "Pending"
            val transactionFee = intent.getDoubleExtra("TRANSACTION_FEE", 20.0)

            // Only set text if view was initialized
            if (::tvStationName.isInitialized) tvStationName.text = stationName
            if (::tvCustomerName.isInitialized) tvCustomerName.text = "Name: $customerName"
            if (::tvOrderDate.isInitialized) tvOrderDate.text = "Date: $orderDate"
            if (::tvOrderTime.isInitialized) tvOrderTime.text = "Time: $orderTime"
            if (::tvOrderType.isInitialized) tvOrderType.text = "Type: $orderType"
            if (::tvGrandTotal.isInitialized) tvGrandTotal.text = "Total: ₱${String.format("%.2f", grandTotal)}"
            if (::tvReferenceNumber.isInitialized) tvReferenceNumber.text = "Reference #: $referenceNumber"
            if (::tvOrderStatus.isInitialized) tvOrderStatus.text = "Status: $orderStatus"
            if (::tvTransactionFee.isInitialized) tvTransactionFee.text = "Fee: ₱${String.format("%.2f", transactionFee)}"

        } catch (e: Exception) {
            // If getting intent data fails, still show success
            e.printStackTrace()
        }
    }
}