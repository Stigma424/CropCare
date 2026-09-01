package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)

        db = FirebaseFirestore.getInstance()

        val sensorId = intent.getStringExtra("SENSOR_ID") ?: ""
        val deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        val sensorName = intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

        val tvTitle = findViewById<TextView>(R.id.tvSensorDetailTitle)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)

        val btnDeleteSensor = findViewById<Button>(R.id.btnDeleteSensor)
        val btnEditSensor = findViewById<Button>(R.id.btnEditSensor)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnRefresh = findViewById<ImageButton>(R.id.btnRefreshDetail)

        tvTitle.text = sensorName

        loadLatestData(deviceId)

        btnRefresh.setOnClickListener {
            loadLatestData(deviceId)
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        findViewById<TextView>(R.id.btnViewHistoryN).setOnClickListener { openHistory(deviceId, "nitrogen", "Nitrogen", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryP).setOnClickListener { openHistory(deviceId, "phosphorus", "Phosphorus", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryK).setOnClickListener { openHistory(deviceId, "potassium", "Potassium", "mg/kg") }
        findViewById<TextView>(R.id.btnViewHistoryMoisture).setOnClickListener { openHistory(deviceId, "moisture", "Moisture", "%") }
        findViewById<TextView>(R.id.btnViewHistoryPh).setOnClickListener { openHistory(deviceId, "ph", "pH Level", "") }
        findViewById<TextView>(R.id.btnViewHistoryTemp).setOnClickListener { openHistory(deviceId, "temperature", "Soil Temperature", "°C") }
        findViewById<TextView>(R.id.btnViewHistoryEc).setOnClickListener { openHistory(deviceId, "ec", "Electrical Conductivity", "") }

        btnEditSensor.setOnClickListener {
            val intent = Intent(this, EditSensorActivity::class.java)
            intent.putExtra("SENSOR_ID", sensorId)
            intent.putExtra("DEVICE_ID", deviceId)
            intent.putExtra("SENSOR_NAME", sensorName)
            startActivity(intent)
        }

        btnDeleteSensor.setOnClickListener {
            db.collection("sensors").document(sensorId).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Sensor deleted", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadLatestData(deviceId: String) {
        if (deviceId.isEmpty()) return

        db.collection("soil_data")
            .whereEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val latestDoc = docs.documents.maxByOrNull { parseAnyDate(it)?.time ?: 0L }

                    if (latestDoc != null) {
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
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
        val intent = Intent(this, SensorHistoryActivity::class.java)
        intent.putExtra("DEVICE_ID", deviceId)
        intent.putExtra("METRIC_KEY", metricKey)
        intent.putExtra("METRIC_TITLE", metricTitle)
        intent.putExtra("UNIT", unit)
        startActivity(intent)
    }
}