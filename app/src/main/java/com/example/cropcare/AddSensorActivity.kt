
package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

class AddSensorActivity : AppCompatActivity() {

    private lateinit var etSensorId: EditText
    private lateinit var etSensorName: EditText
    private lateinit var tvQrStatus: TextView
    private lateinit var btnScanQr: Button
    private lateinit var btnSave: Button

    private var zoneId = ""

    // Keep the claim code only in memory while pairing.
    private var scannedDeviceId: String? = null
    private var scannedClaimCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_sensor)

        zoneId = intent.getStringExtra("ZONE_ID") ?: ""

        etSensorId = findViewById(R.id.etSensorId)
        etSensorName = findViewById(R.id.etSensorName)
        tvQrStatus = findViewById(R.id.tvQrStatus)
        btnScanQr = findViewById(R.id.btnScanQr)
        btnSave = findViewById(R.id.btnSave)

        val btnCancel =
            findViewById<Button>(R.id.btnCancel)

        val btnBack =
            findViewById<ImageButton>(R.id.btnBack)

        etSensorId.isEnabled = false

        btnSave.isEnabled = false

        btnScanQr.setOnClickListener {
            startQrScanner()
        }

        btnSave.setOnClickListener {
            pairScannedSensor()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }

        if (zoneId.isBlank()) {
            showMessage("No farm zone was selected.")
            finish()
        }
    }

    private fun startQrScanner() {
        // Clear any previous QR credentials before scanning.
        scannedDeviceId = null
        scannedClaimCode = null
        etSensorId.setText("")
        btnSave.isEnabled = false
        tvQrStatus.text = "Opening QR scanner..."

        val options = GmsBarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
            .enableAutoZoom()
            .build()

        val scanner = GmsBarcodeScanning.getClient(
            this,
            options
        )

        scanner.startScan()
            .addOnSuccessListener { barcode ->
                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                val rawQrText = barcode.rawValue.orEmpty()

                processQrResult(rawQrText)
            }
            .addOnCanceledListener {
                if (!isFinishing && !isDestroyed) {
                    tvQrStatus.text = "Scan cancelled. Try again."
                }
            }
            .addOnFailureListener { error ->
                if (!isFinishing && !isDestroyed) {
                    tvQrStatus.text = "Scanner unavailable."

                    showMessage(
                        "Could not open QR scanner: " +
                                (error.message ?: "Unknown error") +
                                ". Check Google Play services."
                    )
                }
            }
    }

    private fun processQrResult(rawText: String) {
        // Exact format:
        // CROPCARE1|DEVICE_ID|32_HEX_CHARACTER_CLAIM_CODE
        //
        // Do not log or display the claim code.

        if (rawText.length > 256) {
            rejectQr()
            return
        }

        val parts = rawText.split("|")

        if (parts.size != 3 ||
            parts[0] != "CROPCARE1"
        ) {
            rejectQr()
            return
        }

        val deviceId = parts[1]
        val claimCode = parts[2].uppercase()

        val validDeviceId = Regex(
            "^[A-Za-z0-9_-]{6,128}$"
        ).matches(deviceId)

        val validClaimCode = Regex(
            "^[A-F0-9]{32}$"
        ).matches(claimCode)

        if (!validDeviceId || !validClaimCode) {
            rejectQr()
            return
        }

        scannedDeviceId = deviceId
        scannedClaimCode = claimCode

        etSensorId.setText(deviceId)

        tvQrStatus.text =
            "QR scanned. Ready to pair this device."

        btnSave.isEnabled = true
    }

    private fun rejectQr() {
        scannedDeviceId = null
        scannedClaimCode = null

        etSensorId.setText("")
        btnSave.isEnabled = false

        tvQrStatus.text = "Invalid CropCare QR code."

        showMessage(
            "This QR code does not match the CropCare sensor format."
        )
    }

    private fun pairScannedSensor() {
        val deviceId = scannedDeviceId
        val claimCode = scannedClaimCode

        val sensorName =
            etSensorName.text.toString().trim()

        if (zoneId.isBlank()) {
            showMessage("No farm zone was selected.")
            return
        }

        if (deviceId.isNullOrBlank() ||
            claimCode.isNullOrBlank()
        ) {
            showMessage("Scan a valid CropCare QR code first.")
            return
        }

        if (sensorName.isBlank() ||
            sensorName.length > 80
        ) {
            showMessage("Enter a sensor name (1–80 characters).")
            return
        }

        btnScanQr.isEnabled = false
        btnSave.isEnabled = false
        btnSave.text = "Pairing..."
        tvQrStatus.text = "Verifying QR code with CropCare API..."

        val request = CropCareApiClient.SensorPairingInput(
            deviceId = deviceId,
            claimCode = claimCode,
            sensorName = sensorName,
            zoneId = zoneId
        )

        CropCareApiClient.pairSensor(request) { result ->

            if (isFinishing || isDestroyed) {
                return@pairSensor
            }

            btnScanQr.isEnabled = true
            btnSave.text = "Pair Sensor"

            if (result.successful &&
                result.statusCode == 201
            ) {
                // Clear claim details from this screen.
                scannedClaimCode = null
                scannedDeviceId = null

                tvQrStatus.text = "Sensor paired successfully!"

                showMessage(
                    "Sensor paired! Awaiting actual readings."
                )

                finish()
            } else {
                // Keep this QR available for retry in case
                // the API request failed before reaching the server.
                btnSave.isEnabled = true

                tvQrStatus.text =
                    "Pairing unsuccessful. Check the message below."

                showMessage(
                    "Pairing failed: ${result.message}"
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
