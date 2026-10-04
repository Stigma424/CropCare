package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvLastUpdated: TextView

    private var currentSensorId: String = ""
    private var currentDeviceId: String = ""
    private var currentSensorName: String = "Sensor"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)

        db = FirebaseFirestore.getInstance()

        currentSensorId = intent.getStringExtra("SENSOR_ID") ?: ""
        currentDeviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        currentSensorName = intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

        val tvTitle = findViewById<TextView>(R.id.tvSensorDetailTitle)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)

        val btnDeleteSensor = findViewById<TextView>(R.id.btnDeleteSensor)
        val btnEditSensor = findViewById<TextView>(R.id.btnEditSensor)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnRefresh = findViewById<View>(R.id.btnRefreshDetail)

        tvTitle.text = currentSensorName

        loadLatestData(currentDeviceId)

        btnRefresh.setOnClickListener {
            loadSensorInfoAndData()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnViewHistoryN).setOnClickListener { openHistory(currentDeviceId, "nitrogen", "Nitrogen", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryP).setOnClickListener { openHistory(currentDeviceId, "phosphorus", "Phosphorus", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryK).setOnClickListener { openHistory(currentDeviceId, "potassium", "Potassium", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryMoisture).setOnClickListener { openHistory(currentDeviceId, "moisture", "Moisture", "%") }
        findViewById<TextView>(R.id.btnViewHistoryPh).setOnClickListener { openHistory(currentDeviceId, "ph", "pH Level", "") }
        findViewById<TextView>(R.id.btnViewHistoryTemp).setOnClickListener { openHistory(currentDeviceId, "temperature", "Soil Temperature", "°C") }
        findViewById<TextView>(R.id.btnViewHistoryEc).setOnClickListener { openHistory(currentDeviceId, "ec", "Electrical Conductivity", "") }

        btnEditSensor.setOnClickListener {
            val intent = Intent(this, EditSensorActivity::class.java)
            intent.putExtra("SENSOR_ID", currentSensorId)
            intent.putExtra("DEVICE_ID", currentDeviceId)
            intent.putExtra("SENSOR_NAME", currentSensorName)
            startActivity(intent)
        }

        btnDeleteSensor.setOnClickListener {
            if (currentSensorId.isNotEmpty()) {
                db.collection("sensors").document(currentSensorId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sensor deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            } else {
                finish()
            }
        }

        btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        loadSensorInfoAndData()
    }

    private fun loadSensorInfoAndData() {
        if (currentSensorId.isNotEmpty()) {
            db.collection("sensors").document(currentSensorId).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        currentDeviceId = doc.getString("deviceId") ?: currentDeviceId
                        currentSensorName = doc.getString("sensorName") ?: currentSensorName
                        findViewById<TextView>(R.id.tvSensorDetailTitle).text = currentSensorName
                    }
                    loadLatestData(currentDeviceId)
                }
                .addOnFailureListener {
                    loadLatestData(currentDeviceId)
                }
        } else {
            loadLatestData(currentDeviceId)
        }
    }

    private fun loadLatestData(deviceId: String) {
        val trimmedId = deviceId.trim()
        if (trimmedId.isEmpty()) {
            resetUiToEmpty("Sensor ID is empty or not found")
            return
        }

        db.collection("soil_data")
            .whereEqualTo("deviceId", trimmedId)
            .get()
            .addOnSuccessListener { docs ->
                if (docs.isEmpty) {
                    resetUiToEmpty("No readings found for Sensor ID: $trimmedId")
                    return@addOnSuccessListener
                }

                val latestDoc = docs.documents.maxByOrNull { parseAnyDate(it)?.time ?: 0L }

                if (latestDoc == null) {
                    resetUiToEmpty("No valid readings for Sensor ID: $trimmedId")
                    return@addOnSuccessListener
                }

                val nVal = latestDoc.getDouble("nitrogen") ?: 0.0
                val pVal = latestDoc.getDouble("phosphorus") ?: 0.0
                val kVal = latestDoc.getDouble("potassium") ?: 0.0
                val mVal = latestDoc.getDouble("moisture") ?: 0.0
                val phVal = latestDoc.getDouble("ph") ?: 0.0
                val tVal = latestDoc.getDouble("temperature") ?: 0.0
                val ecVal = latestDoc.getDouble("ec") ?: 0.0

                findViewById<TextView>(R.id.tvSensorN).text = String.format(Locale.US, "%.0f mg/kg", nVal)
                findViewById<TextView>(R.id.tvStatusN).text = SoilUtils.getStatus("N", nVal)

                findViewById<TextView>(R.id.tvSensorP).text = String.format(Locale.US, "%.0f mg/kg", pVal)
                findViewById<TextView>(R.id.tvStatusP).text = SoilUtils.getStatus("P", pVal)

                findViewById<TextView>(R.id.tvSensorK).text = String.format(Locale.US, "%.0f mg/kg", kVal)
                findViewById<TextView>(R.id.tvStatusK).text = SoilUtils.getStatus("K", kVal)

                findViewById<TextView>(R.id.tvSensorMoisture).text = String.format(Locale.US, "%.0f%%", mVal)
                findViewById<TextView>(R.id.tvStatusMoisture).text = SoilUtils.getStatus("MOISTURE", mVal)

                findViewById<TextView>(R.id.tvSensorPh).text = String.format(Locale.US, "%.1f", phVal)
                findViewById<TextView>(R.id.tvStatusPh).text = SoilUtils.getStatus("PH", phVal)

                findViewById<TextView>(R.id.tvSensorTemp).text = String.format(Locale.US, "%.0f°C", tVal)
                findViewById<TextView>(R.id.tvStatusTemp).text = SoilUtils.getStatus("TEMP", tVal)

                findViewById<TextView>(R.id.tvSensorEc).text = String.format(Locale.US, "%.0f", ecVal)
                findViewById<TextView>(R.id.tvStatusEc).text = SoilUtils.getStatus("EC", ecVal)

                val timeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm:ss a", Locale.US)
                val dateObj = parseAnyDate(latestDoc) ?: Date()
                tvLastUpdated.text = "Last updated: ${timeFormat.format(dateObj)}"

                // Check and trigger notifications if sensor values are low/high
                NotificationHelper.checkAndTriggerSoilAlerts(
                    context = this,
                    zoneName = "Sensor $trimmedId",
                    n = nVal,
                    p = pVal,
                    k = kVal,
                    moisture = mVal,
                    ph = phVal,
                    recommendationSummary = ""
                )
            }
            .addOnFailureListener { e ->
                resetUiToEmpty("Failed to load data: ${e.message}")
            }
    }

    private fun resetUiToEmpty(message: String) {
        findViewById<TextView>(R.id.tvSensorN).text = "0 mg/kg"
        findViewById<TextView>(R.id.tvStatusN).text = "No Data"

        findViewById<TextView>(R.id.tvSensorP).text = "0 mg/kg"
        findViewById<TextView>(R.id.tvStatusP).text = "No Data"

        findViewById<TextView>(R.id.tvSensorK).text = "0 mg/kg"
        findViewById<TextView>(R.id.tvStatusK).text = "No Data"

        findViewById<TextView>(R.id.tvSensorMoisture).text = "0%"
        findViewById<TextView>(R.id.tvStatusMoisture).text = "No Data"

        findViewById<TextView>(R.id.tvSensorPh).text = "0.0"
        findViewById<TextView>(R.id.tvStatusPh).text = "No Data"

        findViewById<TextView>(R.id.tvSensorTemp).text = "0°C"
        findViewById<TextView>(R.id.tvStatusTemp).text = "No Data"

        findViewById<TextView>(R.id.tvSensorEc).text = "0"
        findViewById<TextView>(R.id.tvStatusEc).text = "No Data"

        tvLastUpdated.text = message
    }

    private fun parseAnyDate(doc: DocumentSnapshot): Date? {
        val rawValue = doc.get("timestamp") ?: doc.get("lastScanTimestamp")

        return when (rawValue) {
            is Timestamp -> rawValue.toDate()
            is Long -> Date(rawValue)
            is Double -> Date(rawValue.toLong())
            is String -> {
                val formats = arrayOf(
                    "yyyy-MM-dd HH:mm:ss",
                    "MMMM dd, yyyy 'at' h:mm:ss a z",
                    "MMMM dd, yyyy - hh:mm:ss a"
                )
                formats.firstNotNullOfOrNull { fmt ->
                    try { SimpleDateFormat(fmt, Locale.US).parse(rawValue) } catch (_: Exception) { null }
                }
            }
            else -> null
        }
    }

    private fun openHistory(deviceId: String, metricKey: String, metricTitle: String, unit: String) {
        if (deviceId.trim().isEmpty()) {
            Toast.makeText(this, "Cannot view history for empty Sensor ID", Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, SensorHistoryActivity::class.java)
        intent.putExtra("DEVICE_ID", deviceId)
        intent.putExtra("METRIC_KEY", metricKey)
        intent.putExtra("METRIC_TITLE", metricTitle)
        intent.putExtra("UNIT", unit)
        startActivity(intent)
    }
}
