package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class ZoneAdapter(
    private val zoneList: List<ZoneModel>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvZoneName: TextView = itemView.findViewById(R.id.tvZoneName)
        val tvN: TextView = itemView.findViewById(R.id.tvN)
        val tvStatusN: TextView = itemView.findViewById(R.id.tvStatusN)
        val tvP: TextView = itemView.findViewById(R.id.tvP)
        val tvStatusP: TextView = itemView.findViewById(R.id.tvStatusP)
        val tvK: TextView = itemView.findViewById(R.id.tvK)
        val tvStatusK: TextView = itemView.findViewById(R.id.tvStatusK)
        val tvSoilHealth: TextView = itemView.findViewById(R.id.tvSoilHealth)
        val btnViewMore: Button = itemView.findViewById(R.id.btnViewMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zone_card, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val zone = zoneList[position]
        holder.tvZoneName.text = zone.zoneName

        loadZoneReadings(zone.zoneId, holder)

        holder.btnViewMore.setOnClickListener { onItemClick(zone.zoneId) }
    }

    private fun loadZoneReadings(zoneId: String, holder: ZoneViewHolder) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { sensors ->
                val deviceIds = sensors.mapNotNull { it.getString("deviceId") }
                if (deviceIds.isEmpty()) return@addOnSuccessListener

                db.collection("soil_data")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { soilDocs ->
                        if (soilDocs.isEmpty) return@addOnSuccessListener

                        var count = soilDocs.size()
                        var sumN = 0.0; var sumP = 0.0; var sumK = 0.0

                        for (doc in soilDocs) {
                            sumN += doc.getDouble("nitrogen") ?: 0.0
                            sumP += doc.getDouble("phosphorus") ?: 0.0
                            sumK += doc.getDouble("potassium") ?: 0.0
                        }

                        val avgN = sumN / count
                        val avgP = sumP / count
                        val avgK = sumK / count

                        holder.tvN.text = String.format(Locale.US, "%.0f mg/kg", avgN)
                        holder.tvStatusN.text = SoilUtils.getStatus("N", avgN)

                        holder.tvP.text = String.format(Locale.US, "%.0f mg/kg", avgP)
                        holder.tvStatusP.text = SoilUtils.getStatus("P", avgP)

                        holder.tvK.text = String.format(Locale.US, "%.0f mg/kg", avgK)
                        holder.tvStatusK.text = SoilUtils.getStatus("K", avgK)

                        holder.tvSoilHealth.text = "90%"
                    }
            }
    }

    override fun getItemCount(): Int = zoneList.size
}