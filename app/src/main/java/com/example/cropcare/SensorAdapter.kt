
package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorAdapter(
    private val sensors: List<SensorModel>,
    private val onViewDetailsClick: (SensorModel) -> Unit,
    private val onHistory: ((SensorModel) -> Unit)? = null
) : RecyclerView.Adapter<SensorAdapter.SensorViewHolder>() {

    class SensorViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvSensorName: TextView =
            itemView.findViewById(R.id.tvSensorName)

        val tvLastScan: TextView =
            itemView.findViewById(R.id.tvLastScan)

        val tvStatus: TextView =
            itemView.findViewById(R.id.tvStatus)

        val btnHistory: Button = itemView.findViewById(R.id.btnSensorHistory)

        val btnViewDetails: Button =
            itemView.findViewById(R.id.btnViewDetails)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SensorViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_sensor, parent, false)

        return SensorViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: SensorViewHolder,
        position: Int
    ) {
        val sensor = sensors[position]

        holder.tvSensorName.text = sensor.sensorName

        val dateFormat = SimpleDateFormat(
            "EEEE, MMMM dd, yyyy @ hh:mm a",
            Locale.getDefault()
        )

        holder.tvLastScan.text = when {
            sensor.readingStatusUnavailable ->
                "Last reading: Unavailable"

            sensor.readingStatus == "unknown_time" -> "Last reading: timestamp unavailable"
            sensor.lastScanTimestamp > 0L ->
                "Last reading: " +
                        dateFormat.format(
                            Date(sensor.lastScanTimestamp)
                        )

            else ->
                "No readings received yet"
        }

        val isUnverified =
            sensor.pairingStatus.equals(
                "unverified",
                ignoreCase = true
            )

        when {
            sensor.readingStatusUnavailable -> {
                holder.tvStatus.text = "Status Unavailable"
                holder.tvStatus.setTextColor(
                    0xFF757575.toInt()
                )
            }

            sensor.readingStatus == "unknown_time" -> {
                holder.tvStatus.text = "Timestamp unavailable"
                holder.tvStatus.setTextColor(0xFF757575.toInt())
            }
            sensor.lastScanTimestamp <= 0L -> {
                holder.tvStatus.text =
                    "Awaiting First Reading"

                holder.tvStatus.setTextColor(
                    0xFFE69500.toInt()
                )
            }

            sensor.isOnline && isUnverified -> {
                holder.tvStatus.text =
                    "Recent Data - Unverified"

                holder.tvStatus.setTextColor(
                    0xFFE69500.toInt()
                )
            }

            sensor.isOnline -> {
                holder.tvStatus.text = "Recent reading"

                holder.tvStatus.setTextColor(
                    0xFF4CAF50.toInt()
                )
            }

            else -> {
                holder.tvStatus.text =
                    "No Recent Readings"

                holder.tvStatus.setTextColor(
                    0xFFF44336.toInt()
                )
            }
        }

        holder.btnHistory.visibility = if (onHistory == null) View.GONE else View.VISIBLE
        holder.btnHistory.setOnClickListener { onHistory?.invoke(sensor) }
        if (onHistory == null) {
            holder.tvStatus.text = if (sensor.pairingStatus == "qr_claimed") "QR paired" else "Unverified registration"
            holder.tvLastScan.text = "Device ID: ${sensor.deviceId}"
            holder.btnViewDetails.text = "Manage device"
        }
        holder.btnViewDetails.setOnClickListener {
            onViewDetailsClick(sensor)
        }
    }

    override fun getItemCount(): Int {
        return sensors.size
    }
}
