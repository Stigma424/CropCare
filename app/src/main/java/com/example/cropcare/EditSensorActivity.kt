
package com.example.cropcare

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EditSensorActivity : AppCompatActivity() {

    private lateinit var etSensorId: EditText
    private lateinit var etSensorName: EditText
    private lateinit var btnSave: TextView

    private var sensorId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_sensor)

        sensorId = intent.getStringExtra("SENSOR_ID") ?: ""

        etSensorId = findViewById(R.id.etEditSensorId)
        etSensorName = findViewById(R.id.etEditSensorName)

        btnSave = findViewById(R.id.btnSave)

        val btnCancel =
            findViewById<TextView>(R.id.btnCancel)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        etSensorId.isEnabled = false
        etSensorId.isFocusable = false

        btnSave.isEnabled = false

        btnCancel.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnSave.setOnClickListener {
            saveSensorName()
        }

        if (sensorId.isBlank()) {
            showMessage("Missing sensor ID.")
            finish()
            return
        }

        loadSensor()
    }

    private fun loadSensor() {
        CropCareApiClient.getSensor(sensorId) { result ->

            if (isFinishing || isDestroyed) {
                return@getSensor
            }

            if (result.successful && result.data != null) {

                etSensorId.setText(
                    result.data.optString("deviceId", "")
                )

                etSensorName.setText(
                    result.data.optString("sensorName", "")
                )

                btnSave.isEnabled = true
            } else {
                showMessage("Could not load sensor: ${result.message}")
            }
        }
    }

    private fun saveSensorName() {
        val newName = etSensorName.text.toString().trim()

        if (newName.isBlank() || newName.length > 80) {
            showMessage("Enter a sensor name (1–80 characters).")
            return
        }

        btnSave.isEnabled = false

        CropCareApiClient.renameSensor(
            sensorId = sensorId,
            newName = newName
        ) { result ->

            if (isFinishing || isDestroyed) {
                return@renameSensor
            }

            if (result.successful) {
                showMessage("Sensor renamed successfully!")
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
