package com.example.cropcare

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ZoneAdapter(
    private val zones: List<ZoneModel>,
    private val onSeeMoreClick: (String) -> Unit
) : RecyclerView.Adapter<ZoneAdapter.ZoneViewHolder>() {

    class ZoneViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tvItemZoneName)
        val tvArea: TextView = itemView.findViewById(R.id.tvItemZoneArea)
        val btnSeeMore: Button = itemView.findViewById(R.id.btnSeeMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ZoneViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_zone, parent, false)
        return ZoneViewHolder(view)
    }

    override fun onBindViewHolder(holder: ZoneViewHolder, position: Int) {
        val zone = zones[position]
        holder.tvName.text = zone.zoneName
        holder.tvArea.text = "Area: ${zone.zoneAreaSqm} sq.m"
        holder.btnSeeMore.setOnClickListener {
            onSeeMoreClick(zone.zoneId)
        }
    }

    override fun getItemCount(): Int = zones.size
}