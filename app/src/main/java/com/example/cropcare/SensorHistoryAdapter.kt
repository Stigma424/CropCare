package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SensorHistoryAdapter(
    private val history: List<SensorHistoryReading>
) : RecyclerView.Adapter<SensorHistoryAdapter.HistoryHolder>() {

    class HistoryHolder(view: View) : RecyclerView.ViewHolder(view) {
        val date: TextView = view.findViewById(R.id.tvHistoryDate)
        val time: TextView = view.findViewById(R.id.tvHistoryTime)
        val status: TextView = view.findViewById(R.id.tvHistoryStatus)
        val nitrogen: TextView = view.findViewById(R.id.tvHistoryN)
        val phosphorus: TextView = view.findViewById(R.id.tvHistoryP)
        val potassium: TextView = view.findViewById(R.id.tvHistoryK)
        val moisture: TextView = view.findViewById(R.id.tvHistoryMoisture)
        val ph: TextView = view.findViewById(R.id.tvHistoryPh)
        val temp: TextView = view.findViewById(R.id.tvHistoryTemp)
        val ec: TextView = view.findViewById(R.id.tvHistoryEc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryHolder, position: Int) {
        val item = history[position]
        val previous = if (position > 0) history[position - 1] else null
        val showDate = position == 0 || dayKey(item.recordedAtMs) !=
                dayKey(previous?.recordedAtMs ?: 0L)

        holder.date.visibility = if (showDate) View.VISIBLE else View.GONE
        if (showDate) holder.date.text = dateTitle(item.recordedAtMs)

        holder.time.text = if (item.recordedAtMs > 0L) {
            SimpleDateFormat("h:mm a", Locale.getDefault())
                .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(Date(item.recordedAtMs))
        } else "Time unavailable"

        // 'RECORDED' describes a saved historical reading.
        // A past timestamp is not proof the sensor is online now.
        holder.status.text = listOf("nitrogen" to item.nitrogen, "phosphorus" to item.phosphorus, "potassium" to item.potassium, "moisture" to item.moisture, "ph" to item.ph, "temperature" to item.temperature, "ec" to item.ec).map { ReadingStatus.label(it.first, it.second) }.distinct().let { states -> if (states.all { it == "GOOD" }) "GOOD" else states.filter { it != "GOOD" }.joinToString(" · ") }

        ReadingStatus.style(holder.status)
        holder.nitrogen.text = metric(item.nitrogen, "mg/kg", 1)
        holder.phosphorus.text = metric(item.phosphorus, "mg/kg", 1)
        holder.potassium.text = metric(item.potassium, "mg/kg", 1)
        holder.moisture.text = metric(item.moisture, "%", 1)
        holder.ph.text = metric(item.ph, "", 2)
        holder.temp.text = metric(item.temperature, "°C", 1)
        holder.ec.text = metric(item.ec, "", 1)
    }

    override fun getItemCount(): Int = history.size

    private fun metric(value: Double?, unit: String, decimals: Int): String {
        if (value == null) return "—"
        val raw = if (value == value.toLong().toDouble()) {
            value.toLong().toString()
        } else {
            String.format(Locale.US, "%.${decimals}f", value)
        }
        return if (unit.isEmpty()) raw else "$raw $unit"
    }

    private fun dayKey(milliseconds: Long): String =
        if (milliseconds <= 0L) "unknown" else
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(Date(milliseconds))

    private fun dateTitle(milliseconds: Long): String {
        if (milliseconds <= 0L) return "UNKNOWN DATE"

        val date = Date(milliseconds)
        val today = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila"))
        val yesterday = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila")).apply {
            add(Calendar.DAY_OF_YEAR, -1)
        }
        val recordDate = Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila")).apply { time = date }
        val label = when {
            sameDay(today, recordDate) -> "TODAY"
            sameDay(yesterday, recordDate) -> "YESTERDAY"
            else -> SimpleDateFormat("EEEE", Locale.US)
                .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(date).uppercase(Locale.US)
        }
        val monthDay = SimpleDateFormat("MMMM dd, yyyy", Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(date).uppercase(Locale.US)
        return "$label · $monthDay"
    }

    private fun sameDay(a: Calendar, b: Calendar): Boolean =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
}


