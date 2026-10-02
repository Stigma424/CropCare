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
        val currentUser = auth.currentUser
        tvWelcome?.text = "Welcome to Dashboard!\nLogged in as: ${currentUser?.email ?: "User"}"

        val rvZones = findViewById<RecyclerView?>(R.id.rvZones)
        rvZones?.let {
            it.layoutManager = LinearLayoutManager(this)
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
        fetchUserZones()
    }

    private fun fetchUserZones() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("zones")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val zoneDocs = documents.documents
                val tvActiveZonesCount = findViewById<TextView>(R.id.tvActiveZonesCount)
                tvActiveZonesCount?.text = "${zoneDocs.size} zones active"

                if (zoneDocs.isEmpty()) {
                    zoneList.clear()
                    zoneAdapter.notifyDataSetChanged()
                    findViewById<TextView>(R.id.tvDashboardSoilHealth)?.text = "0%"
                    return@addOnSuccessListener
                }

                val zoneTasks = zoneDocs.map { zoneDoc ->
                    val zoneId = zoneDoc.id
                    fetchZoneHealthScore(zoneId)
                }

                Tasks.whenAllComplete(zoneTasks).addOnCompleteListener { _ ->
                    val tempZoneList = mutableListOf<ZoneModel>()
                    var totalHealth = 0.0
                    var validHealthCount = 0

                    for ((index, doc) in zoneDocs.withIndex()) {
                        val zone = ZoneModel(
                            zoneId = doc.id,
                            zoneName = doc.getString("zoneName") ?: "",
                            zoneAreaSqm = doc.getDouble("zoneAreaSqm") ?: 0.0,
                            dateOfPlanting = doc.getLong("dateOfPlanting") ?: 0L,
                            isHarvested = doc.getBoolean("isHarvested") ?: false
                        )
                        tempZoneList.add(zone)

                        val task = zoneTasks[index]
                        if (task.isSuccessful && task.result != null) {
                            val score = task.result as Double
                            if (score > 0.0) {
                                totalHealth += score
                                validHealthCount++
                            }
                        }
                    }

                    zoneList.clear()
                    zoneList.addAll(tempZoneList)
                    zoneAdapter.notifyDataSetChanged()

                    val avgHealth = if (validHealthCount > 0) totalHealth / validHealthCount else 0.0
                    findViewById<TextView>(R.id.tvDashboardSoilHealth)?.text = String.format(Locale.US, "%.0f%%", avgHealth)

                    Toast.makeText(this, "Zones refreshed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching zones: ${e.message}", Toast.LENGTH_SHORT).show()
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

                        if (latestDocs.isEmpty()) {
                            completionSource.setResult(0.0)
                            return@addOnSuccessListener
                        }

                        val count = latestDocs.size.toDouble()
                        var sumN = 0.0; var sumP = 0.0; var sumK = 0.0
                        var sumMoisture = 0.0; var sumPh = 0.0

                        for (doc in latestDocs) {
                            sumN += getDoubleValue(doc, "nitrogen", "n", "N")
                            sumP += getDoubleValue(doc, "phosphorus", "p", "P")
                            sumK += getDoubleValue(doc, "potassium", "k", "K")
                            sumMoisture += getDoubleValue(doc, "moisture", "humidity")
                            sumPh += getDoubleValue(doc, "ph", "pH")
                        }

                        val avgN = sumN / count
                        val avgP = sumP / count
                        val avgK = sumK / count
                        val avgMoisture = sumMoisture / count
                        val avgPh = sumPh / count

                        var score = 0
                        if (avgN in 20.0..80.0) score += 20 else if (avgN > 10) score += 10
                        if (avgP in 10.0..50.0) score += 20 else if (avgP > 5) score += 10
                        if (avgK in 20.0..60.0) score += 20 else if (avgK > 10) score += 10
                        if (avgMoisture in 30.0..75.0) score += 20 else if (avgMoisture in 15.0..85.0) score += 10
                        if (avgPh in 5.8..7.5) score += 20 else if (avgPh in 5.0..8.0) score += 10

                        completionSource.setResult(score.toDouble())
                    }
                    .addOnFailureListener {
                        completionSource.setResult(0.0)
                    }
            }
            .addOnFailureListener {
                completionSource.setResult(0.0)
            }

        return completionSource.task
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
        val rawValue = doc.get("timestamp") ?: doc.get("lastScanTimestamp")
        return when (rawValue) {
            is Timestamp -> rawValue.toDate()
            is Long -> Date(rawValue)
            is Double -> Date(rawValue.toLong())
            else -> null
        }
    }
}
