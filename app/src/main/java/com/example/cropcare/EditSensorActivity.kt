package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
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
        val btnSave = findViewById<Button>(R.id.btnSave)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

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

            db.collection("sensors").document(sensorId).update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Sensor updated!", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }

        btnCancel.setOnClickListener { finish() }
        btnBack.setOnClickListener { finish() }
    }
}