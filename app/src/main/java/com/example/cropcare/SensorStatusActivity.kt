package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SensorStatusActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SensorAdapter
    private val sensorList = mutableListOf<SensorModel>()
    private var zoneId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_status)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

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
        fetchSensors()
    }

    private fun fetchSensors() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { docs ->
                sensorList.clear()
                for (doc in docs) {
                    val sensor = SensorModel(
                        sensorId = doc.id,
                        deviceId = doc.getString("deviceId") ?: "",
                        sensorName = doc.getString("sensorName") ?: "",
                        zoneId = doc.getString("zoneId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        lastScanTimestamp = doc.getLong("lastScanTimestamp") ?: System.currentTimeMillis(),
                        isOnline = doc.getBoolean("isOnline") ?: true
                    )
                    sensorList.add(sensor)
                }
                adapter.notifyDataSetChanged()
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