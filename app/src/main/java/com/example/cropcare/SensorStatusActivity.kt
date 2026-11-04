package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SensorStatusActivity : AppCompatActivity() {
    private val items = mutableListOf<SensorModel>()
    private lateinit var adapter: SensorAdapter
    private var zoneId = ""
    private var request: MonitoringRequest? = null
    private var removal: ApiRequestHandle? = null
    private var removing = false
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { if (!removing) load(); handler.postDelayed(this, 60_000) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_sensor_status)
        zoneId = intent.getStringExtra("ZONE_ID").orEmpty()
        adapter = SensorAdapter(items, { sensor -> confirmRemove(sensor) }, { sensor ->
            startActivity(Intent(this, SensorDetailActivity::class.java).putExtra("SENSOR_ID", sensor.sensorId)
                .putExtra("DEVICE_ID", sensor.deviceId).putExtra("SENSOR_NAME", sensor.sensorName))
        })
        findViewById<RecyclerView>(R.id.rvSensors).apply {
            layoutManager = LinearLayoutManager(this@SensorStatusActivity); adapter = this@SensorStatusActivity.adapter
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddSensor).setOnClickListener {
            startActivity(Intent(this, AddSensorActivity::class.java).putExtra("ZONE_ID", zoneId))
        }
        findViewById<Button>(R.id.btnDeleteAll).apply { isEnabled = false; text = "Bulk removal unavailable" }
        findViewById<View>(R.id.tvSensorListState).setOnClickListener { if (!removing) load() }
        if (zoneId.isBlank()) { Toast.makeText(this, "Missing farm zone.", Toast.LENGTH_LONG).show(); finish() }
    }
    private fun confirmRemove(sensor: SensorModel) {
        if (removing) return
        AlertDialog.Builder(this).setTitle("Unpair ${sensor.sensorName}?")
            .setMessage("The API will release any authorized QR claim. Historical soil readings are retained.")
            .setNegativeButton("Cancel", null).setPositiveButton("Unpair") { _, _ ->
                if (removing) return@setPositiveButton
                removing = true; request?.cancel()
                removal = CropCareHttpClient.request("DELETE", "/api/sensors/${android.net.Uri.encode(sensor.sensorId)}") { result ->
                    removing = false
                    Toast.makeText(this, if (result.successful) {
                        if (result.data?.optBoolean("qrReusable") == true) "Sensor unpaired. Its original QR can be reused."
                        else "Registration removed. No linked QR claim was changed."
                    } else result.message, Toast.LENGTH_LONG).show()
                    load()
                }
            }.show()
    }
    private fun load() {
        request?.cancel()
        val state = findViewById<TextView>(R.id.tvSensorListState)
        state.text = "Loading sensors…"
        request = ZoneMonitoringRepository.sensors(zoneId) { snapshots, error ->
            if (snapshots == null) state.text = (error ?: "Sensors unavailable") + " · tap to retry"
            else {
                items.clear(); items.addAll(snapshots.map { it.sensor }); adapter.notifyDataSetChanged()
                state.text = if (items.isEmpty()) "No sensors registered. Use + to pair a sensor." else "${items.size} sensors · tap to refresh"
            }
        }
    }
    override fun onStart() { super.onStart(); handler.post(refresh) }
    override fun onStop() { handler.removeCallbacks(refresh); request?.cancel(); removal?.cancel(); removing = false; super.onStop() }
}
