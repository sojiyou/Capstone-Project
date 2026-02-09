// File: Order.kt
package com.example.aquallera

import java.io.Serializable

data class Order(
    val orderId: String = "",
    val stationId: String = "",
    val stationName: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val customerPhone: String = "",
    val orderType: String = "", // "Delivery" or "Pickup"
    val date: String = "",
    val time: String = "",
    val pureWaterQty: Int = 0,
    val springWaterQty: Int = 0,
    val mineralWaterQty: Int = 0,
    val pureWaterPrice: Double = 0.0,
    val springWaterPrice: Double = 0.0,
    val mineralWaterPrice: Double = 0.0,
    val pureWaterTotal: Double = 0.0,
    val springWaterTotal: Double = 0.0,
    val mineralWaterTotal: Double = 0.0,
    val waterSubtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val transactionFee: Double = 0.0,
    val grandTotal: Double = 0.0,
    val locationDetails: String = "",
    val deliveryAddress: String = "",
    val deliveryLatitude: Double = 0.0,
    val deliveryLongitude: Double = 0.0,
    val additionalDetails: String = "",
    val paymentMethod: String = "Cash on Delivery",
    val status: String = "Pending", // Pending, Confirmed, Completed, Cancelled
    val createdAt: Long = System.currentTimeMillis(),
    val referenceNumber: String = "" // Added this field
) : Serializable