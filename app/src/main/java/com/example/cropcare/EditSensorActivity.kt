package com.example.cropcare

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class EditSensorActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_sensor)

        db = FirebaseFirestore.getInstance()

        val sensorId = intent.getStringExtra("SENSOR_ID") ?: ""
        val currentDeviceId = intent.getStringExtra("DEVICE_ID") ?: ""
        val currentSensorName = intent.getStringExtra("SENSOR_NAME") ?: ""

        val etSensorId = findViewById<EditText>(R.id.etEditSensorId)
        val etSensorName = findViewById<EditText>(R.id.etEditSensorName)
        val btnSave = findViewById<TextView>(R.id.btnSave)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        etSensorId.setText(currentDeviceId)
        etSensorName.setText(currentSensorName)

        btnSave.setOnClickListener {
            val newDeviceId = etSensorId.text.toString().trim()
            val newSensorName = etSensorName.text.toString().trim()

            if (newDeviceId.isEmpty() || newSensorName.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "deviceId" to newDeviceId,
                "sensorName" to newSensorName
            )

            if (sensorId.isNotEmpty()) {
                db.collection("sensors").document(sensorId).update(updates)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Sensor updated!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else if (currentDeviceId.isNotEmpty()) {
                db.collection("sensors").whereEqualTo("deviceId", currentDeviceId).get()
                    .addOnSuccessListener { query ->
                        if (!query.isEmpty) {
                            val docId = query.documents[0].id
                            db.collection("sensors").document(docId).update(updates)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Sensor updated!", Toast.LENGTH_SHORT).show()
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Sensor not found in database", Toast.LENGTH_SHORT).show()
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Search failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(this, "Missing sensor identifier", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }
    }
}