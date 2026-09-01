package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.util.Date

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

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val btnAddZone = findViewById<Button>(R.id.btnAddZone)
        val btnRefreshZones = findViewById<Button>(R.id.btnRefreshZones)
        val rvZones = findViewById<RecyclerView>(R.id.rvZones)

        val currentUser = auth.currentUser
        tvWelcome.text = "Welcome to Dashboard!\nLogged in as: ${currentUser?.email ?: "User"}"

        rvZones.layoutManager = LinearLayoutManager(this)
        zoneAdapter = ZoneAdapter(zoneList) { zoneId ->
            val intent = Intent(this, ZoneManagementActivity::class.java)
            intent.putExtra("ZONE_ID", zoneId)
            startActivity(intent)
        }
        rvZones.adapter = zoneAdapter

        btnAddZone.setOnClickListener {
            startActivity(Intent(this, AddZoneActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnRefreshZones.setOnClickListener {
            fetchUserZones()
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
                zoneList.clear()

                val zoneDocs = documents.documents
                if (zoneDocs.isEmpty()) {
                    zoneAdapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                // Fetch latest readings per zone in parallel
                val zoneTasks = zoneDocs.map { zoneDoc ->
                    val zoneId = zoneDoc.getString("zoneId") ?: zoneDoc.id
                    fetchLatestZoneAverages(zoneId)
                }

                Tasks.whenAllComplete(zoneTasks).addOnCompleteListener {
                    for (i in zoneDocs.indices) {
                        val doc = zoneDocs[i]
                        val zone = ZoneModel(
                            zoneId = doc.getString("zoneId") ?: doc.id,
                            zoneName = doc.getString("zoneName") ?: "",
                            zoneAreaSqm = doc.getDouble("zoneAreaSqm") ?: 0.0,
                            dateOfPlanting = doc.getLong("dateOfPlanting") ?: 0L,
                            isHarvested = doc.getBoolean("isHarvested") ?: false
                        )
                        zoneList.add(zone)
                    }
                    zoneAdapter.notifyDataSetChanged()
                    Toast.makeText(this, "Zones refreshed", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching zones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchLatestZoneAverages(zoneId: String): com.google.android.gms.tasks.Task<Void> {
        val completionSource = com.google.android.gms.tasks.TaskCompletionSource<Void>()

        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .get()
            .addOnSuccessListener { sensorDocs ->
                val deviceIds = sensorDocs.mapNotNull { it.getString("deviceId") }.distinct()
                if (deviceIds.isEmpty()) {
                    completionSource.setResult(null)
                    return@addOnSuccessListener
                }

                val tasks = deviceIds.map { deviceId ->
                    db.collection("soil_data")
                        .whereEqualTo("deviceId", deviceId)
                        .get()
                }

                Tasks.whenAllSuccess<QuerySnapshot>(tasks).addOnSuccessListener { snapshots ->
                    val latestReadings = mutableListOf<DocumentSnapshot>()
                    for (snapshot in snapshots) {
                        val latest = snapshot.documents.maxByOrNull { parseAnyDate(it)?.time ?: 0L }
                        if (latest != null) latestReadings.add(latest)
                    }
                    completionSource.setResult(null)
                }.addOnFailureListener {
                    completionSource.setResult(null)
                }
            }
            .addOnFailureListener {
                completionSource.setResult(null)
            }

        return completionSource.task
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