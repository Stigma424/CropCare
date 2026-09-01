package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorAdapter(
    private val sensors: List<SensorModel>,
    private val onDeleteClick: (SensorModel) -> Unit,
    private val onViewDetailsClick: (SensorModel) -> Unit
) : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

    class SensorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSensorName: TextView = itemView.findViewById(R.id.tvSensorName)
        val tvLastScan: TextView = itemView.findViewById(R.id.tvLastScan)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDeleteSensor)
        val btnViewDetails: Button = itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SensorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(holder: SensorViewHolder, position: Int) {
        val sensor = sensors[position]
        holder.tvSensorName.text = sensor.sensorName

        val sdf = SimpleDateFormat("EEEE, MMMM dd, yyyy @ hh:mm a", Locale.getDefault())
        val formattedDate = if (sensor.lastScanTimestamp > 0) sdf.format(Date(sensor.lastScanTimestamp)) else "No scan data"
        holder.tvLastScan.text = formattedDate

        if (sensor.isOnline) {
            holder.tvStatus.text = "Active"
            holder.tvStatus.setTextColor(0xFF4CAF50.toInt())
        } else {
            holder.tvStatus.text = "Offline"
            holder.tvStatus.setTextColor(0xFFF44336.toInt())
        }

        holder.btnDelete.setOnClickListener { onDeleteClick(sensor) }
        holder.btnViewDetails.setOnClickListener { onViewDetailsClick(sensor) }
    }

    override fun getItemCount(): Int = sensors.size
}