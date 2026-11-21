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

class DeviceManagementActivity : AppCompatActivity() {
    private val items = mutableListOf<SensorModel>()
    private lateinit var adapter: SensorAdapter
    private var request: ApiRequestHandle? = null
    private var removal: ApiRequestHandle? = null
    private var removing = false
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() { if (!removing) load(); handler.postDelayed(this, 60_000) }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_device_management)
        adapter = SensorAdapter(items, ::manageSensor)
        findViewById<RecyclerView>(R.id.rvSensors).apply {
            layoutManager = LinearLayoutManager(this@DeviceManagementActivity); adapter = this@DeviceManagementActivity.adapter
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnAddSensor).setOnClickListener {
            if (!removing) chooseZone("Choose a zone for the new sensor") { zoneId ->
                startActivity(Intent(this, AddSensorActivity::class.java).putExtra("ZONE_ID", zoneId))
            }
        }
        findViewById<Button>(R.id.btnDeleteAll).apply { text = "Delete All"; setOnClickListener { confirmRemoveAll() } }
        findViewById<View>(R.id.tvSensorListState).setOnClickListener { if (!removing) load() }
    }
    private var assignment: ApiRequestHandle? = null

    private fun manageSensor(sensor: SensorModel) {
        if (removing) return
        AlertDialog.Builder(this).setTitle(sensor.sensorName)
            .setItems(arrayOf("Rename sensor", "Change assigned zone", "Unpair sensor")) { _, position ->
                when (position) {
                    0 -> startActivity(Intent(this, EditSensorActivity::class.java).putExtra("SENSOR_ID", sensor.sensorId))
                    1 -> chooseZone("Assign to a zone", sensor.zoneId) { assignZone(sensor, it) }
                    2 -> confirmRemove(sensor)
                }
            }.show()
    }

    private fun chooseZone(title: String, currentZone: String = "", selected: (String) -> Unit) {
        assignment?.cancel()
        assignment = CropCareHttpClient.request("GET", "/api/zones") { result ->
            val zones = result.array
            if (!result.successful || zones == null) {
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                return@request
            }
            val choices = (0 until zones.length()).mapNotNull { zones.optJSONObject(it) }
            if (choices.isEmpty()) {
                Toast.makeText(this, "Create a farm zone first.", Toast.LENGTH_LONG).show()
                return@request
            }
            AlertDialog.Builder(this).setTitle(title)
                .setSingleChoiceItems(choices.map { it.optString("zoneName") }.toTypedArray(),
                    choices.indexOfFirst { it.optString("zoneId") == currentZone }) { dialog, position ->
                    dialog.dismiss()
                    val destination = choices[position].optString("zoneId")
                    if (destination.isNotBlank() && destination != currentZone) selected(destination)
                }
                .setNegativeButton("Cancel", null).show()
        }
    }

    private fun assignZone(sensor: SensorModel, zoneId: String) {
        val path = "/api/sensors/${android.net.Uri.encode(sensor.sensorId)}/zone"
        assignment = CropCareHttpClient.request("PUT", path, org.json.JSONObject().put("zoneId", zoneId)) { updated ->
            Toast.makeText(this, updated.message, Toast.LENGTH_LONG).show()
            load()
        }
    }

    private fun confirmRemoveAll() {
        if (removing || items.isEmpty()) return
        val ids = items.map { it.sensorId }.distinct()
        AlertDialog.Builder(this).setTitle("Delete All sensors?")
            .setMessage("Unpair all ${ids.size} sensors registered to your account? Their QR codes can be paired again. Historical readings are retained.")
            .setNegativeButton("Cancel", null).setPositiveButton("Delete All") { _, _ ->
                removing = true; request?.cancel()
                fun next(index: Int) {
                    if (isFinishing || isDestroyed || !removing) return
                    if (index == ids.size) { removing = false; Toast.makeText(this, "All sensors unpaired", Toast.LENGTH_LONG).show(); load(); return }
                    findViewById<Button>(R.id.btnDeleteAll).text = "Unpairing ${index + 1}/${ids.size}…"
                    removal = CropCareHttpClient.request("DELETE", "/api/sensors/${android.net.Uri.encode(ids[index])}") { result ->
                        if (result.successful) next(index + 1)
                        else { removing = false; Toast.makeText(this, "Unpaired $index sensors. ${result.message}", Toast.LENGTH_LONG).show(); load() }
                    }
                }
                next(0)
            }.show()
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
        findViewById<Button>(R.id.btnDeleteAll).text = "Delete All"
        state.text = "Loading sensors…"
        request = ZoneMonitoringRepository.listSensors(null) { sensors, error ->
            if (sensors == null) state.text = (error ?: "Sensors unavailable") + " · tap to retry"
            else {
                items.clear(); items.addAll(sensors); adapter.notifyDataSetChanged()
                state.text = if (items.isEmpty()) "No sensors registered. Use + to pair a sensor." else "${items.size} sensors · tap to refresh"
            }
        }
    }
    override fun onStart() { super.onStart(); handler.post(refresh) }
    override fun onStop() { handler.removeCallbacks(refresh); request?.cancel(); removal?.cancel(); assignment?.cancel(); removing = false; super.onStop() }
}
