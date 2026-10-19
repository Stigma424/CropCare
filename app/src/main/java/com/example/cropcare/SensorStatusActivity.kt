
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
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import java.util.Date

class SensorStatusActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val sensorList = mutableListOf<SensorModel>()
    private lateinit var adapter: SensorAdapter

    private var zoneId = ""
    private var zoneName = "Zone"

    private val handler = Handler(Looper.getMainLooper())
    private var requestGeneration = 0

    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchSensors()
            handler.postDelayed(this, 60000L)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_status)

        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        val rvSensors =
            findViewById<RecyclerView>(R.id.rvSensors)

        val btnAddSensor =
            findViewById<ImageButton>(R.id.btnAddSensor)

        val btnDeleteAll =
            findViewById<Button>(R.id.btnDeleteAll)

        val btnBack =
            findViewById<ImageButton>(R.id.btnBack)

        rvSensors.layoutManager =
            LinearLayoutManager(this)

        adapter = SensorAdapter(
            sensorList,
            onDeleteClick = { sensor ->
                deleteSensor(sensor)
            },
            onViewDetailsClick = { sensor ->
                val intent = Intent(
                    this,
                    SensorDetailActivity::class.java
                )

                intent.putExtra("SENSOR_ID", sensor.sensorId)
                intent.putExtra("DEVICE_ID", sensor.deviceId)
                intent.putExtra("SENSOR_NAME", sensor.sensorName)

                startActivity(intent)
            }
        )

        rvSensors.adapter = adapter

        btnAddSensor.setOnClickListener {
            startActivity(
                Intent(this, AddSensorActivity::class.java)
                    .putExtra("ZONE_ID", zoneId)
            )
        }

        btnDeleteAll.setOnClickListener {
            deleteAllSensors()
        }

        btnBack.setOnClickListener {
            finish()
        }

        if (zoneId.isBlank()) {
            showMessage("Missing zone ID.")
            finish()
            return
        }

        db.collection("zones")
            .document(zoneId)
            .get()
            .addOnSuccessListener { document ->
                zoneName =
                    document.getString("zoneName") ?: "Zone"
            }
    }

    override fun onResume() {
        super.onResume()

        handler.removeCallbacks(refreshRunnable)
        handler.post(refreshRunnable)
    }

    override fun onPause() {
        super.onPause()

        handler.removeCallbacks(refreshRunnable)

        // Ignore results from an earlier screen visit.
        requestGeneration++
    }

    private fun fetchSensors() {
        val userId = auth.currentUser?.uid ?: return

        if (zoneId.isBlank()) {
            return
        }

        val generation = ++requestGeneration

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get(Source.SERVER)
            .addOnSuccessListener { docs ->

                if (!isCurrentRequest(generation)) {
                    return@addOnSuccessListener
                }

                if (docs.isEmpty) {
                    sensorList.clear()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                val documents = docs.documents

                val results =
                    arrayOfNulls<SensorModel>(documents.size)

                var remaining = documents.size

                fun complete(
                    index: Int,
                    sensor: SensorModel
                ) {
                    if (!isCurrentRequest(generation)) {
                        return
                    }

                    results[index] = sensor
                    remaining--

                    if (remaining == 0) {
                        sensorList.clear()
                        sensorList.addAll(
                            results.filterNotNull()
                        )

                        adapter.notifyDataSetChanged()
                    }
                }

                documents.forEachIndexed { index, doc ->

                    val sensorId = doc.id
                    val deviceId =
                        doc.getString("deviceId") ?: ""

                    val sensorName =
                        doc.getString("sensorName") ?: ""

                    val pairingStatus =
                        doc.getString("pairingStatus") ?: ""

                    fun finishWithReading(
                        latestDate: Date?,
                        queryFailed: Boolean = false
                    ) {
                        val lastReadingTime =
                            latestDate?.time ?: 0L

                        val now = System.currentTimeMillis()

                        val ageMillis = now - lastReadingTime

                        val isOnline =
                            lastReadingTime > 0L &&
                                    ageMillis in 0L..120000L

                        // Newly registered sensors have no data.
                        // That is NOT the same as going offline.
                        //
                        // Do not generate offline warnings for
                        // unverified sensors or query failures.
                        if (
                            lastReadingTime > 0L &&
                            !isOnline &&
                            !queryFailed &&
                            pairingStatus != "unverified"
                        ) {
                            NotificationHelper
                                .checkAndTriggerOfflineSensorAlert(
                                    this@SensorStatusActivity,
                                    zoneName,
                                    sensorName
                                )
                        }

                        complete(
                            index,
                            SensorModel(
                                sensorId = sensorId,
                                deviceId = deviceId,
                                sensorName = sensorName,
                                zoneId = zoneId,
                                userId = userId,
                                lastScanTimestamp =
                                    lastReadingTime,
                                isOnline = isOnline,
                                pairingStatus = pairingStatus,
                                readingStatusUnavailable =
                                    queryFailed
                            )
                        )
                    }

                    if (deviceId.isBlank()) {
                        finishWithReading(null)
                        return@forEachIndexed
                    }

                    db.collection("soil_data")
                        .whereEqualTo("deviceId", deviceId)
                        .get(Source.SERVER)
                        .addOnSuccessListener { readings ->

                            if (!isCurrentRequest(generation)) {
                                return@addOnSuccessListener
                            }

                            val latestDate =
                                readings.documents
                                    .mapNotNull {
                                        parseReadingDate(it)
                                    }
                                    .maxByOrNull { it.time }

                            finishWithReading(latestDate)
                        }
                        .addOnFailureListener {

                            if (!isCurrentRequest(generation)) {
                                return@addOnFailureListener
                            }

                            // A query failure is not proof
                            // that the device is offline.
                            finishWithReading(
                                latestDate = null,
                                queryFailed = true
                            )
                        }
                }
            }
            .addOnFailureListener { error ->

                if (!isCurrentRequest(generation)) {
                    return@addOnFailureListener
                }

                showMessage(
                    "Could not load sensors: ${error.message}"
                )
            }
    }

    private fun isCurrentRequest(generation: Int): Boolean {
        return generation == requestGeneration &&
                !isFinishing &&
                !isDestroyed
    }

    private fun parseReadingDate(
        doc: DocumentSnapshot
    ): Date? {
        val raw = doc.get("timestamp")
            ?: doc.get("lastScanTimestamp")

        return when (raw) {
            is Timestamp -> raw.toDate()
            is Long -> Date(raw)
            is Int -> Date(raw.toLong())
            is Double -> Date(raw.toLong())
            else -> null
        }
    }

    private fun deleteSensor(sensor: SensorModel) {
        // Use the API endpoint implemented in the
        // previous batch.
        CropCareApiClient.deleteSensor(
            sensor.sensorId
        ) { result ->

            if (result.successful) {
                showMessage("Sensor registration deleted.")
                fetchSensors()

            } else {
                showMessage(
                    "Delete failed: ${result.message}"
                )
            }
        }
    }

    private fun deleteAllSensors() {
        if (sensorList.isEmpty()) {
            showMessage("No sensors to delete.")
            return
        }

        // Keep batch deletion through the authenticated
        // Firestore client for now, using owner-only rules.
        // A dedicated bulk-delete API will replace this.
        val batch = db.batch()

        sensorList.forEach { sensor ->
            val reference = db.collection("sensors")
                .document(sensor.sensorId)

            batch.delete(reference)
        }

        batch.commit()
            .addOnSuccessListener {
                showMessage("All sensors deleted.")
                fetchSensors()
            }
            .addOnFailureListener { error ->
                showMessage(
                    "Delete failed: ${error.message}"
                )
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
