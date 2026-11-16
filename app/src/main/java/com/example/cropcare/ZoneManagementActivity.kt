
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ZoneManagementActivity : AppCompatActivity() {

    private var monitoringRequest: MonitoringRequest? = null
    private var subscriptionRequest: ApiRequestHandle? = null
    private var refreshGeneration = 0
    private val auth = FirebaseAuth.getInstance()

    private var currentZoneId = ""
    private var zoneAreaSqm = 0.0
    private var dateOfPlanting = 0L
    private var isHarvested = false
    private var isSubscribed = false
    private var zoneUpdateInProgress = false
    private var zoneLoaded = false

    private lateinit var tvManageZoneName: TextView
    private lateinit var tvLastUpdated: TextView
    private lateinit var tvCornAge: TextView
    private lateinit var tvSoilHealth: TextView

    private lateinit var tvAvgN: TextView
    private lateinit var tvStatusN: TextView
    private lateinit var tvAvgP: TextView
    private lateinit var tvStatusP: TextView
    private lateinit var tvAvgK: TextView
    private lateinit var tvStatusK: TextView

    private lateinit var tvAvgMoisture: TextView
    private lateinit var tvStatusMoisture: TextView
    private lateinit var tvAvgPh: TextView
    private lateinit var tvStatusPh: TextView
    private lateinit var tvAvgTemp: TextView
    private lateinit var tvStatusTemp: TextView
    private lateinit var tvAvgEc: TextView
    private lateinit var tvStatusEc: TextView

    private lateinit var tvRecommendationText: TextView
    private lateinit var btnSeeMoreRec: Button
    private lateinit var btnNewCornPlanted: Button
    private lateinit var btnHarvest: Button
    private lateinit var btnDeleteZone: Button

    private var activeAssessment: FarmerAssessment? = null
    private var assessmentRequest: ApiRequestHandle? = null
    private var assessmentProgram = "HYB-1"
    private var detailView: RecommendationView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone_management)
        ReadingStatus.watch(findViewById(android.R.id.content))

        currentZoneId = intent.getStringExtra("ZONE_ID") ?: ""

        tvManageZoneName = findViewById(R.id.tvManageZoneName)
        tvLastUpdated = findViewById(R.id.tvLastUpdated)
        tvCornAge = findViewById(R.id.tvCornAge)
        tvSoilHealth = findViewById(R.id.tvSoilHealth)

        tvAvgN = findViewById(R.id.tvAvgN)
        tvStatusN = findViewById(R.id.tvStatusN)
        tvAvgP = findViewById(R.id.tvAvgP)
        tvStatusP = findViewById(R.id.tvStatusP)
        tvAvgK = findViewById(R.id.tvAvgK)
        tvStatusK = findViewById(R.id.tvStatusK)

        tvAvgMoisture = findViewById(R.id.tvAvgMoisture)
        tvStatusMoisture = findViewById(R.id.tvStatusMoisture)
        tvAvgPh = findViewById(R.id.tvAvgPh)
        tvStatusPh = findViewById(R.id.tvStatusPh)
        tvAvgTemp = findViewById(R.id.tvAvgTemp)
        tvStatusTemp = findViewById(R.id.tvStatusTemp)
        tvAvgEc = findViewById(R.id.tvAvgEc)
        tvStatusEc = findViewById(R.id.tvStatusEc)

        tvRecommendationText =
            findViewById(R.id.tvRecommendationText)

        btnSeeMoreRec = findViewById(R.id.btnSeeMoreRec)
        btnSeeMoreRec.isEnabled = false
        btnNewCornPlanted = findViewById(R.id.btnNewCornPlanted)
        btnHarvest = findViewById(R.id.btnHarvest)
        btnDeleteZone = findViewById(R.id.btnDeleteZone)
        showNoReadings()
        tvManageZoneName.text = "Loading zone…"
        tvLastUpdated.text = "Loading measurements…"
        findViewById<android.view.View>(R.id.btnEditZone).setOnClickListener { editZone() }

        // Use one visible button for both crop actions.
        // The old top button has a height of 1dp in XML.
        btnNewCornPlanted.visibility = View.GONE

        btnHarvest.text = "Loading..."
        btnHarvest.isEnabled = false

        btnHarvest.setOnClickListener {
            if (!zoneLoaded || zoneUpdateInProgress) {
                return@setOnClickListener
            }

            if (isHarvested) {
                confirmNewPlanting()
            } else {
                confirmHarvest()
            }
        }

        btnSeeMoreRec.setOnClickListener {
            showDetailedRecommendationDialog()
        }

        findViewById<Button>(R.id.btnRefresh)
            .setOnClickListener {
                loadZoneData()
            }

        findViewById<Button>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        findViewById<Button>(R.id.btnSensorStatus)
            .setOnClickListener {
                startActivity(
                    Intent(this, SensorStatusActivity::class.java)
                        .putExtra("ZONE_ID", currentZoneId)
                )
            }

        btnDeleteZone.setOnClickListener {
            confirmDeleteZone()
        }

        if (currentZoneId.isBlank()) {
            showMessage("Missing farm zone.")
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        if (currentZoneId.isNotBlank()) {
            loadZoneData()
        }
    }

    private fun loadZoneData() {
        val uid = auth.currentUser?.uid

        if (uid == null) {
            showMessage("Please log in again.")
            finish()
            return
        }

        monitoringRequest?.cancel()
        refreshGeneration++
        subscriptionRequest?.cancel()
        val subscriptionGeneration = refreshGeneration
        subscriptionRequest = CropCareFarmerApi.subscription { result ->
            if (subscriptionGeneration != refreshGeneration || uid != auth.currentUser?.uid) return@subscription
            isSubscribed = result.successful && result.data?.optBoolean("isSubscribed") == true
            loadZoneFromApi()
        }
    }
    private fun loadZoneFromApi() {
        zoneLoaded = false
        btnHarvest.isEnabled = false
        findViewById<View>(R.id.btnEditZone).isEnabled = false

        val currentGeneration = ++refreshGeneration
        CropCareApiClient.getZone(currentZoneId) { result ->
            if (currentGeneration != refreshGeneration) return@getZone

            if (isFinishing || isDestroyed) {
                return@getZone
            }

            if (!result.successful || result.data == null) {
                btnHarvest.text = "Zone Unavailable"

                showMessage(
                    "Unable to load zone: ${result.message}"
                )
                return@getZone
            }

            val zone = result.data

            tvManageZoneName.text =
                zone.optString("zoneName", "Zone Details")

            zoneAreaSqm = zone.optDouble("zoneAreaSqm", 0.0)
            dateOfPlanting = zone.optLong("dateOfPlanting", 0L)
            isHarvested = zone.optBoolean("isHarvested", false)

            zoneLoaded = true
            findViewById<View>(R.id.btnEditZone).isEnabled = true

            updateCornAge()
            fetchZoneAverages()
            fetchAssessment()
        }
    }

    private fun updateCornAge() {

        // Always use this bottom button for the next action.
        btnNewCornPlanted.visibility = View.GONE
        btnHarvest.isEnabled =
            zoneLoaded && !zoneUpdateInProgress

        if (isHarvested) {
            tvCornAge.text = "Harvested"

            // FIX: The button changes its label and action.
            btnHarvest.text = "New Corn Planted"

        } else if (dateOfPlanting > 0L) {
            val elapsed =
                System.currentTimeMillis() - dateOfPlanting

            val days = TimeUnit.MILLISECONDS.toDays(
                elapsed.coerceAtLeast(0L)
            )

            tvCornAge.text = "$days days after planting"

            btnHarvest.text = "Mark Harvested"

        } else {
            tvCornAge.text = "No planting date recorded"
            btnHarvest.text = "Mark Harvested"
        }
    }

    private fun confirmHarvest() {
        if (zoneUpdateInProgress) return

        AlertDialog.Builder(this)
            .setTitle("Mark Zone as Harvested?")
            .setMessage(
                "This marks the current corn crop as harvested. " +
                        "You can start a new planting afterward."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                val changes = JSONObject().apply {
                    put("isHarvested", true)
                }

                updateZone(changes)
            }
            .show()
    }

    private fun confirmNewPlanting() {
        if (zoneUpdateInProgress) return

        val today = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila"))
        android.app.DatePickerDialog(this, { _, year, month, day ->
            val selected = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila")).apply {
                clear(); set(year, month, day, 0, 0, 0)
            }.timeInMillis
            if (selected <= 0L || selected >= 4_102_444_800_000L) { showMessage("Choose a date between 1970 and 2099."); return@DatePickerDialog }
            AlertDialog.Builder(this).setTitle("Start a new planting cycle?")
                .setMessage("Use the selected planting date? Historical readings are retained.")
                .setNegativeButton("Cancel", null).setPositiveButton("Confirm") { _, _ ->
                    updateZone(JSONObject().put("dateOfPlanting", selected).put("isHarvested", false))
                }.show()
        }, today.get(java.util.Calendar.YEAR), today.get(java.util.Calendar.MONTH), today.get(java.util.Calendar.DAY_OF_MONTH)).show()
    }
    private fun updateZone(changes: JSONObject) {
        if (zoneUpdateInProgress) return

        zoneUpdateInProgress = true
        btnHarvest.isEnabled = false
        btnHarvest.text = "Saving..."

        CropCareApiClient.updateZone(
            currentZoneId,
            changes
        ) { result ->

            if (isFinishing || isDestroyed) {
                return@updateZone
            }

            zoneUpdateInProgress = false

            if (result.successful) {

                // Update the displayed state immediately.
                if (changes.has("isHarvested")) {
                    isHarvested =
                        changes.optBoolean("isHarvested")
                }

                if (changes.has("dateOfPlanting")) {
                    dateOfPlanting =
                        changes.optLong("dateOfPlanting")
                }

                updateCornAge()
                showMessage("Zone updated successfully.")

                // Refresh the authoritative state from the API.
                loadZoneData()

            } else {
                updateCornAge()
                showMessage(
                    "Could not update zone: ${result.message}"
                )
            }
        }
    }

    private fun confirmDeleteZone() {
        AlertDialog.Builder(this)
            .setTitle("Delete Farm Zone?")
            .setMessage(
                "You must remove its registered sensors first. " +
                        "Historical soil readings will not be deleted."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                deleteZone()
            }
            .show()
    }

    private fun deleteZone() {
        btnDeleteZone.isEnabled = false

        CropCareApiClient.deleteZone(currentZoneId) { result ->

            if (isFinishing || isDestroyed) {
                return@deleteZone
            }

            if (result.successful) {
                showMessage("Zone deleted successfully.")
                finish()

            } else {
                btnDeleteZone.isEnabled = true

                if (result.statusCode == 409) {
                    showMessage(
                        "Remove the registered sensors " +
                                "before deleting this zone."
                    )
                } else {
                    showMessage("Delete failed: ${result.message}")
                }
            }
        }
    }

    private fun fetchZoneAverages() {
        monitoringRequest?.cancel()
        val zone = ZoneModel(currentZoneId, tvManageZoneName.text.toString(), zoneAreaSqm, dateOfPlanting, isHarvested)
        monitoringRequest = ZoneMonitoringRepository.zone(zone) { snapshot, error ->
            if (snapshot == null) {
                showNoReadings(); tvLastUpdated.text = error ?: "Monitoring unavailable"; return@zone
            }
            val a = snapshot.averages
            if (soilKeys.all { a[it] != null }) {
                updateUiWithAverages(a.getValue("nitrogen")!!, a.getValue("phosphorus")!!,
                    a.getValue("potassium")!!, a.getValue("moisture")!!, a.getValue("ph")!!,
                    a.getValue("temperature")!!, a.getValue("ec")!!,
                    snapshot.newestAtMs.takeIf { it > 0 }?.let { Date(it) })
            } else {
                showNoReadings()
                val valueIds = listOf(R.id.tvAvgN, R.id.tvAvgP, R.id.tvAvgK, R.id.tvAvgMoisture, R.id.tvAvgPh, R.id.tvAvgTemp, R.id.tvAvgEc)
                val statusIds = listOf(R.id.tvStatusN, R.id.tvStatusP, R.id.tvStatusK, R.id.tvStatusMoisture, R.id.tvStatusPh, R.id.tvStatusTemp, R.id.tvStatusEc)
                val units = listOf("mg/kg", "mg/kg", "mg/kg", "%", "", "°C", "us/cm")
                soilKeys.forEachIndexed { index, key ->
                    val value = a[key]
                    findViewById<TextView>(valueIds[index]).text = value?.let { String.format(Locale.US, "%.1f %s", it, units[index]) } ?: "—"
                    findViewById<TextView>(statusIds[index]).text = ReadingStatus.label(key, value)
                }
                if (activeAssessment == null) tvRecommendationText.text = "Waiting for the server assessment; no local recommendation is calculated."
            }
            tvLastUpdated.append("\n" + snapshot.notice)
        }
    }

    private fun editZone() {
        if (!zoneLoaded || zoneUpdateInProgress) return
        val form = layoutInflater.inflate(R.layout.dialog_edit_zone, null)
        val name = form.findViewById<android.widget.EditText>(R.id.etEditZoneName)
        val area = form.findViewById<android.widget.EditText>(R.id.etEditZoneArea)
        name.setText(tvManageZoneName.text); area.setText(zoneAreaSqm.toString())
        val dialog = AlertDialog.Builder(this).setTitle("Edit farm zone").setView(form)
            .setNegativeButton("Cancel", null).setPositiveButton("Save", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = area.text.toString().toDoubleOrNull()
                val title = name.text.toString().trim()
                if (title.length !in 1..80) { name.error = "Enter 1–80 characters"; return@setOnClickListener }
                if (value == null || !value.isFinite() || value <= 0 || value > 100_000_000) {
                    area.error = "Enter a valid area in square meters"; return@setOnClickListener
                }
                updateZone(JSONObject().put("zoneName", title).put("zoneAreaSqm", value)); dialog.dismiss()
            }
        }
        dialog.show()
    }

    override fun onStop() {
        refreshGeneration++; monitoringRequest?.cancel(); subscriptionRequest?.cancel(); assessmentRequest?.cancel(); super.onStop()
    }
    private fun showNoReadings() {
        tvAvgN.text = "—"
        tvAvgP.text = "—"
        tvAvgK.text = "—"
        tvAvgMoisture.text = "—"
        tvAvgPh.text = "—"
        tvAvgTemp.text = "—"
        tvAvgEc.text = "—"

        tvStatusN.text = "No Data"
        tvStatusP.text = "No Data"
        tvStatusK.text = "No Data"
        tvStatusMoisture.text = "No Data"
        tvStatusPh.text = "No Data"
        tvStatusTemp.text = "No Data"
        tvStatusEc.text = "No Data"

        tvLastUpdated.text = "Last updated: No data"
        if (activeAssessment == null) tvSoilHealth.text = "Server assessment unavailable"
        if (activeAssessment == null) tvRecommendationText.text = "Server assessment pending."

        // Monitoring errors never replace a server assessment with a local calculation.
        btnSeeMoreRec.isEnabled = activeAssessment != null
    }

    private fun updateUiWithAverages(
        n: Double,
        p: Double,
        k: Double,
        m: Double,
        ph: Double,
        temp: Double,
        ec: Double,
        latestDate: Date?
    ) {
        tvAvgN.text = String.format(Locale.US, "%.1f mg/kg", n)
        tvStatusN.text = getParameterStatus("N", n)

        tvAvgP.text = String.format(Locale.US, "%.1f mg/kg", p)
        tvStatusP.text = getParameterStatus("P", p)

        tvAvgK.text = String.format(Locale.US, "%.1f mg/kg", k)
        tvStatusK.text = getParameterStatus("K", k)

        tvAvgMoisture.text =
            String.format(Locale.US, "%.1f%%", m)
        tvStatusMoisture.text =
            getParameterStatus("MOISTURE", m)

        tvAvgPh.text = String.format(Locale.US, "%.1f", ph)
        tvStatusPh.text = getParameterStatus("PH", ph)

        tvAvgTemp.text =
            String.format(Locale.US, "%.1f°C", temp)
        tvStatusTemp.text =
            getParameterStatus("TEMP", temp)

        tvAvgEc.text =
            String.format(Locale.US, "%.1f us/cm", ec)
        tvStatusEc.text =
            getParameterStatus("EC", ec)

        tvLastUpdated.text = latestDate?.let { "Recorded: " + SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.US).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(it) } ?: "Reading time unavailable"
        if (activeAssessment == null) tvSoilHealth.text = "Server assessment loading or unavailable"
    }

    private fun fetchAssessment() {
        assessmentRequest?.cancel()
        val generation = refreshGeneration
        val owner = auth.currentUser?.uid
        activeAssessment = null; btnSeeMoreRec.isEnabled = false
        findViewById<android.widget.LinearLayout>(R.id.recommendationSummaryCards).apply { removeAllViews(); visibility = android.view.View.GONE }
        tvRecommendationText.visibility = android.view.View.VISIBLE
        tvRecommendationText.text = "Loading server advisory…"
        tvSoilHealth.text = "Assessment loading…"
        detailView?.showMessage("Loading current recommendation…")
        assessmentRequest = CropCareFarmerApi.assessment(currentZoneId, assessmentProgram) { result ->
            if (generation != refreshGeneration || owner != auth.currentUser?.uid || isFinishing || isDestroyed) return@assessment
            val assessment = result.data?.takeIf { result.successful }?.let { runCatching { FarmerAssessment.parse(it) }.getOrNull() }
            activeAssessment = assessment
            btnSeeMoreRec.isEnabled = assessment != null
            if (assessment != null) {
                tvSoilHealth.text = "Soil health score: " + (assessment.score?.let { "$it/100" } ?: "Unavailable")
                tvRecommendationText.visibility = android.view.View.GONE
                findViewById<android.widget.LinearLayout>(R.id.recommendationSummaryCards).apply {
                    removeAllViews(); visibility = android.view.View.VISIBLE
                    addView(RecommendationView(this@ZoneManagementActivity) { }.apply { renderSummary(assessment) })
                }
                detailView?.render(assessment)
            } else {
                val message = when(result.statusCode) { 403 -> "An approved Pro subscription is required for detailed recommendations."; 401 -> "Session expired. Sign in again."; else -> if (result.successful) "API assessment update required" else result.message }
                tvSoilHealth.text = "Assessment unavailable"
                tvRecommendationText.text = message; detailView?.showMessage(message)
            }
        }
    }
    private fun showDetailedRecommendationDialog() {
        val assessment = activeAssessment ?: return
        val content = RecommendationView(this) { program ->
            if (program != assessmentProgram) { assessmentProgram = program; fetchAssessment() }
        }.apply { render(assessment) }
        detailView = content
        val scroll = android.widget.ScrollView(this).apply { addView(content) }
        val dialog = AlertDialog.Builder(this).setTitle("Zone recommendation").setView(scroll).setPositiveButton("Close", null).create()
        dialog.setOnDismissListener { detailView = null }
        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(android.graphics.Color.rgb(29, 78, 43))
    }
    private fun getParameterStatus(type: String, value: Double): String = ReadingStatus.label(type, value)

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}


