package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SensorStatusActivity : AppCompatActivity() {
    private val items = mutableListOf<SensorModel>()
    private lateinit var adapter: SensorAdapter
    private var zoneId = ""
    private var request: MonitoringRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { load(); handler.postDelayed(this, 60_000) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_sensor_status)
        zoneId = intent.getStringExtra("ZONE_ID").orEmpty()
        adapter = SensorAdapter(items, { sensor ->
            startActivity(Intent(this, SensorDetailActivity::class.java).putExtra("SENSOR_ID", sensor.sensorId)
                .putExtra("ZONE_ID", zoneId).putExtra("DEVICE_ID", sensor.deviceId).putExtra("SENSOR_NAME", sensor.sensorName))
        }, { sensor ->
            startActivity(Intent(this, SensorHistoryActivity::class.java).putExtra("SENSOR_ID", sensor.sensorId)
                .putExtra("SENSOR_NAME", sensor.sensorName))
        })
        findViewById<RecyclerView>(R.id.rvSensors).apply {
            layoutManager = LinearLayoutManager(this@SensorStatusActivity); adapter = this@SensorStatusActivity.adapter
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.tvSensorListState).setOnClickListener { load() }
        if (zoneId.isBlank()) { Toast.makeText(this, "Missing farm zone.", Toast.LENGTH_LONG).show(); finish() }
    }
    private fun load() {
        request?.cancel()
        val state = findViewById<TextView>(R.id.tvSensorListState)
        state.text = "Loading sensors…"
        request = ZoneMonitoringRepository.sensors(zoneId) { snapshots, error ->
            if (snapshots == null) state.text = (error ?: "Sensors unavailable") + " · tap to retry"
            else {
                items.clear(); items.addAll(snapshots.map { it.sensor }); adapter.notifyDataSetChanged()
                state.text = if (items.isEmpty()) "No sensors registered." else "${items.size} sensors · tap to refresh"
            }
        }
    }
    override fun onStart() { super.onStart(); handler.post(refresh) }
    override fun onStop() { handler.removeCallbacks(refresh); request?.cancel(); super.onStop() }
}
