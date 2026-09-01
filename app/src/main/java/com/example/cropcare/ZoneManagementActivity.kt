package com.example.cropcare

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar
import java.util.Locale

class ZoneManagementActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var zoneId: String = ""
    private var currentPlantingDate: Long = 0L

    private lateinit var tvAvgN: TextView
    private lateinit var tvStatusN: TextView
    private lateinit var tvAvgP: TextView
    private lateinit var tvStatusP: TextView
    private lateinit var tvAvgK: TextView
    private lateinit var tvStatusK: TextView

    private lateinit var tvAvgMoisture: TextView
    private lateinit var tvStatusMoisture: TextView
    private lateinit var tvAvgPh: TextView
    private lateinit var tvStatusPh: TextView
    private lateinit var tvAvgTemp: TextView
    private lateinit var tvStatusTemp: TextView
    private lateinit var tvAvgEc: TextView
    private lateinit var tvStatusEc: TextView

    private lateinit var tvSoilHealth: TextView
    private lateinit var tvRecommendationText: TextView
    private lateinit var btnSeeMoreRec: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone_management)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        val tvManageZoneName = findViewById<TextView>(R.id.tvManageZoneName)
        val tvCornAge = findViewById<TextView>(R.id.tvCornAge)
        val btnHarvest = findViewById<Button>(R.id.btnHarvest)
        val btnNewCornPlanted = findViewById<Button>(R.id.btnNewCornPlanted)
        val btnSensorStatus = findViewById<Button>(R.id.btnSensorStatus)
        val btnDeleteZone = findViewById<Button>(R.id.btnDeleteZone)
        val btnBack = findViewById<Button>(R.id.btnBack)
        val btnRefresh = findViewById<Button>(R.id.btnRefresh)

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

        tvSoilHealth = findViewById(R.id.tvSoilHealth)
        tvRecommendationText = findViewById(R.id.tvRecommendationText)
        btnSeeMoreRec = findViewById(R.id.btnSeeMoreRec)

        loadZoneData(tvManageZoneName, tvCornAge, btnHarvest, btnNewCornPlanted)

        btnRefresh.setOnClickListener {
            loadZoneData(tvManageZoneName, tvCornAge, btnHarvest, btnNewCornPlanted)
            Toast.makeText(this, "Readings updated", Toast.LENGTH_SHORT).show()
        }

        btnSeeMoreRec.setOnClickListener {
            Toast.makeText(this, "Opening full recommendation details...", Toast.LENGTH_SHORT).show()
        }

        btnSensorStatus.setOnClickListener {
            val intent = Intent(this, SensorStatusActivity::class.java)
            intent.putExtra("ZONE_ID", zoneId)
            startActivity(intent)
        }

        btnDeleteZone.setOnClickListener {
            db.collection("zones").document(zoneId).delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "Zone deleted!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        btnBack.setOnClickListener { finish() }
    }

    private fun loadZoneData(tvName: TextView, tvAge: TextView, btnHarvest: Button, btnReplant: Button) {
        if (zoneId.isEmpty()) return
        db.collection("zones").document(zoneId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                tvName.text = doc.getString("zoneName") ?: "Zone"
                val isHarvested = doc.getBoolean("isHarvested") ?: false
                currentPlantingDate = doc.getLong("dateOfPlanting") ?: 0L

                if (isHarvested) {
                    tvAge.text = "Crop Status: Harvested"
                    btnHarvest.visibility = View.GONE
                    btnReplant.visibility = View.VISIBLE
                } else {
                    val ageDays = calculateAgeInDays(currentPlantingDate)
                    tvAge.text = "$ageDays days after planting"
                    btnHarvest.visibility = View.VISIBLE
                    btnReplant.visibility = View.GONE
                }
                fetchZoneSensorAverages()
            }
        }
    }

    private fun fetchZoneSensorAverages() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("sensors")
            .whereEqualTo("zoneId", zoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { sensorDocs ->
                val deviceIds = sensorDocs.mapNotNull { it.getString("deviceId") }
                if (deviceIds.isEmpty()) return@addOnSuccessListener

                db.collection("soil_data")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { soilDocs ->
                        if (soilDocs.isEmpty) return@addOnSuccessListener

                        var sumN = 0.0; var sumP = 0.0; var sumK = 0.0
                        var sumM = 0.0; var sumPh = 0.0; var sumT = 0.0; var sumEc = 0.0
                        val count = soilDocs.size()

                        for (doc in soilDocs) {
                            sumN += doc.getDouble("nitrogen") ?: 0.0
                            sumP += doc.getDouble("phosphorus") ?: 0.0
                            sumK += doc.getDouble("potassium") ?: 0.0
                            sumM += doc.getDouble("moisture") ?: 0.0
                            sumPh += doc.getDouble("ph") ?: 0.0
                            sumT += doc.getDouble("temperature") ?: 0.0
                            sumEc += doc.getDouble("ec") ?: 0.0
                        }

                        val avgN = sumN / count; val avgP = sumP / count; val avgK = sumK / count
                        val avgM = sumM / count; val avgPh = sumPh / count; val avgT = sumT / count; val avgEc = sumEc / count

                        tvAvgN.text = String.format(Locale.US, "%.0f mg/kg", avgN)
                        tvStatusN.text = SoilUtils.getStatus("N", avgN)

                        tvAvgP.text = String.format(Locale.US, "%.0f mg/kg", avgP)
                        tvStatusP.text = SoilUtils.getStatus("P", avgP)

                        tvAvgK.text = String.format(Locale.US, "%.0f mg/kg", avgK)
                        tvStatusK.text = SoilUtils.getStatus("K", avgK)

                        tvAvgMoisture.text = String.format(Locale.US, "%.0f%%", avgM)
                        tvStatusMoisture.text = SoilUtils.getStatus("MOISTURE", avgM)

                        tvAvgPh.text = String.format(Locale.US, "%.1f", avgPh)
                        tvStatusPh.text = SoilUtils.getStatus("PH", avgPh)

                        tvAvgTemp.text = String.format(Locale.US, "%.0f°C", avgT)
                        tvStatusTemp.text = SoilUtils.getStatus("TEMP", avgT)

                        tvAvgEc.text = String.format(Locale.US, "%.0f", avgEc)
                        tvStatusEc.text = SoilUtils.getStatus("EC", avgEc)

                        tvSoilHealth.text = "Soil Health: 90%"

                        // Recommendations placeholder logic
                        if (avgN < 30) {
                            tvRecommendationText.text = "Add 50 kilos of urea fertilizer to promote green leafy growth."
                        } else {
                            tvRecommendationText.text = "All parameters are balanced. Maintain standard irrigation schedule."
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