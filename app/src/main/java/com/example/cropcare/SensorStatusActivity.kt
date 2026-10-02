package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Date

class SensorStatusActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SensorAdapter
    private val sensorList = mutableListOf<SensorModel>()
    private var zoneId: String = ""
    private var zoneName: String = "Zone"

    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchSensors()
            handler.postDelayed(this, 60000L) // Re-check every 1 minute
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_status)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        if (zoneId.isNotEmpty()) {
            db.collection("zones").document(zoneId).get().addOnSuccessListener { doc ->
                zoneName = doc.getString("zoneName") ?: "Zone"
            }
        }

        val rvSensors = findViewById<RecyclerView>(R.id.rvSensors)
        val btnAddSensor = findViewById<ImageButton>(R.id.btnAddSensor)
        val btnDeleteAll = findViewById<Button>(R.id.btnDeleteAll)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        rvSensors.layoutManager = LinearLayoutManager(this)
        adapter = SensorAdapter(
            sensorList,
            onDeleteClick = { sensor -> deleteSensor(sensor) },
            onViewDetailsClick = { sensor ->
                val intent = Intent(this, SensorDetailActivity::class.java)
                intent.putExtra("SENSOR_ID", sensor.sensorId)
                intent.putExtra("DEVICE_ID", sensor.deviceId)
                intent.putExtra("SENSOR_NAME", sensor.sensorName)
                startActivity(intent)
            }
        )
        rvSensors.adapter = adapter

        btnAddSensor.setOnClickListener {
            val intent = Intent(this, AddSensorActivity::class.java)
            intent.putExtra("ZONE_ID", zoneId)
            startActivity(intent)
        }

        btnDeleteAll.setOnClickListener {
            deleteAllSensors()
        }

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun fetchSensors() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    sensorList.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                val tempSensors = mutableListOf<SensorModel>()
                val tasks = docs.map { doc ->
                    val sensorId = doc.id
                    val deviceId = doc.getString("deviceId") ?: ""
                    val sensorName = doc.getString("sensorName") ?: ""
                    val zId = doc.getString("zoneId") ?: ""
                    val uId = doc.getString("userId") ?: ""
                    val fallbackTimestamp = doc.getLong("lastScanTimestamp") ?: System.currentTimeMillis()

                    // Check latest soil_data timestamp for this deviceId
                    db.collection("soil_data")
                        .whereEqualTo("deviceId", deviceId)
                        .get()
                        .addOnSuccessListener { soilDocs ->
                            val latestDoc = soilDocs.documents.maxByOrNull { parseAnyDate(it)?.time ?: 0L }
                            val latestTime = if (latestDoc != null) {
                                parseAnyDate(latestDoc)?.time ?: fallbackTimestamp
                            } else fallbackTimestamp

                            // Sensor is ONLINE if latest reading occurred within the last 2 minutes (120,000 ms)
                            val now = System.currentTimeMillis()
                            val isOnline = (now - latestTime) <= 120000L

                            if (!isOnline) {
                                NotificationHelper.checkAndTriggerOfflineSensorAlert(
                                    this@SensorStatusActivity,
                                    zoneName,
                                    sensorName
                                )
                            }

                            tempSensors.add(
                                SensorModel(
                                    sensorId = sensorId,
                                    deviceId = deviceId,
                                    sensorName = sensorName,
                                    zoneId = zId,
                                    userId = uId,
                                    lastScanTimestamp = latestTime,
                                    isOnline = isOnline
                                )
                            )
                        }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    sensorList.clear()
                    sensorList.addAll(tempSensors)
                    adapter.notifyDataSetChanged()
                }
            }
    }

    private fun parseAnyDate(doc: DocumentSnapshot): Date? {
        val rawValue = doc.get("timestamp") ?: doc.get("lastScanTimestamp")
        return when (rawValue) {
            is Timestamp -> rawValue.toDate()
            is Long -> Date(rawValue)
            is Double -> Date(rawValue.toLong())
            else -> null
        }
    }

    private fun deleteSensor(sensor: SensorModel) {
        db.collection("sensors").document(sensor.sensorId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Sensor deleted", Toast.LENGTH_SHORT).show()
                fetchSensors()
            }
    }

    private fun deleteAllSensors() {
        val batch = db.batch()
        for (sensor in sensorList) {
            val ref = db.collection("sensors").document(sensor.sensorId)
            batch.delete(ref)
        }
        batch.commit().addOnSuccessListener {
            Toast.makeText(this, "All sensors deleted", Toast.LENGTH_SHORT).show()
            fetchSensors()
        }
    }
}
