package com.example.cropcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AddZoneActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedDateMillis: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_zone)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etZoneName = findViewById<EditText>(R.id.etZoneName)
        val etZoneArea = findViewById<EditText>(R.id.etZoneArea)
        val etPlantingDate = findViewById<EditText>(R.id.etPlantingDate)
        val btnAddZoneSubmit = findViewById<Button>(R.id.btnAddZoneSubmit)
        val btnBack = findViewById<Button>(R.id.btnBack)

        etPlantingDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                    }
                    selectedDateMillis = selectedCal.timeInMillis
                    etPlantingDate.setText("$dayOfMonth/${month + 1}/$year")
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }

        btnAddZoneSubmit.setOnClickListener {
            val zoneName = etZoneName.text.toString().trim()
            val zoneAreaStr = etZoneArea.text.toString().trim()
            val userId = auth.currentUser?.uid ?: ""

            if (zoneName.isEmpty() || zoneAreaStr.isEmpty() || selectedDateMillis == 0L) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val zoneArea = zoneAreaStr.toDoubleOrNull() ?: 0.0
            val newZoneRef = db.collection("zones").document()

            val zoneMap = hashMapOf(
                "zoneId" to newZoneRef.id,
                "userId" to userId,
                "zoneName" to zoneName,
                "zoneAreaSqm" to zoneArea,
                "dateOfPlanting" to selectedDateMillis,
                "isHarvested" to false
            )

            newZoneRef.set(zoneMap)
                .addOnSuccessListener {
                    Toast.makeText(this, "Zone Added Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to add zone: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}