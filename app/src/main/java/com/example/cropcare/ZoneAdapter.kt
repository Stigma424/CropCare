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
        val tvHealthStatus: TextView? = itemView.findViewById(R.id.tvHealthStatus)
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
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // 1. Try fetching soil_data directly by zoneId
        db.collection("soil_data")
            .whereEqualTo("zoneId", zoneId)
            .get()
            .addOnSuccessListener { directDocs ->
                if (!directDocs.isEmpty) {
                    val latestDocs = directDocs.documents
                        .groupBy { it.getString("deviceId") ?: "default" }
                        .mapNotNull { (_, docs) -> docs.maxByOrNull { parseAnyDate(it)?.time ?: 0L } }

                    bindSoilDataToHolder(latestDocs, holder, zoneName)
                } else {
                    // 2. Fallback: fetch sensors for zoneId -> fetch soil_data by deviceId
                    db.collection("sensors")
                        .whereEqualTo("zoneId", zoneId)
                        .whereEqualTo("userId", userId)
                        .get()
                        .addOnSuccessListener { sensors ->
                            val deviceIds = sensors.mapNotNull { it.getString("deviceId") }.distinct()
                            if (deviceIds.isEmpty()) {
                                bindEmptyDataToHolder(holder, "No sensors connected")
                                return@addOnSuccessListener
                            }

                            db.collection("soil_data")
                                .whereIn("deviceId", deviceIds)
                                .get()
                                .addOnSuccessListener { soilDocs ->
                                    if (soilDocs.isEmpty) {
                                        bindEmptyDataToHolder(holder, "No soil data recorded")
                                        return@addOnSuccessListener
                                    }

                                    val latestDocs = soilDocs.documents
                                        .groupBy { it.getString("deviceId") ?: "default" }
                                        .mapNotNull { (_, docs) -> docs.maxByOrNull { parseAnyDate(it)?.time ?: 0L } }

                                    bindSoilDataToHolder(latestDocs, holder, zoneName)
                                }
                                .addOnFailureListener {
                                    bindEmptyDataToHolder(holder, "No soil data recorded")
                                }
                        }
                        .addOnFailureListener {
                            bindEmptyDataToHolder(holder, "No sensors connected")
                        }
                }
            }
            .addOnFailureListener {
                bindEmptyDataToHolder(holder, "No soil data recorded")
            }
    }

    private fun bindSoilDataToHolder(latestDocs: List<DocumentSnapshot>, holder: ZoneViewHolder, zoneName: String) {
        if (latestDocs.isEmpty()) {
            bindEmptyDataToHolder(holder, "No soil data recorded")
            return
        }

        val newestDoc = latestDocs.maxByOrNull { parseAnyDate(it)?.time ?: 0L }
        val latestDate = newestDoc?.let { parseAnyDate(it) }

        if (latestDate != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
            holder.tvDate.text = "As of ${dateFormat.format(latestDate)}"
            holder.tvTime.text = timeFormat.format(latestDate)
        } else {
            holder.tvDate.text = "As of N/A"
            holder.tvTime.text = ""
        }

        val count = latestDocs.size.toDouble()
        val avgN = latestDocs.sumOf { getDoubleValue(it, "nitrogen", "n", "N", "nitro") } / count
        val avgP = latestDocs.sumOf { getDoubleValue(it, "phosphorus", "p", "P", "phos") } / count
        val avgK = latestDocs.sumOf { getDoubleValue(it, "potassium", "k", "K", "pot") } / count
        val avgMoisture = latestDocs.sumOf { getDoubleValue(it, "moisture", "humidity", "mois") } / count
        val avgPh = latestDocs.sumOf { getDoubleValue(it, "ph", "pH", "PH") } / count

        holder.tvN.text = String.format(Locale.US, "%.0f mg/kg", avgN)
        holder.tvStatusN.text = SoilUtils.getStatus("N", avgN)

        holder.tvP.text = String.format(Locale.US, "%.0f mg/kg", avgP)
        holder.tvStatusP.text = SoilUtils.getStatus("P", avgP)

        holder.tvK.text = String.format(Locale.US, "%.0f mg/kg", avgK)
        holder.tvStatusK.text = SoilUtils.getStatus("K", avgK)

        var score = 0
        if (avgN >= 20) score += 20
        if (avgP >= 10) score += 20
        if (avgK >= 100) score += 20
        if (avgMoisture in 40.0..80.0) score += 20
        if (avgPh in 5.5..7.5) score += 20

        val healthStatus = when {
            score >= 80 -> "Optimal"
            score >= 40 -> "Fair"
            else -> "Poor"
        }

        holder.tvHealthStatus?.text = healthStatus
        holder.tvSoilHealth.text = "$score%"

        if (avgN < 20.0) {
            holder.tvAlert.text = "Low nitrogen - action needed"
            holder.tvAlert.visibility = View.VISIBLE
        } else {
            holder.tvAlert.visibility = View.GONE
        }

        NotificationHelper.checkAndTriggerSoilAlerts(
            holder.itemView.context,
            zoneName,
            avgN, avgP, avgK, avgMoisture, avgPh,
            ""
        )
    }

    private fun bindEmptyDataToHolder(holder: ZoneViewHolder, alertMsg: String) {
        holder.tvN.text = "0 mg/kg"
        holder.tvStatusN.text = "No Data"
        holder.tvP.text = "0 mg/kg"
        holder.tvStatusP.text = "No Data"
        holder.tvK.text = "0 mg/kg"
        holder.tvStatusK.text = "No Data"
        holder.tvHealthStatus?.text = "Poor"
        holder.tvSoilHealth.text = "0%"
        holder.tvDate.text = "No data"
        holder.tvTime.text = ""
        holder.tvAlert.text = alertMsg
        holder.tvAlert.visibility = View.VISIBLE
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