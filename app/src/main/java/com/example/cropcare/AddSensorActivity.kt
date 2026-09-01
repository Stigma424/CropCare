package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.random.Random

class AddSensorActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        val zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        val etSensorId = findViewById<EditText>(R.id.etSensorId)
        val etSensorName = findViewById<EditText>(R.id.etSensorName)
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        btnSave.setOnClickListener {
            val deviceId = etSensorId.text.toString().trim()
            val sensorName = etSensorName.text.toString().trim()
            val userId = auth.currentUser?.uid ?: ""

            if (deviceId.isEmpty() || sensorName.isEmpty()) {
                Toast.makeText(this, "Please enter both Sensor ID and Name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newDocRef = db.collection("sensors").document()
            val sensorMap = hashMapOf(
                "sensorId" to newDocRef.id,
                "deviceId" to deviceId,
                "sensorName" to sensorName,
                "zoneId" to zoneId,
                "userId" to userId,
                "lastScanTimestamp" to System.currentTimeMillis(),
                "isOnline" to true
            )

            newDocRef.set(sensorMap).addOnSuccessListener {
                // Populate random readings for soil_data matching schema
                val soilDataMap = hashMapOf(
                    "deviceId" to deviceId,
                    "ec" to Random.nextInt(20, 45),
                    "moisture" to Random.nextDouble(18.0, 55.0),
                    "nitrogen" to Random.nextInt(10, 60),
                    "ph" to Random.nextDouble(5.0, 7.5),
                    "phosphorus" to Random.nextInt(1, 15),
                    "potassium" to Random.nextInt(1, 20),
                    "temperature" to Random.nextDouble(25.0, 38.0),
                    "timestamp" to FieldValue.serverTimestamp()
                )
                db.collection("soil_data").add(soilDataMap)

                Toast.makeText(this, "Sensor added successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }
    }
}