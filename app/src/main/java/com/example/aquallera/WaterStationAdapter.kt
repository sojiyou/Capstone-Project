package com.example.aquallera

import android.content.Intent
import android.location.Location
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WaterStationAdapter(
    private var stations: List<WaterStation>,
    private var userLocation: Location?,
    private val onViewOnMapClick: (WaterStation) -> Unit
) : RecyclerView.Adapter<WaterStationAdapter.StationViewHolder>() {

    class StationViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvStationName: TextView = view.findViewById(R.id.tvStationName)
        val tvStationStatus: TextView = view.findViewById(R.id.tvStationStatus)
        val tvStationAddress: TextView = view.findViewById(R.id.tvStationAddress)
        val tvStationDistance: TextView = view.findViewById(R.id.tvStationDistance)
        val tvPurePrice: TextView = view.findViewById(R.id.tvPurePrice)
        val tvSpringPrice: TextView = view.findViewById(R.id.tvSpringPrice)
        val tvMineralPrice: TextView = view.findViewById(R.id.tvMineralPrice)
        val btnViewOnMap: Button = view.findViewById(R.id.btnViewOnMap)
        val btnOrderNow: Button = view.findViewById(R.id.btnOrderNow)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_water_station, parent, false)
        return StationViewHolder(view)
    }

    override fun onBindViewHolder(holder: StationViewHolder, position: Int) {
        val station = stations[position]
        val context = holder.itemView.context

        // Station name
        holder.tvStationName.text = station.stationName

        // Online/Offline status based on isOnline field
        val isOnline = station.isOnline
        val isApproved = station.status == "approved"

        if (isApproved && isOnline) {
            // Station is approved and owner is online
            holder.tvStationStatus.text = "● Online"
            holder.tvStationStatus.setTextColor(context.getColor(android.R.color.holo_green_dark))
            holder.btnOrderNow.isEnabled = true
            holder.btnOrderNow.alpha = 1.0f
        } else if (isApproved && !isOnline) {
            // Station is approved but owner is offline
            holder.tvStationStatus.text = "● Offline"
            holder.tvStationStatus.setTextColor(context.getColor(android.R.color.holo_orange_dark))
            holder.btnOrderNow.isEnabled = true
            holder.btnOrderNow.alpha = 1.0f
        } else {
            // Station is not approved (closed)
            holder.tvStationStatus.text = "● Closed"
            holder.tvStationStatus.setTextColor(context.getColor(android.R.color.holo_red_dark))
            holder.btnOrderNow.isEnabled = false
            holder.btnOrderNow.alpha = 0.5f
        }

        // Address
        holder.tvStationAddress.text = station.address.ifEmpty { "Address not available" }

        // Calculate distance using your existing WaterStation function
        if (userLocation != null && station.latitude != 0.0 && station.longitude != 0.0) {
            val distanceResult = station.calculateDistanceTo(
                userLocation!!.latitude,
                userLocation!!.longitude
            )
            holder.tvStationDistance.text = "${distanceResult.formatted} away"
        } else {
            holder.tvStationDistance.text = "Distance unavailable"
        }

        // ✅ Handle nullable pricing with safe defaults
        holder.tvPurePrice.text = "₱${String.format("%.2f", station.pricing_gallon_pure ?: 0.0)}"
        holder.tvSpringPrice.text = "₱${String.format("%.2f", station.pricing_liter_spring ?: 0.0)}"
        holder.tvMineralPrice.text = "₱${String.format("%.2f", station.pricing_gallon_mineral ?: 0.0)}"

        // View on Map button
        holder.btnViewOnMap.setOnClickListener {
            onViewOnMapClick(station)
        }

        // Order Now button
        holder.btnOrderNow.setOnClickListener {
            if (station.status == "approved") {
                val intent = Intent(context, CreateOrderActivity::class.java)
                intent.putExtra("WATER_STATION", station)
                context.startActivity(intent)
            } else {
                android.widget.Toast.makeText(
                    context,
                    "This station is currently not accepting orders",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount() = stations.size

    fun updateStations(newStations: List<WaterStation>) {
        stations = newStations
        notifyDataSetChanged()
    }

    fun updateUserLocation(newLocation: Location?) {
        userLocation = newLocation
        notifyDataSetChanged()
    }
}