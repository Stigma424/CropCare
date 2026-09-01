package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Locale

class SensorDetailActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)

        db = FirebaseFirestore.getInstance()

        val sensorId = intent.getStringExtra("SENSOR_ID") ?: ""
        val deviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        val sensorName = intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

        val tvTitle = findViewById<TextView>(R.id.tvSensorDetailTitle)

        val tvN = findViewById<TextView>(R.id.tvSensorN)
        val tvStatusN = findViewById<TextView>(R.id.tvStatusN)
        val tvP = findViewById<TextView>(R.id.tvSensorP)
        val tvStatusP = findViewById<TextView>(R.id.tvStatusP)
        val tvK = findViewById<TextView>(R.id.tvSensorK)
        val tvStatusK = findViewById<TextView>(R.id.tvStatusK)

        val tvMoisture = findViewById<TextView>(R.id.tvSensorMoisture)
        val tvStatusMoisture = findViewById<TextView>(R.id.tvStatusMoisture)
        val tvPh = findViewById<TextView>(R.id.tvSensorPh)
        val tvStatusPh = findViewById<TextView>(R.id.tvStatusPh)
        val tvTemp = findViewById<TextView>(R.id.tvSensorTemp)
        val tvStatusTemp = findViewById<TextView>(R.id.tvStatusTemp)
        val tvEc = findViewById<TextView>(R.id.tvSensorEc)
        val tvStatusEc = findViewById<TextView>(R.id.tvStatusEc)

        val btnDeleteSensor = findViewById<Button>(R.id.btnDeleteSensor)
        val btnEditSensor = findViewById<Button>(R.id.btnEditSensor)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        tvTitle.text = sensorName

        db.collection("soil_data")
            .whereEqualTo("deviceId", deviceId)
            .get()
            .addOnSuccessListener { docs ->
                if (!docs.isEmpty) {
                    val doc = docs.documents[0]
                    val nVal = doc.getDouble("nitrogen") ?: 0.0
                    val pVal = doc.getDouble("phosphorus") ?: 0.0
                    val kVal = doc.getDouble("potassium") ?: 0.0
                    val mVal = doc.getDouble("moisture") ?: 0.0
                    val phVal = doc.getDouble("ph") ?: 0.0
                    val tVal = doc.getDouble("temperature") ?: 0.0
                    val ecVal = doc.getDouble("ec") ?: 0.0

                    tvN.text = String.format(Locale.US, "%.0f mg/kg", nVal)
                    tvStatusN.text = SoilUtils.getStatus("N", nVal)

                    tvP.text = String.format(Locale.US, "%.0f mg/kg", pVal)
                    tvStatusP.text = SoilUtils.getStatus("P", pVal)

                    tvK.text = String.format(Locale.US, "%.0f mg/kg", kVal)
                    tvStatusK.text = SoilUtils.getStatus("K", kVal)

                    tvMoisture.text = String.format(Locale.US, "%.0f%%", mVal)
                    tvStatusMoisture.text = SoilUtils.getStatus("MOISTURE", mVal)

                    tvPh.text = String.format(Locale.US, "%.1f", phVal)
                    tvStatusPh.text = SoilUtils.getStatus("PH", phVal)

                    tvTemp.text = String.format(Locale.US, "%.0f°C", tVal)
                    tvStatusTemp.text = SoilUtils.getStatus("TEMP", tVal)

                    tvEc.text = String.format(Locale.US, "%.0f", ecVal)
                    tvStatusEc.text = SoilUtils.getStatus("EC", ecVal)
                }
            }

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
}