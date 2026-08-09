package com.example.cropcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ZoneManagementActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private var zoneId: String = ""
    private var currentPlantingDate: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone_management)

        db = FirebaseFirestore.getInstance()
        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        val tvManageZoneName = findViewById<TextView>(R.id.tvManageZoneName)
        val tvCornAge = findViewById<TextView>(R.id.tvCornAge)
        val btnHarvest = findViewById<Button>(R.id.btnHarvest)
        val btnNewCornPlanted = findViewById<Button>(R.id.btnNewCornPlanted)
        val btnSensorStatus = findViewById<Button>(R.id.btnSensorStatus)
        val btnDeleteZone = findViewById<Button>(R.id.btnDeleteZone)
        val btnBack = findViewById<Button>(R.id.btnBack)

        loadZoneData(tvManageZoneName, tvCornAge, btnHarvest, btnNewCornPlanted)

        btnHarvest.setOnClickListener {
            db.collection("zones").document(zoneId)
                .update("isHarvested", true)
                .addOnSuccessListener {
                    Toast.makeText(this, "Zone harvested!", Toast.LENGTH_SHORT).show()
                    btnHarvest.visibility = View.GONE
                    btnNewCornPlanted.visibility = View.VISIBLE
                    tvCornAge.text = "Crop Status: Harvested"
                }
        }

        btnNewCornPlanted.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                    }
                    val newDateMillis = selectedCal.timeInMillis

                    val updates = mapOf(
                        "dateOfPlanting" to newDateMillis,
                        "isHarvested" to false
                    )

                    db.collection("zones").document(zoneId).update(updates)
                        .addOnSuccessListener {
                            Toast.makeText(this, "New crop date set!", Toast.LENGTH_SHORT).show()
                            loadZoneData(tvManageZoneName, tvCornAge, btnHarvest, btnNewCornPlanted)
                        }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnSensorStatus.setOnClickListener {
            Toast.makeText(this, "Sensors: All systems operational", Toast.LENGTH_SHORT).show()
        }

        btnDeleteZone.setOnClickListener {
            db.collection("zones").document(zoneId).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Zone deleted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadZoneData(
        tvName: TextView,
        tvAge: TextView,
        btnHarvest: Button,
        btnReplant: Button
    ) {
        if (zoneId.isEmpty()) return

        db.collection("zones").document(zoneId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val name = doc.getString("zoneName") ?: "Zone"
                    val isHarvested = doc.getBoolean("isHarvested") ?: false
                    currentPlantingDate = doc.getLong("dateOfPlanting") ?: 0L

                    tvName.text = name

                    if (isHarvested) {
                        tvAge.text = "Crop Status: Harvested"
                        btnHarvest.visibility = View.GONE
                        btnReplant.visibility = View.VISIBLE
                    } else {
                        val ageDays = calculateAgeInDays(currentPlantingDate)
                        tvAge.text = "Corn Age: $ageDays days"
                        btnHarvest.visibility = View.VISIBLE
                        btnReplant.visibility = View.GONE
                    }
                }
            }
    }

    private fun calculateAgeInDays(plantingDateMillis: Long): Long {
        if (plantingDateMillis == 0L) return 0
        val diff = System.currentTimeMillis() - plantingDateMillis
        return if (diff > 0) diff / (1000 * 60 * 60 * 24) else 0
    }
}