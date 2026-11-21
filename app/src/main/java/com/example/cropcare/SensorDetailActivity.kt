
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorDetailActivity : AppCompatActivity() {

    private var monitoringRequest: ApiRequestHandle? = null
    private var generation = 0
    private var readingIsRecent = false
    private var sensorId = ""
    private var sensorName = "Sensor"

    private lateinit var lastUpdated: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_detail)
        ReadingStatus.watch(findViewById(android.R.id.content))

        sensorId =
            intent.getStringExtra("SENSOR_ID") ?: ""

        sensorName =
            intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

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
            loadAssessment()
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

        findViewById<android.view.View>(R.id.btnHyb1).setOnClickListener { program = "HYB-1"; loadAssessment() }
        findViewById<android.view.View>(R.id.btnHyb2).setOnClickListener { program = "HYB-2"; loadAssessment() }
        findViewById<android.view.View>(R.id.btnSensorDemoPlan).setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }
        listOf(R.id.btnViewHistoryN, R.id.btnViewHistoryP, R.id.btnViewHistoryK, R.id.btnViewHistoryMoisture,
            R.id.btnViewHistoryPh, R.id.btnViewHistoryTemp, R.id.btnViewHistoryEc).forEachIndexed { index, id ->
            findViewById<android.view.View>(id).setOnClickListener {
                if (sensorId.isNotBlank()) startActivity(Intent(this, SensorHistoryActivity::class.java)
                    .putExtra("SENSOR_ID", sensorId).putExtra("SENSOR_NAME", sensorName).putExtra("METRIC_INDEX", index))
            }
        }        //clear("Waiting for monitoring data...")
    }

    override fun onResume() {
        super.onResume()
        loadMonitoring()
        loadAssessment()
        loadZone()
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

            findViewById<TextView>(
                R.id.tvSensorDetailTitle
            ).text = sensorName

            val status =
                response.optString("status", "unknown_time")

            readingIsRecent = status == "recent"
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
                "%.1f µS/cm"
            )
        }
    }

    override fun onStop() {
        generation++; monitoringRequest?.cancel(); assessmentRequest?.cancel(); zoneRequest?.cancel(); super.onStop()
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
                if (readingIsRecent) ReadingStatus.label(key, number) else "Check freshness"
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

    private var program = "HYB-1"
    private var assessmentRequest: ApiRequestHandle? = null
    private var zoneRequest: ApiRequestHandle? = null

    private fun loadZone() {
        val label = findViewById<TextView>(R.id.tvAssignedZone)
        if (sensorId.isBlank()) return
        zoneRequest?.cancel()
        zoneRequest = CropCareHttpClient.request("GET", "/api/sensors/${android.net.Uri.encode(sensorId)}") { result ->
            val zoneId = result.data?.optString("zoneId").orEmpty()
            if (!result.successful || zoneId.isBlank()) {
                label.text = if (result.successful) "No assigned zone" else "Assigned zone unavailable"
                return@request
            }
            zoneRequest = CropCareHttpClient.request("GET", "/api/zones/${android.net.Uri.encode(zoneId)}") { zone ->
                label.text = zone.data?.optString("zoneName")?.takeIf { it.isNotBlank() } ?: "Assigned zone unavailable"
            }
        }
    }

    private fun loadAssessment() {
        if (sensorId.isBlank()) return
        assessmentRequest?.cancel()
        val requestedProgram = program
        val content = findViewById<TextView>(R.id.tvSensorAssessment)
        val subscribe = findViewById<android.view.View>(R.id.btnSensorDemoPlan)
        findViewById<android.view.View>(R.id.btnHyb1).isEnabled = program != "HYB-1"
        findViewById<android.view.View>(R.id.btnHyb2).isEnabled = program != "HYB-2"
        subscribe.visibility = android.view.View.GONE
        content.text = "Loading $program recommendations…"
        val path = "/api/sensors/${android.net.Uri.encode(sensorId)}/assessment?programId=$program"
        assessmentRequest = CropCareHttpClient.request("GET", path) { result ->
            if (program != requestedProgram) return@request
            val data = result.data
            if (!result.successful || data == null) {
                content.text = result.message
                if (result.statusCode == 403) subscribe.visibility = android.view.View.VISIBLE
                return@request
            }
            val assessment = runCatching { FarmerAssessment.parse(data) }.getOrNull()
            if (assessment == null) {
                content.text = "Sensor recommendations are unavailable."
                return@request
            }
            content.text = buildString {
                appendLine("Soil health score: ${assessment.score?.let { "$it/100" } ?: "Unavailable"} · ${assessment.label}")
                val quality = data.optJSONArray("sensorQuality")?.optJSONObject(0)
                quality?.optString("message")?.takeIf { it.isNotBlank() }?.let { appendLine(it) }
                val actions = data.optJSONArray("recommendations")
                for (index in 0 until (actions?.length() ?: 0)) {
                    val action = actions?.optJSONObject(index) ?: continue
                    appendLine()
                    appendLine(action.optString("title"))
                    appendLine(action.optString("whatToDo"))
                    appendLine(action.optString("why"))
                }
                appendLine()
                appendLine("Fertilizer reference plan · $requestedProgram")
                val area = data.optDouble("sensorAssignedAreaSqm")
                if (area.isFinite()) appendLine(String.format(Locale.US, "Published rates scaled to %.2f m² assigned to this sensor.", area))
                appendLine("Account for prior applications and confirm crop stage before applying fertilizer.")
                val entries = assessment.displayPlan?.optJSONArray("entries")
                for (index in 0 until (entries?.length() ?: 0)) {
                    val entry = entries?.optJSONObject(index) ?: continue
                    appendLine()
                    appendLine(String.format(Locale.US, "%s (%s) · %.2f kg", entry.optString("product"),
                        entry.optString("fertilizerGrade"), entry.optDouble("kgForPlot")))
                    appendLine(entry.optString("applicationTiming"))
                }
            }.trimEnd()
        }
    }
}
