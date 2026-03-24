package com.example.aquallera

import android.location.Location
import com.google.firebase.database.PropertyName
import java.io.Serializable
import kotlin.math.*

data class WaterStation(

    val id: String = "",

    // Renamed backing field to avoid Kotlin auto-generating isOnline() getter
    // which conflicts with Firebase boolean deserialization
    @get:PropertyName("isOnline")
    @set:PropertyName("isOnline")
    var online: Boolean = false,

    @PropertyName("status") val status: String = "pending",
    @PropertyName("stationName") val stationName: String = "",
    @PropertyName("ownerName") val ownerName: String = "",
    @PropertyName("email") val email: String = "",
    @PropertyName("phone") val phone: String = "",
    @PropertyName("address") val address: String = "",
    @PropertyName("city") val city: String = "",
    @PropertyName("state") val state: String = "",
    @PropertyName("zipCode") val zipCode: String = "",

    @PropertyName("latitude") val latitude: Double = 0.0,
    @PropertyName("longitude") val longitude: Double = 0.0,

    @PropertyName("pricing_gallon_pure") val pricing_gallon_pure: Double? = null,
    @PropertyName("pricing_liter_spring") val pricing_liter_spring: Double? = null,
    @PropertyName("pricing_gallon_mineral") val pricing_gallon_mineral: Double? = null,
    @PropertyName("pricing_delivery_fee") val pricing_delivery_fee: Double? = null,

    @PropertyName("deliveryRadius") val deliveryRadius: Int = 5,
    @PropertyName("deliveryHours") val deliveryHours: List<String>? = null,
    @PropertyName("businessHours") val businessHours: Map<String, String> = emptyMap(),
    @PropertyName("serviceTypes") val serviceTypes: List<String> = emptyList(),
    @PropertyName("createdAt") val createdAt: String = "",
    @PropertyName("updatedAt") val updatedAt: String = "",

    ) : Serializable {

    // Convenience property so the rest of the code still works
    val isOnline: Boolean get() = online

    // Main distance calculation function
    fun calculateDistanceTo(userLat: Double, userLon: Double): DistanceResult {
        if (latitude == 0.0 && longitude == 0.0) {
            return DistanceResult(
                meters = -1f,
                formatted = "Unknown",
                accuracyScore = 0.0
            )
        }

        val distanceMeters = calculateDistanceUsingAndroid(latitude, longitude, userLat, userLon)

        return DistanceResult(
            meters = distanceMeters,
            formatted = formatDistance(distanceMeters.toDouble()),
            accuracyScore = calculateAccuracyScore(latitude, longitude)
        )
    }

    private fun calculateDistanceUsingAndroid(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Float {
        val results = FloatArray(1)
        return try {
            Location.distanceBetween(lat1, lon1, lat2, lon2, results)
            results[0]
        } catch (e: Exception) {
            calculateDistanceUsingHaversine(lat1, lon1, lat2, lon2).toFloat()
        }
    }

    private fun calculateDistanceUsingHaversine(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371000.0

        val latRad1 = Math.toRadians(lat1)
        val lonRad1 = Math.toRadians(lon1)
        val latRad2 = Math.toRadians(lat2)
        val lonRad2 = Math.toRadians(lon2)

        val deltaLat = latRad2 - latRad1
        val deltaLon = lonRad2 - lonRad1

        val a = sin(deltaLat / 2).pow(2) +
                cos(latRad1) * cos(latRad2) *
                sin(deltaLon / 2).pow(2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun isWithinDeliveryRange(userLat: Double, userLon: Double): Boolean {
        val distance = calculateDistanceTo(userLat, userLon).meters
        val radiusMeters = deliveryRadius * 1000
        return distance <= radiusMeters
    }

    fun getDeliveryRadiusInt(): Int = deliveryRadius

    fun hasDeliveryService(): Boolean {
        return serviceTypes.contains("delivery")
    }

    fun getFormattedDeliveryHours(): List<String> {
        return deliveryHours?.map { time -> formatTime(time) } ?: emptyList()
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

    private fun formatDistance(meters: Double): String {
        return when {
            meters < 0 -> "Unknown"
            meters < 1000 -> "${meters.roundToInt()} m"
            meters < 10000 -> "${(meters / 1000).roundToDecimal(1)} km"
            else -> "${(meters / 1000).roundToInt()} km"
        }
    }

    private fun calculateAccuracyScore(lat: Double, lon: Double): Double {
        return when {
            lat == 0.0 || lon == 0.0 -> 0.0
            lat < -90 || lat > 90 || lon < -180 || lon > 180 -> 0.1
            else -> 1.0
        }
    }

    private fun Double.roundToDecimal(places: Int): String {
        return "%.${places}f".format(this)
    }

    private fun Double.roundToInt(): Int = round(this).toInt()
}

data class DistanceResult(
    val meters: Float,
    val formatted: String,
    val accuracyScore: Double
)