
package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AddSensorActivity : AppCompatActivity() {

    private lateinit var btnSave: Button
    private lateinit var etSensorId: EditText
    private lateinit var etSensorName: EditText

    private var zoneId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        etSensorId = findViewById(R.id.etSensorId)
        etSensorName = findViewById(R.id.etSensorName)
        btnSave = findViewById(R.id.btnSave)

        val btnCancel =
            findViewById<Button>(R.id.btnCancel)

        val btnBack =
            findViewById<ImageButton>(R.id.btnBack)

        btnSave.setOnClickListener {
            submitSensor()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun submitSensor() {
        val deviceId = etSensorId.text.toString().trim()
        val sensorName = etSensorName.text.toString().trim()

        if (zoneId.isBlank()) {
            showMessage("Please select a farm zone.")
            return
        }

        if (deviceId.isBlank() || sensorName.isBlank()) {
            showMessage("Enter Device ID and Sensor Name.")
            return
        }

        if (deviceId.length > 128 || sensorName.length > 80) {
            showMessage("Device ID or Sensor Name is too long.")
            return
        }

        btnSave.isEnabled = false

        val sensor = CropCareApiClient.SensorInput(
            deviceId = deviceId,
            sensorName = sensorName,
            zoneId = zoneId
        )

        CropCareApiClient.registerSensor(sensor) { result ->

            if (isFinishing || isDestroyed) {
                return@registerSensor
            }

            if (result.successful &&
                result.statusCode == 201
            ) {
                showMessage(
                    "Sensor registered. Awaiting verification and readings."
                )
                finish()
            } else {
                btnSave.isEnabled = true
                showMessage(result.message)
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
