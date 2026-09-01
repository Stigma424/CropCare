package com.example.cropcare

import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorHistoryActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private val historyList = mutableListOf<HistoryItem>()
    private lateinit var adapter: SensorHistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_history)

        db = FirebaseFirestore.getInstance()

        val deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        val metricKey = intent.getStringExtra("METRIC_KEY") ?: "nitrogen"
        val metricTitle = intent.getStringExtra("METRIC_TITLE") ?: "Nitrogen"
        val unit = intent.getStringExtra("UNIT") ?: "mg/kg"

        val tvMetricTitle = findViewById<TextView>(R.id.tvMetricTitle)
        val btnBack = findViewById<ImageButton>(R.id.btnBackHistory)
        val rvHistory = findViewById<RecyclerView>(R.id.rvHistory)

        tvMetricTitle.text = metricTitle

        rvHistory.layoutManager = LinearLayoutManager(this)
        adapter = SensorHistoryAdapter(historyList)
        rvHistory.adapter = adapter

        btnBack.setOnClickListener { finish() }

        fetchHistory(deviceId, metricKey, metricTitle, unit)
    }

    private fun fetchHistory(deviceId: String, metricKey: String, metricTitle: String, unit: String) {
        db.collection("soil_data")
            .whereEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener { docs ->
                historyList.clear()
                val fullDateFormat = SimpleDateFormat("MMMM dd, yyyy - hh:mm:ss a", Locale.US)

                val sortedDocs = docs.documents.sortedByDescending {
                    parseAnyDate(it)?.time ?: 0L
                }

                for (doc in sortedDocs) {
                    val dateObj = parseAnyDate(doc) ?: continue
                    val valDouble = doc.getDouble(metricKey) ?: 0.0

                    val timeStr = fullDateFormat.format(dateObj)
                    val valueStr = if (unit.isEmpty()) {
                        String.format(Locale.US, "%s = %.1f", metricTitle, valDouble)
                    } else {
                        String.format(Locale.US, "%s = %.0f %s", metricTitle, valDouble, unit)
                    }

                    historyList.add(HistoryItem(timeStr, valueStr))
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
}