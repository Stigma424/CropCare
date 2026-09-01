package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ZoneManagementActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var currentZoneId: String = ""

    private lateinit var tvManageZoneName: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvCornAge: TextView
    private lateinit var btnNewCornPlanted: Button
    private lateinit var tvSoilHealth: TextView

    // NPK
    private lateinit var tvAvgN: TextView
    private lateinit var tvStatusN: TextView
    private lateinit var tvAvgP: TextView
    private lateinit var tvStatusP: TextView
    private lateinit var tvAvgK: TextView
    private lateinit var tvStatusK: TextView

    // Other details
    private lateinit var tvAvgMoisture: TextView
    private lateinit var tvStatusMoisture: TextView
    private lateinit var tvAvgPh: TextView
    private lateinit var tvStatusPh: TextView
    private lateinit var tvAvgTemp: TextView
    private lateinit var tvStatusTemp: TextView
    private lateinit var tvAvgEc: TextView
    private lateinit var tvStatusEc: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone_management)

        db = FirebaseFirestore.getInstance()
        currentZoneId = intent.getStringExtra("ZONE_ID") ?: ""

        tvManageZoneName = findViewById(R.id.tvManageZoneName)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvCornAge = findViewById(R.id.tvCornAge)
        btnNewCornPlanted = findViewById(R.id.btnNewCornPlanted)
        tvSoilHealth = findViewById(R.id.tvSoilHealth)

        tvAvgN = findViewById(R.id.tvAvgN)
        tvStatusN = findViewById(R.id.tvStatusN)
        tvAvgP = findViewById(R.id.tvAvgP)
        tvStatusP = findViewById(R.id.tvStatusP)
        tvAvgK = findViewById(R.id.tvAvgK)
        tvStatusK = findViewById(R.id.tvStatusK)

        tvAvgMoisture = findViewById(R.id.tvAvgMoisture)
        tvStatusMoisture = findViewById(R.id.tvStatusMoisture)
        tvAvgPh = findViewById(R.id.tvAvgPh)
        tvStatusPh = findViewById(R.id.tvStatusPh)
        tvAvgTemp = findViewById(R.id.tvAvgTemp)
        tvStatusTemp = findViewById(R.id.tvStatusTemp)
        tvAvgEc = findViewById(R.id.tvAvgEc)
        tvStatusEc = findViewById(R.id.tvStatusEc)

        findViewById<Button>(R.id.btnRefresh).setOnClickListener {
            loadZoneData()
            Toast.makeText(this, "Refreshed", Toast.LENGTH_SHORT).show()
        }

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnSensorStatus).setOnClickListener {
            val intent = Intent(this, SensorStatusActivity::class.java)
            intent.putExtra("ZONE_ID", currentZoneId)
            startActivity(intent)
        }

        findViewById<Button>(R.id.btnDeleteZone).setOnClickListener {
            if (currentZoneId.isNotEmpty()) {
                db.collection("zones").document(currentZoneId).delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Zone deleted", Toast.LENGTH_SHORT).show()
                        finish()
                    }
            }
        }

        findViewById<Button>(R.id.btnHarvest).setOnClickListener {
            db.collection("zones").document(currentZoneId)
                .update("isHarvested", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Zone marked as harvested", Toast.LENGTH_SHORT).show()
                    loadZoneData()
                }
        }

        btnNewCornPlanted.setOnClickListener {
            val now = System.currentTimeMillis()
            db.collection("zones").document(currentZoneId)
                .update("dateOfPlanting", now, "isHarvested", false)
                .addOnSuccessListener {
                    Toast.makeText(this, "New crop record updated", Toast.LENGTH_SHORT).show()
                    loadZoneData()
                }
        }

        loadZoneData()
    }

    private fun loadZoneData() {
        if (currentZoneId.isEmpty()) return

        db.collection("zones").document(currentZoneId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    tvManageZoneName.text = doc.getString("zoneName") ?: "Zone Details"

                    val isHarvested = doc.getBoolean("isHarvested") ?: false
                    val plantingTime = doc.getLong("dateOfPlanting") ?: 0L

                    if (isHarvested) {
                        tvCornAge.text = "Harvested"
                        btnNewCornPlanted.visibility = View.VISIBLE
                    } else if (plantingTime > 0) {
                        val days = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - plantingTime)
                        tvCornAge.text = "$days days after planting"
                        btnNewCornPlanted.visibility = View.GONE
                    } else {
                        tvCornAge.text = "No planting date recorded"
                        btnNewCornPlanted.visibility = View.GONE
                    }

                    fetchZoneAverages()
                }
            }
    }

    private fun fetchZoneAverages() {
        // Fetch all documents from soil_data directly to safeguard against missing sensor mappings
        db.collection("soil_data").get().addOnSuccessListener { directSnapshot ->
            val directDocs = directSnapshot.documents.filter { doc ->
                val zId = doc.getString("zoneId")
                zId == null || zId == currentZoneId || zId.isEmpty()
            }

            if (directDocs.isNotEmpty()) {
                val latestDocs = directDocs
                    .groupBy { it.getString("deviceId") ?: it.getString("sensorId") ?: "default" }
                    .mapNotNull { (_, docs) -> docs.maxByOrNull { parseAnyDate(it)?.time ?: 0L } }

                processReadings(latestDocs)
            } else {
                updateUiWithAverages(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null)
            }
        }.addOnFailureListener { e ->
            Log.e("ZoneManagement", "Error fetching soil data", e)
            updateUiWithAverages(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null)
        }
    }

    private fun processReadings(readings: List<DocumentSnapshot>) {
        if (readings.isEmpty()) {
            updateUiWithAverages(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null)
            return
        }

        val count = readings.size.toDouble()
        val newestDate = readings.mapNotNull { parseAnyDate(it) }.maxByOrNull { it.time }

        val avgN = readings.sumOf { getDoubleValue(it, "nitrogen", "n", "N", "nitro") } / count
        val avgP = readings.sumOf { getDoubleValue(it, "phosphorus", "p", "P", "phos") } / count
        val avgK = readings.sumOf { getDoubleValue(it, "potassium", "k", "K", "pot") } / count
        val avgMoisture = readings.sumOf { getDoubleValue(it, "moisture", "humidity", "mois") } / count
        val avgPh = readings.sumOf { getDoubleValue(it, "ph", "pH", "PH") } / count
        val avgTemp = readings.sumOf { getDoubleValue(it, "temperature", "temp") } / count
        val avgEc = readings.sumOf { getDoubleValue(it, "ec", "EC") } / count

        updateUiWithAverages(avgN, avgP, avgK, avgMoisture, avgPh, avgTemp, avgEc, newestDate)
    }

    private fun getDoubleValue(doc: DocumentSnapshot, vararg keys: String): Double {
        for (key in keys) {
            val valDouble = doc.getDouble(key)
            if (valDouble != null) return valDouble

            val valLong = doc.getLong(key)
            if (valLong != null) return valLong.toDouble()

            val valString = doc.getString(key)
            if (valString != null) {
                val parsed = valString.toDoubleOrNull()
                if (parsed != null) return parsed
            }
        }
        return 0.0
    }

    private fun updateUiWithAverages(
        n: Double, p: Double, k: Double, m: Double,
        ph: Double, t: Double, ec: Double, latestDate: Date?
    ) {
        tvAvgN.text = String.format(Locale.US, "%.0f mg/kg", n)
        tvStatusN.text = getParameterStatus("N", n)

        tvAvgP.text = String.format(Locale.US, "%.0f mg/kg", p)
        tvStatusP.text = getParameterStatus("P", p)

        tvAvgK.text = String.format(Locale.US, "%.0f mg/kg", k)
        tvStatusK.text = getParameterStatus("K", k)

        tvAvgMoisture.text = String.format(Locale.US, "%.0f%%", m)
        tvStatusMoisture.text = getParameterStatus("MOISTURE", m)

        tvAvgPh.text = String.format(Locale.US, "%.1f", ph)
        tvStatusPh.text = getParameterStatus("PH", ph)

        tvAvgTemp.text = String.format(Locale.US, "%.0f°C", t)
        tvStatusTemp.text = getParameterStatus("TEMP", t)

        tvAvgEc.text = String.format(Locale.US, "%.0f us/cm", ec)
        tvStatusEc.text = getParameterStatus("EC", ec)

        if (n == 0.0 && p == 0.0 && k == 0.0) {
            tvSoilHealth.text = "Soil Health: Poor (0/100)"
        } else {
            val overallHealth = calculateOverallSoilHealth(n, p, k, m, ph)
            tvSoilHealth.text = "Soil Health: $overallHealth"
        }

        if (latestDate != null) {
            val timeFormat = SimpleDateFormat("MMM dd, yyyy - hh:mm:ss a", Locale.US)
            tvLastUpdated.text = "Last updated: ${timeFormat.format(latestDate)}"
        } else {
            tvLastUpdated.text = "Last updated: No data"
        }
    }

    private fun getParameterStatus(type: String, value: Double): String {
        if (value <= 0.0) return "Low"
        return try {
            SoilUtils.getStatus(type, value)
        } catch (_: Exception) {
            when (type) {
                "N" -> if (value in 20.0..50.0) "Normal" else "Low"
                "P" -> if (value in 10.0..30.0) "Normal" else "Low"
                "K" -> if (value in 100.0..200.0) "Normal" else "Low"
                "MOISTURE" -> if (value in 40.0..80.0) "Normal" else "Low"
                "PH" -> if (value in 5.5..7.5) "Normal" else "Low"
                else -> if (value > 0) "Normal" else "Low"
            }
        }
    }

    private fun calculateOverallSoilHealth(n: Double, p: Double, k: Double, m: Double, ph: Double): String {
        var score = 0
        if (n >= 20) score += 20
        if (p >= 10) score += 20
        if (k >= 100) score += 20
        if (m in 40.0..80.0) score += 20
        if (ph in 5.5..7.5) score += 20

        return when {
            score >= 80 -> "Optimal ($score/100)"
            score >= 40 -> "Fair ($score/100)"
            else -> "Poor ($score/100)"
        }
    }

    private fun parseAnyDate(doc: DocumentSnapshot): Date? {
        val keys = arrayOf("timestamp", "lastScanTimestamp", "createdAt", "date", "updatedAt")
        for (key in keys) {
            val rawValue = doc.get(key) ?: continue
            when (rawValue) {
                is Timestamp -> return rawValue.toDate()
                is Long -> return Date(rawValue)
                is Double -> return Date(rawValue.toLong())
                is String -> {
                    val formats = arrayOf(
                        "yyyy-MM-dd HH:mm:ss",
                        "MMMM dd, yyyy 'at' h:mm:ss a z",
                        "MMMM dd, yyyy - hh:mm:ss a"
                    )
                    for (fmt in formats) {
                        try {
                            val parsed = SimpleDateFormat(fmt, Locale.US).parse(rawValue)
                            if (parsed != null) return parsed
                        } catch (_: Exception) {}
                    }
                }
            }
        }
        return null
    }
}