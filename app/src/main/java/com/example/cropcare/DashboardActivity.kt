package com.example.cropcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var zoneAdapter: ZoneAdapter
    private val zoneList = mutableListOf<ZoneModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        checkNotificationPermission()

        val tvWelcome = findViewById<TextView?>(R.id.tvWelcome)
        tvWelcome?.text = "Good Morning, User"
        loadUsername()

        val rvZones = findViewById<RecyclerView?>(R.id.rvZones)
        rvZones?.let {
            it.layoutManager = LinearLayoutManager(this)
            it.isNestedScrollingEnabled = false
            zoneAdapter = ZoneAdapter(zoneList) { zoneId ->
                val intent = Intent(this, ZoneManagementActivity::class.java)
                intent.putExtra("ZONE_ID", zoneId)
                startActivity(intent)
            }
            it.adapter = zoneAdapter
        }

        // Bottom Navigation & Actions
        findViewById<View?>(R.id.navHome)?.setOnClickListener {
            // Already home
        }
        findViewById<View?>(R.id.navAddZone)?.setOnClickListener {
            startActivity(Intent(this, AddZoneActivity::class.java))
        }
        findViewById<View?>(R.id.navNotif)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View?>(R.id.navSettings)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        findViewById<View?>(R.id.btnRefreshZones)?.setOnClickListener {
            fetchUserZones()
        }

        // Retrieve all zones the user has access to when first opened
        fetchUserZones()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUsername()
        fetchUserZones()
    }

    private fun loadUsername() {
        val currentUser = auth.currentUser ?: return
        val tvWelcome = findViewById<TextView?>(R.id.tvWelcome)

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val uname = doc.getString("username")
                    val fName = doc.getString("firstName")
                    val nameToDisplay = when {
                        !uname.isNullOrEmpty() -> uname
                        !fName.isNullOrEmpty() -> fName
                        else -> currentUser.email?.substringBefore('@') ?: "User"
                    }
                    tvWelcome?.text = "Good Morning, $nameToDisplay"
                } else {
                    val fallback = currentUser.email?.substringBefore('@') ?: "User"
                    tvWelcome?.text = "Good Morning, $fallback"
                }
            }
            .addOnFailureListener {
                val fallback = currentUser.email?.substringBefore('@') ?: "User"
                tvWelcome?.text = "Good Morning, $fallback"
            }
    }

    private fun fetchUserZones() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("zones")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                processZoneDocuments(documents.documents)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching zones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processZoneDocuments(zoneDocs: List<DocumentSnapshot>) {
        val tvActiveZonesCount = findViewById<TextView>(R.id.tvActiveZonesCount)
        tvActiveZonesCount?.text = "${zoneDocs.size} zones active"

        if (zoneDocs.isEmpty()) {
            zoneList.clear()
            zoneAdapter.notifyDataSetChanged()
            findViewById<TextView>(R.id.tvDashboardSoilHealth)?.text = "0%"
            return
        }

        // Immediately map zone documents and populate RecyclerView adapter
        val tempZoneList = zoneDocs.map { doc ->
            ZoneModel(
                zoneId = doc.id,
                zoneName = doc.getString("zoneName") ?: "",
                zoneAreaSqm = doc.getDouble("zoneAreaSqm") ?: 0.0,
                dateOfPlanting = doc.getLong("dateOfPlanting") ?: 0L,
                isHarvested = doc.getBoolean("isHarvested") ?: false
            )
        }

        zoneList.clear()
        zoneList.addAll(tempZoneList)
        zoneAdapter.notifyDataSetChanged()

        // Fetch health scores asynchronously for dashboard overall average
        val zoneTasks = zoneDocs.map { zoneDoc ->
            fetchZoneHealthScore(zoneDoc.id)
        }

        Tasks.whenAllComplete(zoneTasks).addOnCompleteListener { _ ->
            var totalHealth = 0.0
            var validHealthCount = 0

            for (task in zoneTasks) {
                if (task.isSuccessful && task.result != null) {
                    val score = task.result as Double
                    if (score > 0.0) {
                        totalHealth += score
                        validHealthCount++
                    }
                }
            }

            val avgHealth = if (validHealthCount > 0) totalHealth / validHealthCount else 0.0
            findViewById<TextView>(R.id.tvDashboardSoilHealth)?.text = String.format(Locale.US, "%.0f%%", avgHealth)

            Toast.makeText(this, "Zones refreshed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchZoneHealthScore(zoneId: String): Task<Double> {
        val completionSource = TaskCompletionSource<Double>()
        val userId = auth.currentUser?.uid ?: run {
            completionSource.setResult(0.0)
            return completionSource.task
        }

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { sensorDocs ->
                val deviceIds = sensorDocs.mapNotNull { it.getString("deviceId") }.distinct()
                if (deviceIds.isEmpty()) {
                    completionSource.setResult(0.0)
                    return@addOnSuccessListener
                }

                db.collection("soil_data")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { soilDocs ->
                        if (soilDocs.isEmpty) {
                            completionSource.setResult(0.0)
                            return@addOnSuccessListener
                        }

                        val latestDocs = soilDocs.documents
                            .groupBy { it.getString("deviceId") ?: "default" }
                            .mapNotNull { (_, docs) -> docs.maxByOrNull { parseAnyDate(it)?.time ?: 0L } }

                        val score = calculateHealthScore(latestDocs)
                        completionSource.setResult(score)
                    }
                    .addOnFailureListener { completionSource.setResult(0.0) }
            }
            .addOnFailureListener { completionSource.setResult(0.0) }

        return completionSource.task
    }

    private fun calculateHealthScore(readings: List<DocumentSnapshot>): Double {
        if (readings.isEmpty()) return 0.0
        val count = readings.size.toDouble()

        val avgN = readings.sumOf { getDoubleValue(it, "nitrogen", "n", "N", "nitro") } / count
        val avgP = readings.sumOf { getDoubleValue(it, "phosphorus", "p", "P", "phos") } / count
        val avgK = readings.sumOf { getDoubleValue(it, "potassium", "k", "K", "pot") } / count
        val avgMoisture = readings.sumOf { getDoubleValue(it, "moisture", "humidity", "mois") } / count
        val avgPh = readings.sumOf { getDoubleValue(it, "ph", "pH", "PH") } / count

        var score = 0.0
        if (avgN >= 20) score += 20
        if (avgP >= 10) score += 20
        if (avgK >= 100) score += 20
        if (avgMoisture in 40.0..80.0) score += 20
        if (avgPh in 5.5..7.5) score += 20

        return score
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