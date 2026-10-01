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

            // Validate Sensor ID format (alphanumeric, dashes, underscores, length >= 3)
            val isValidFormat = Regex("^[a-zA-Z0-9_-]{3,30}$").matches(deviceId)
            if (!isValidFormat) {
                Toast.makeText(
                    this,
                    "Invalid Sensor ID format. Use 3-30 letters, numbers, or dashes (e.g. SENSOR-101).",
                    Toast.LENGTH_LONG
                ).show()
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            Toast.makeText(this, "Validating Sensor ID...", Toast.LENGTH_SHORT).show()

            // Check if deviceId already exists in Firestore sensors collection
            db.collection("sensors")
                .whereEqualTo("deviceId", deviceId)
                .get()
                .addOnSuccessListener { existingSensors ->
                    if (!existingSensors.isEmpty) {
                        btnSave.isEnabled = true
                        Toast.makeText(
                            this,
                            "Sensor ID '$deviceId' is already registered in the system. Please enter a valid unique Sensor ID.",
                            Toast.LENGTH_LONG
                        ).show()
                        return@addOnSuccessListener
                    }

                    // Save new valid sensor
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
                        val soilDataMap = hashMapOf(
                            "deviceId" to deviceId,
                            "zoneId" to zoneId,
                            "userId" to userId,
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
                        btnSave.isEnabled = true
                        Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Validation error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }
    }
}
