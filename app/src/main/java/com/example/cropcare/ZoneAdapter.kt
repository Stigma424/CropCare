package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ZoneAdapter(
    private val zoneList: List<ZoneModel>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvZoneName: TextView = itemView.findViewById(R.id.tvZoneName)
        val tvRefresh: TextView = itemView.findViewById(R.id.tvRefresh)
        val tvDate: TextView = itemView.findViewById(R.id.tvDate)
        val tvTime: TextView = itemView.findViewById(R.id.tvTime)
        val tvAlert: TextView = itemView.findViewById(R.id.tvAlert)
        val tvN: TextView = itemView.findViewById(R.id.tvN)
        val tvStatusN: TextView = itemView.findViewById(R.id.tvStatusN)
        val tvP: TextView = itemView.findViewById(R.id.tvP)
        val tvStatusP: TextView = itemView.findViewById(R.id.tvStatusP)
        val tvK: TextView = itemView.findViewById(R.id.tvK)
        val tvStatusK: TextView = itemView.findViewById(R.id.tvStatusK)
        val tvSoilHealth: TextView = itemView.findViewById(R.id.tvSoilHealth)
        val btnViewMore: TextView = itemView.findViewById(R.id.btnViewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zone_card, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val zone = zoneList[position]
        holder.tvZoneName.text = zone.zoneName

        loadZoneReadings(zone.zoneId, zone.zoneName, holder)

        holder.tvRefresh.setOnClickListener {
            loadZoneReadings(zone.zoneId, zone.zoneName, holder)
            Toast.makeText(holder.itemView.context, "Refreshing ${zone.zoneName}...", Toast.LENGTH_SHORT).show()
        }

        holder.btnViewMore.setOnClickListener { onItemClick(zone.zoneId) }
    }

    private fun loadZoneReadings(zoneId: String, zoneName: String, holder: ZoneViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { sensors ->
                val deviceIds = sensors.mapNotNull { it.getString("deviceId") }.distinct()
                if (deviceIds.isEmpty()) {
                    holder.tvN.text = "0 mg/kg"
                    holder.tvStatusN.text = "No Sensor"
                    holder.tvP.text = "0 mg/kg"
                    holder.tvStatusP.text = "No Sensor"
                    holder.tvK.text = "0 mg/kg"
                    holder.tvStatusK.text = "No Sensor"
                    holder.tvSoilHealth.text = "Overall Soil Health  0%"
                    holder.tvDate.text = "No data"
                    holder.tvTime.text = ""
                    holder.tvAlert.text = "No sensors connected"
                    holder.tvAlert.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                db.collection("soil_data")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { soilDocs ->
                        if (soilDocs.isEmpty) {
                            holder.tvN.text = "0 mg/kg"
                            holder.tvStatusN.text = "No Data"
                            holder.tvP.text = "0 mg/kg"
                            holder.tvStatusP.text = "No Data"
                            holder.tvK.text = "0 mg/kg"
                            holder.tvStatusK.text = "No Data"
                            holder.tvSoilHealth.text = "Overall Soil Health  0%"
                            holder.tvDate.text = "No data"
                            holder.tvTime.text = ""
                            holder.tvAlert.text = "No soil data recorded"
                            holder.tvAlert.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }

                        // Take the LATEST reading per device in this zone
                        val latestDocs = soilDocs.documents
                            .groupBy { it.getString("deviceId") ?: "default" }
                            .mapNotNull { (_, docs) -> docs.maxByOrNull { parseAnyDate(it)?.time ?: 0L } }

                        if (latestDocs.isEmpty()) {
                            holder.tvN.text = "0 mg/kg"
                            holder.tvStatusN.text = "No Data"
                            holder.tvP.text = "0 mg/kg"
                            holder.tvStatusP.text = "No Data"
                            holder.tvK.text = "0 mg/kg"
                            holder.tvStatusK.text = "No Data"
                            holder.tvSoilHealth.text = "Overall Soil Health  0%"
                            holder.tvDate.text = "No data"
                            holder.tvTime.text = ""
                            holder.tvAlert.text = "No soil data recorded"
                            holder.tvAlert.visibility = View.VISIBLE
                            return@addOnSuccessListener
                        }

                        val newestDoc = latestDocs.maxByOrNull { parseAnyDate(it)?.time ?: 0L }
                        val latestDate = newestDoc?.let { parseAnyDate(it) }

                        if (latestDate != null) {
                            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
                            holder.tvDate.text = "As of ${dateFormat.format(latestDate)}"
                            holder.tvTime.text = "    ${timeFormat.format(latestDate)}"
                        } else {
                            holder.tvDate.text = "As of N/A"
                            holder.tvTime.text = ""
                        }

                        val count = latestDocs.size.toDouble()
                        var sumN = 0.0; var sumP = 0.0; var sumK = 0.0
                        var sumMoisture = 0.0; var sumPh = 0.0

                        for (doc in latestDocs) {
                            sumN += getDoubleValue(doc, "nitrogen", "n", "N")
                            sumP += getDoubleValue(doc, "phosphorus", "p", "P")
                            sumK += getDoubleValue(doc, "potassium", "k", "K")
                            sumMoisture += getDoubleValue(doc, "moisture", "humidity")
                            sumPh += getDoubleValue(doc, "ph", "pH")
                        }

                        val avgN = sumN / count
                        val avgP = sumP / count
                        val avgK = sumK / count
                        val avgMoisture = sumMoisture / count
                        val avgPh = sumPh / count

                        holder.tvN.text = String.format(Locale.US, "%.0f mg/kg", avgN)
                        holder.tvStatusN.text = SoilUtils.getStatus("N", avgN)

                        holder.tvP.text = String.format(Locale.US, "%.0f mg/kg", avgP)
                        holder.tvStatusP.text = SoilUtils.getStatus("P", avgP)

                        holder.tvK.text = String.format(Locale.US, "%.0f mg/kg", avgK)
                        holder.tvStatusK.text = SoilUtils.getStatus("K", avgK)

                        val healthScore = calculateSoilHealthPercentage(avgN, avgP, avgK, avgMoisture, avgPh)
                        holder.tvSoilHealth.text = "Overall Soil Health  $healthScore%"

                        if (avgN < 20.0) {
                            holder.tvAlert.text = "Low nitrogen - action needed"
                            holder.tvAlert.visibility = View.VISIBLE
                        } else {
                            holder.tvAlert.visibility = View.GONE
                        }

                        // Trigger notifications and log to notification history when sensor data is received/loaded
                        NotificationHelper.checkAndTriggerSoilAlerts(
                            holder.itemView.context,
                            zoneName,
                            avgN, avgP, avgK, avgMoisture, avgPh,
                            ""
                        )
                    }
            }
    }

    private fun getDoubleValue(doc: DocumentSnapshot, vararg keys: String): Double {
        for (key in keys) {
            val valDouble = doc.getDouble(key)
            if (valDouble != null) return valDouble

            val valLong = doc.getLong(key)
            if (valLong != null) return valLong.toDouble()

            val valString = doc.getString(key)
            if (valString != null) {
                val parsed = valString.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
        return 0.0
    }

    private fun calculateSoilHealthPercentage(n: Double, p: Double, k: Double, m: Double, ph: Double): Int {
        var score = 0
        if (n in 20.0..80.0) score += 20 else if (n > 10) score += 10
        if (p in 10.0..50.0) score += 20 else if (p > 5) score += 10
        if (k in 20.0..60.0) score += 20 else if (k > 10) score += 10
        if (m in 30.0..75.0) score += 20 else if (m in 15.0..85.0) score += 10
        if (ph in 5.8..7.5) score += 20 else if (ph in 5.0..8.0) score += 10
        return score
    }

    private fun parseAnyDate(doc: DocumentSnapshot): Date? {
        val keys = arrayOf("timestamp", "lastScanTimestamp", "createdAt", "date", "updatedAt")
        for (key in keys) {
            val rawValue = doc.get(key) ?: continue
            when (rawValue) {
                is Timestamp -> return rawValue.toDate()
                is Long -> return Date(rawValue)
                is Double -> return Date(rawValue.toLong())
                is String -> {
                    val formats = arrayOf(
                        "yyyy-MM-dd HH:mm:ss",
                        "MMMM dd, yyyy 'at' h:mm:ss a z",
                        "MMMM dd, yyyy - hh:mm:ss a"
                    )
                    for (fmt in formats) {
                        try {
                            val parsed = SimpleDateFormat(fmt, Locale.US).parse(rawValue)
                            if (parsed != null) return parsed
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return null
    }

    override fun getItemCount(): Int = zoneList.size
}
