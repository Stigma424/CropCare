
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorDetailActivity : AppCompatActivity() {

    private var monitoringRequest: ApiRequestHandle? = null
    private var removeRequest: ApiRequestHandle? = null
    private var removing = false
    private var generation = 0
    private var sensorId = ""
    private var sensorName = "Sensor"
    private var deviceId = ""
    private var pairingStatus = ""

    private lateinit var lastUpdated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)

        sensorId =
            intent.getStringExtra("SENSOR_ID") ?: ""

        sensorName =
            intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

        deviceId =
            intent.getStringExtra("DEVICE_ID") ?: ""

        lastUpdated = findViewById(R.id.tvLastUpdated)

        findViewById<TextView>(
            R.id.tvSensorDetailTitle
        ).text = sensorName

        findViewById<ImageButton>(
            R.id.btnBack
        ).setOnClickListener {
            finish()
        }

        findViewById<android.view.View>(
            R.id.btnRefreshDetail
        ).setOnClickListener {
            loadMonitoring()
        }

        findViewById<TextView>(
            R.id.btnViewAllHistory
        ).setOnClickListener {

            if (sensorId.isBlank()) {
                return@setOnClickListener
            }

            startActivity(
                Intent(
                    this,
                    SensorHistoryActivity::class.java
                ).apply {
                    putExtra("SENSOR_ID", sensorId)
                    putExtra("SENSOR_NAME", sensorName)
                }
            )
        }

        findViewById<TextView>(
            R.id.btnEditSensor
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    EditSensorActivity::class.java
                ).apply {
                    putExtra("SENSOR_ID", sensorId)
                    putExtra("DEVICE_ID", deviceId)
                    putExtra("SENSOR_NAME", sensorName)
                }
            )
        }

        findViewById<TextView>(
            R.id.btnDeleteSensor
        ).setOnClickListener {

            if (removing || sensorId.isBlank()) return@setOnClickListener

            AlertDialog.Builder(this)
                .setTitle("Unpair sensor?")
                .setMessage(
                    "The API releases any authorized QR claim. Historical soil readings will not be deleted."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove") { _, _ ->

                    if (removing) return@setPositiveButton
                    removing = true
                    removeRequest = CropCareHttpClient.request("DELETE", "/api/sensors/${android.net.Uri.encode(sensorId)}") { result ->
                        removing = false

                        if (result.successful) {
                            show(if (result.data?.optBoolean("qrReusable") == true) "Unpaired. The original QR can be reused." else "Registration removed. No linked QR claim was changed.")
                            finish()
                        } else {
                            show(result.message)
                        }
                    }
                }
                .show()
        }

        listOf(R.id.btnViewHistoryN, R.id.btnViewHistoryP, R.id.btnViewHistoryK, R.id.btnViewHistoryMoisture,
            R.id.btnViewHistoryPh, R.id.btnViewHistoryTemp, R.id.btnViewHistoryEc).forEachIndexed { index, id ->
            findViewById<android.view.View>(id).setOnClickListener {
                if (sensorId.isNotBlank()) startActivity(Intent(this, SensorHistoryActivity::class.java)
                    .putExtra("SENSOR_ID", sensorId).putExtra("SENSOR_NAME", sensorName).putExtra("METRIC_INDEX", index))
            }
        }        clear("Waiting for monitoring data...")
    }

    override fun onResume() {
        super.onResume()
        loadMonitoring()
    }

    private fun loadMonitoring() {
        if (sensorId.isBlank()) {
            clear("Missing sensor ID.")
            return
        }

        lastUpdated.text =
            "Loading latest sensor measurement..."

        monitoringRequest?.cancel()
        val current = ++generation
        monitoringRequest = SensorMonitoringApi.get(sensorId) { response, error ->
            if (current != generation) return@get

            if (isFinishing || isDestroyed) {
                return@get
            }

            if (response == null) {
                clear(error ?: "Monitoring unavailable.")
                return@get
            }

            sensorName =
                response.optString("sensorName", sensorName)

            deviceId =
                response.optString("deviceId", deviceId)

            pairingStatus =
                response.optString("pairingStatus", "")

            findViewById<TextView>(
                R.id.tvSensorDetailTitle
            ).text = sensorName

            val status =
                response.optString("status", "unknown_time")

            val reading =
                response.optJSONObject("reading")

            if (reading == null) {
                clear(
                    "Awaiting first reading. " +
                            "Sensor registered but not yet reporting."
                )
                return@get
            }

            val timestampMs =
                response.optLong("lastReadingAtMs", 0L)

            val label = when (status) {
                "recent" -> "Recent reading"
                "stale" -> "No recent readings"
                "unknown_time" -> "Timestamp unavailable"
                else -> "Sensor reading"
            }

            val date =
                if (timestampMs > 0) {
                    SimpleDateFormat(
                        "MMM dd, yyyy - hh:mm:ss a",
                        Locale.US
                    ).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(Date(timestampMs))
                } else {
                    "Unknown date"
                }

            lastUpdated.text = "$label · $date"

            field(
                R.id.tvSensorN,
                R.id.tvStatusN,
                reading,
                "nitrogen",
                "%.0f mg/kg"
            )

            field(
                R.id.tvSensorP,
                R.id.tvStatusP,
                reading,
                "phosphorus",
                "%.0f mg/kg"
            )

            field(
                R.id.tvSensorK,
                R.id.tvStatusK,
                reading,
                "potassium",
                "%.0f mg/kg"
            )

            field(
                R.id.tvSensorMoisture,
                R.id.tvStatusMoisture,
                reading,
                "moisture",
                "%.1f%%"
            )

            field(
                R.id.tvSensorPh,
                R.id.tvStatusPh,
                reading,
                "ph",
                "%.1f"
            )

            field(
                R.id.tvSensorTemp,
                R.id.tvStatusTemp,
                reading,
                "temperature",
                "%.1f°C"
            )

            field(
                R.id.tvSensorEc,
                R.id.tvStatusEc,
                reading,
                "ec",
                "%.1f"
            )
        }
    }

    override fun onStop() {
        generation++; monitoringRequest?.cancel(); removeRequest?.cancel(); removing = false; super.onStop()
    }
    private fun field(
        valueId: Int,
        statusId: Int,
        reading: JSONObject,
        key: String,
        format: String
    ) {
        val number =
            if (reading.isNull(key)) {
                null
            } else {
                reading.optDouble(
                    key,
                    Double.NaN
                ).takeIf {
                    it.isFinite()
                }
            }

        findViewById<TextView>(valueId).text =
            if (number == null) {
                "—"
            } else {
                String.format(Locale.US, format, number)
            }

        // Display recorded measurements without
        // making unvalidated fertilizer assessments.
        findViewById<TextView>(statusId).text =
            if (number == null) {
                "No Data"
            } else {
                "Recorded"
            }
    }

    private fun clear(message: String) {
        lastUpdated.text = message

        val values = intArrayOf(
            R.id.tvSensorN,
            R.id.tvSensorP,
            R.id.tvSensorK,
            R.id.tvSensorMoisture,
            R.id.tvSensorPh,
            R.id.tvSensorTemp,
            R.id.tvSensorEc
        )

        val statuses = intArrayOf(
            R.id.tvStatusN,
            R.id.tvStatusP,
            R.id.tvStatusK,
            R.id.tvStatusMoisture,
            R.id.tvStatusPh,
            R.id.tvStatusTemp,
            R.id.tvStatusEc
        )

        values.forEach {
            findViewById<TextView>(it).text = "—"
        }

        statuses.forEach {
            findViewById<TextView>(it).text = "No Data"
        }
    }

    private fun show(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
