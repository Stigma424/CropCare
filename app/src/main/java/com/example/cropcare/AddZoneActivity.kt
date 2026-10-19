
package com.example.cropcare

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class AddZoneActivity : AppCompatActivity() {

    private var selectedDateMillis: Long = 0L

    private lateinit var etZoneName: EditText
    private lateinit var etZoneArea: EditText
    private lateinit var etPlantingDate: EditText
    private lateinit var btnAddZoneSubmit: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_zone)

        etZoneName = findViewById(R.id.etZoneName)
        etZoneArea = findViewById(R.id.etZoneArea)
        etPlantingDate = findViewById(R.id.etPlantingDate)

        btnAddZoneSubmit =
            findViewById(R.id.btnAddZoneSubmit)

        val btnBack = findViewById<Button>(R.id.btnBack)

        // Planting date must be selected using DatePicker.
        etPlantingDate.isFocusable = false
        etPlantingDate.isClickable = true

        etPlantingDate.setOnClickListener {
            openDatePicker()
        }

        btnAddZoneSubmit.setOnClickListener {
            submitZone()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()

        val picker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->

                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, dayOfMonth, 0, 0, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                selectedDateMillis =
                    selectedCalendar.timeInMillis

                etPlantingDate.setText(
                    "$dayOfMonth/${month + 1}/$year"
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        picker.show()
    }

    private fun submitZone() {
        val zoneName =
            etZoneName.text.toString().trim()

        val zoneAreaText =
            etZoneArea.text.toString().trim()

        if (zoneName.isBlank() ||
            zoneAreaText.isBlank() ||
            selectedDateMillis == 0L
        ) {
            showMessage("Please complete all zone fields.")
            return
        }

        if (zoneName.length > 80) {
            showMessage("Zone name cannot exceed 80 characters.")
            return
        }

        val zoneArea = zoneAreaText.toDoubleOrNull()

        if (zoneArea == null ||
            !zoneArea.isFinite() ||
            zoneArea <= 0.0 ||
            zoneArea > 100_000_000.0
        ) {
            showMessage("Enter a valid zone area in square meters.")
            return
        }

        val request = CropCareApiClient.ZoneInput(
            zoneName = zoneName,
            zoneAreaSqm = zoneArea,
            dateOfPlanting = selectedDateMillis
        )

        btnAddZoneSubmit.isEnabled = false
        btnAddZoneSubmit.text = "Saving Zone..."

        CropCareApiClient.createZone(request) { result ->

            if (isFinishing || isDestroyed) {
                return@createZone
            }

            if (result.successful &&
                result.statusCode == 201
            ) {
                showMessage("Zone added successfully!")
                finish()
            } else {
                btnAddZoneSubmit.isEnabled = true
                btnAddZoneSubmit.text = "Add Zone"

                showMessage(
                    "Failed to add zone: ${result.message}"
                )
            }
        }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
