package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class HistoryItem(val timeFormatted: String, val valueFormatted: String)

class SensorHistoryAdapter(
    private val historyList: List<HistoryItem>
) : RecyclerView.Adapter<SensorHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView = view.findViewById(R.id.tvHistoryTime)
        val tvValue: TextView = view.findViewById(R.id.tvHistoryValue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyList[position]
        holder.tvTime.text = item.timeFormatted
        holder.tvValue.text = item.valueFormatted
    }

    override fun getItemCount(): Int = historyList.size
}