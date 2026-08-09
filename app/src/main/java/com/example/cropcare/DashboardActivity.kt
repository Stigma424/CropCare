package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
                for (doc in documents) {
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
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching zones: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}