
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class SensorStatusActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val items = mutableListOf<SensorModel>()

    private lateinit var adapter: SensorAdapter

    private val handler = Handler(Looper.getMainLooper())

    private var generation = 0
    private var zoneId = ""

    private val refresher = object : Runnable {
        override fun run() {
            loadSensors()
            handler.postDelayed(this, 60_000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_status)

        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        val list =
            findViewById<RecyclerView>(R.id.rvSensors)

        list.layoutManager =
            LinearLayoutManager(this)

        adapter = SensorAdapter(
            items,

            onDeleteClick = { sensor ->

                if (
                    sensor.pairingStatus == "qr_claimed" ||
                    sensor.pairingStatus == "verified"
                ) {
                    show(
                        "Unpairing QR sensors is on hold. " +
                                "Do not delete their registration."
                    )
                } else {
                    CropCareApiClient.deleteSensor(
                        sensor.sensorId
                    ) { result ->

                        if (result.successful) {
                            loadSensors()
                        } else {
                            show(result.message)
                        }
                    }
                }
            },

            onViewDetailsClick = { sensor ->

                startActivity(
                    Intent(
                        this,
                        SensorDetailActivity::class.java
                    ).apply {
                        putExtra("SENSOR_ID", sensor.sensorId)
                        putExtra("DEVICE_ID", sensor.deviceId)
                        putExtra("SENSOR_NAME", sensor.sensorName)
                    }
                )
            }
        )

        list.adapter = adapter

        findViewById<ImageButton>(
            R.id.btnAddSensor
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    AddSensorActivity::class.java
                ).putExtra("ZONE_ID", zoneId)
            )
        }

        findViewById<ImageButton>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        // Deleting all sensors could orphan
        // QR claims in device_registry.
        // Leave it disabled until unpair is ready.
        findViewById<Button>(
            R.id.btnDeleteAll
        ).apply {
            isEnabled = false
            text = "Unpair feature pending"
        }
    }

    override fun onResume() {
        super.onResume()

        handler.removeCallbacks(refresher)
        handler.post(refresher)
    }

    override fun onPause() {
        super.onPause()

        generation++
        handler.removeCallbacks(refresher)
    }

    private fun loadSensors() {
        val uid = auth.currentUser?.uid ?: return

        if (zoneId.isBlank()) {
            return
        }

        val myGeneration = ++generation

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", uid)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshot ->

                if (
                    myGeneration != generation ||
                    isFinishing ||
                    isDestroyed
                ) {
                    return@addOnSuccessListener
                }

                val docs = snapshot.documents

                if (docs.isEmpty()) {
                    items.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                val output =
                    arrayOfNulls<SensorModel>(docs.size)

                var pending = docs.size

                docs.forEachIndexed { index, doc ->

                    val id = doc.id

                    val deviceId =
                        doc.getString("deviceId") ?: ""

                    val name =
                        doc.getString("sensorName") ?: "Sensor"

                    val pairing =
                        doc.getString("pairingStatus") ?: ""

                    SensorMonitoringApi.get(
                        id
                    ) { result, error ->

                        if (
                            myGeneration != generation ||
                            isFinishing ||
                            isDestroyed
                        ) {
                            return@get
                        }

                        val status =
                            result?.optString("status")
                                ?: "unavailable"

                        val lastReadingMs =
                            result?.optLong(
                                "lastReadingAtMs",
                                0L
                            ) ?: 0L

                        output[index] = SensorModel(
                            sensorId = id,
                            deviceId = deviceId,
                            sensorName = name,
                            zoneId = zoneId,
                            userId = uid,
                            lastScanTimestamp = lastReadingMs,
                            isOnline = status == "recent",
                            pairingStatus = pairing,
                            readingStatusUnavailable =
                                error != null ||
                                        status == "unavailable"
                        )

                        pending--

                        if (pending == 0) {
                            items.clear()
                            items.addAll(
                                output.filterNotNull()
                            )

                            adapter.notifyDataSetChanged()
                        }
                    }
                }
            }
            .addOnFailureListener { error ->
                show(
                    "Could not load sensors: ${error.message}"
                )
            }
    }

    private fun show(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
