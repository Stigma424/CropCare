package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ZoneAdapter(private val zones: List<ZoneMonitoringSnapshot>, private val onRefresh: () -> Unit,
    private val onItemClick: (String) -> Unit) : RecyclerView.Adapter<ZoneAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        fun text(id: Int, value: String) { itemView.findViewById<TextView>(id).text = value }
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_zone_card, parent, false))
    override fun getItemCount() = zones.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val zone = zones[position]; val averages = zone.averages
        holder.text(R.id.tvZoneName, zone.zone.zoneName)
        listOf(Triple("nitrogen", R.id.tvN, R.id.tvStatusN), Triple("phosphorus", R.id.tvP, R.id.tvStatusP),
            Triple("potassium", R.id.tvK, R.id.tvStatusK)).forEach { (key, valueId, statusId) ->
            val value = averages[key]
            holder.text(valueId, value?.let { String.format(Locale.US, "%.1f mg/kg", it) } ?: "—")
            val parameter = when(key) { "nitrogen" -> "N"; "phosphorus" -> "P"; else -> "K" }
            holder.text(statusId, ReadingStatus.label(key, value))
            ReadingStatus.style(holder.itemView.findViewById(statusId))
        }
        val score = zone.score
        holder.text(R.id.tvSoilHealth, score?.let { "${it.toInt()}%" } ?: "—")
        holder.text(R.id.tvHealthStatus, zone.scoreLabel)
        holder.text(R.id.tvDate, if (zone.newestAtMs > 0) SimpleDateFormat("MMM dd, yyyy", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }.format(Date(zone.newestAtMs)) else "Time unavailable")
        holder.text(R.id.tvTime, if (zone.newestAtMs > 0) SimpleDateFormat("h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }.format(Date(zone.newestAtMs)) else "")
        holder.text(R.id.tvAlert, zone.notice)
        holder.itemView.findViewById<View>(R.id.tvAlert).visibility =
            if (zone.notice.isBlank() || zone.notice == "Recent recorded measurements") View.GONE else View.VISIBLE
        holder.itemView.findViewById<View>(R.id.tvRefresh).setOnClickListener { onRefresh() }
        holder.itemView.findViewById<View>(R.id.btnViewMore).setOnClickListener { onItemClick(zone.zone.zoneId) }
    }
}


