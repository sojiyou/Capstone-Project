package com.example.aquallera

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

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
    private lateinit var btnCancelOrder: Button  // NEW

    // Data
    private var orderId: String = ""
    private var currentStatus: String = "Pending"

    // Firebase
    private lateinit var database: FirebaseDatabase
    private var statusListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        database = FirebaseDatabase.getInstance("https://aquallera-default-rtdb.asia-southeast1.firebasedatabase.app")

        try {
            initializeViews()
            setupClickListeners()
            displayOrderDetails()
            // Listen for real-time status updates from Firebase
            listenForStatusUpdates()
        } catch (e: Exception) {
            e.printStackTrace()
            tvHeader.text = "Order Successful!"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up listener to avoid memory leaks
        if (orderId.isNotEmpty()) {
            statusListener?.let {
                database.reference.child("orders").child(orderId).child("status").removeEventListener(it)
            }
        }
    }

    private fun initializeViews() {
        tvHeader = findViewById(R.id.tvHeader)
        btnContinue = findViewById(R.id.btnContinue)
        btnCancelOrder = findViewById(R.id.btnCancelOrder)  // NEW

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
            Log.e("OrderSuccess", "Some views not found: ${e.message}")
        }
    }

    private fun setupClickListeners() {
        btnContinue.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Cancel order button
        btnCancelOrder.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun displayOrderDetails() {
        tvHeader.text = "Order Successful!"

        try {
            orderId = intent.getStringExtra("ORDER_ID") ?: ""
            val stationName = intent.getStringExtra("STATION_NAME") ?: "Water Station"
            val customerName = intent.getStringExtra("CUSTOMER_NAME") ?: "Customer"
            val orderDate = intent.getStringExtra("ORDER_DATE") ?: ""
            val orderTime = intent.getStringExtra("ORDER_TIME") ?: ""
            val orderType = intent.getStringExtra("ORDER_TYPE") ?: "Pickup"
            val grandTotal = intent.getDoubleExtra("GRAND_TOTAL", 0.0)
            val referenceNumber = intent.getStringExtra("REFERENCE_NUMBER") ?: ""
            val orderStatus = intent.getStringExtra("ORDER_STATUS") ?: "Pending"
            val transactionFee = intent.getDoubleExtra("TRANSACTION_FEE", 20.0)

            currentStatus = orderStatus

            if (::tvStationName.isInitialized) tvStationName.text = stationName
            if (::tvCustomerName.isInitialized) tvCustomerName.text = "Name: $customerName"
            if (::tvOrderDate.isInitialized) tvOrderDate.text = "Date: $orderDate"
            if (::tvOrderTime.isInitialized) tvOrderTime.text = "Time: $orderTime"
            if (::tvOrderType.isInitialized) tvOrderType.text = "Type: $orderType"
            if (::tvGrandTotal.isInitialized) tvGrandTotal.text = "Total: ₱${String.format("%.2f", grandTotal)}"
            if (::tvReferenceNumber.isInitialized) tvReferenceNumber.text = "Reference #: $referenceNumber"
            if (::tvOrderStatus.isInitialized) tvOrderStatus.text = "Status: $orderStatus"
            if (::tvTransactionFee.isInitialized) tvTransactionFee.text = "Fee: ₱${String.format("%.2f", transactionFee)}"

            // Show or hide cancel button based on current status
            updateCancelButtonVisibility(orderStatus)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Listen for real-time status changes from Firebase
    private fun listenForStatusUpdates() {
        if (orderId.isEmpty()) return

        val statusRef = database.reference.child("orders").child(orderId).child("status")

        statusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStatus = snapshot.getValue(String::class.java) ?: return
                currentStatus = newStatus

                // Update status text on screen
                if (::tvOrderStatus.isInitialized) {
                    tvOrderStatus.text = "Status: $newStatus"
                }

                // Show/hide cancel button based on updated status
                updateCancelButtonVisibility(newStatus)

                Log.d("OrderSuccess", "Status updated to: $newStatus")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("OrderSuccess", "Failed to listen for status: ${error.message}")
            }
        }

        statusRef.addValueEventListener(statusListener!!)
    }

    // Only show cancel button when status is "Pending"
    private fun updateCancelButtonVisibility(status: String) {
        if (status.lowercase() == "pending") {
            btnCancelOrder.visibility = View.VISIBLE
        } else {
            btnCancelOrder.visibility = View.GONE
        }
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Cancel Order")
            .setMessage("Are you sure you want to cancel this order? This action cannot be undone.")
            .setPositiveButton("Yes, Cancel Order") { _, _ ->
                cancelOrder()
            }
            .setNegativeButton("No, Keep Order") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun cancelOrder() {
        if (orderId.isEmpty()) {
            Toast.makeText(this, "Order ID not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // Double-check status from Firebase before cancelling (prevent race condition)
        database.reference.child("orders").child(orderId).child("status")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val latestStatus = snapshot.getValue(String::class.java) ?: "Pending"

                    if (latestStatus.lowercase() != "pending") {
                        Toast.makeText(
                            this@OrderSuccessActivity,
                            "This order can no longer be cancelled. Status is already: $latestStatus",
                            Toast.LENGTH_LONG
                        ).show()
                        updateCancelButtonVisibility(latestStatus)
                        return
                    }

                    // Status is still Pending — proceed with cancellation
                    database.reference.child("orders").child(orderId).child("status")
                        .setValue("Cancelled")
                        .addOnSuccessListener {
                            Toast.makeText(
                                this@OrderSuccessActivity,
                                "Order cancelled successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Navigate back to Orders list
                            val intent = Intent(this@OrderSuccessActivity, OrdersActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this@OrderSuccessActivity,
                                "Failed to cancel order: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@OrderSuccessActivity,
                        "Error: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }
}