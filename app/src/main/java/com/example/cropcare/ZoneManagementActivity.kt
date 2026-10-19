
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ZoneManagementActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
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

    private var activeRecommendation: RecommendationResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_zone_management)

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
        btnNewCornPlanted = findViewById(R.id.btnNewCornPlanted)
        btnHarvest = findViewById(R.id.btnHarvest)
        btnDeleteZone = findViewById(R.id.btnDeleteZone)

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

        db.collection("users")
            .document(uid)
            .get()
            .addOnSuccessListener { userDoc ->
                isSubscribed =
                    userDoc.getBoolean("isSubscribed") ?: false

                loadZoneFromApi()
            }
            .addOnFailureListener {
                isSubscribed = false
                loadZoneFromApi()
            }
    }

    private fun loadZoneFromApi() {
        zoneLoaded = false
        btnHarvest.isEnabled = false

        CropCareApiClient.getZone(currentZoneId) { result ->

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

            updateCornAge()
            fetchZoneAverages()
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

        AlertDialog.Builder(this)
            .setTitle("Start New Corn Planting?")
            .setMessage(
                "This starts a new crop cycle using today's date. " +
                        "The zone will no longer be marked harvested."
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Confirm") { _, _ ->

                val changes = JSONObject().apply {
                    put("dateOfPlanting", System.currentTimeMillis())
                    put("isHarvested", false)
                }

                updateZone(changes)
            }
            .show()
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
        val userId = auth.currentUser?.uid

        if (userId == null || currentZoneId.isBlank()) {
            showNoReadings()
            return
        }

        db.collection("sensors")
            .whereEqualTo("zoneId", currentZoneId)
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { sensors ->

                val deviceIds = sensors.documents
                    .mapNotNull { it.getString("deviceId") }
                    .filter { it.isNotBlank() }
                    .distinct()

                if (deviceIds.isEmpty()) {
                    showNoReadings()
                    return@addOnSuccessListener
                }

                if (deviceIds.size > 30) {
                    showNoReadings()
                    showMessage(
                        "Too many sensors for the current reading query."
                    )
                    return@addOnSuccessListener
                }

                db.collection("soil_data")
                    .whereIn("deviceId", deviceIds)
                    .get()
                    .addOnSuccessListener { readings ->

                        if (readings.isEmpty) {
                            showNoReadings()
                            return@addOnSuccessListener
                        }

                        val latestDocuments = readings.documents
                            .groupBy {
                                it.getString("deviceId") ?: ""
                            }
                            .mapNotNull { (_, documents) ->
                                documents.maxByOrNull {
                                    parseAnyDate(it)?.time ?: 0L
                                }
                            }

                        processReadings(latestDocuments)
                    }
                    .addOnFailureListener { error ->
                        Log.e("CropCareZone", "Reading query failed", error)
                        showNoReadings()
                    }
            }
            .addOnFailureListener { error ->
                Log.e("CropCareZone", "Sensor query failed", error)
                showNoReadings()
            }
    }

    private fun processReadings(
        readings: List<DocumentSnapshot>
    ) {
        if (readings.isEmpty()) {
            showNoReadings()
            return
        }

        val count = readings.size.toDouble()

        val newestDate = readings
            .mapNotNull { parseAnyDate(it) }
            .maxByOrNull { it.time }

        val n = readings.sumOf {
            getDoubleValue(it, "nitrogen", "n", "N", "nitro")
        } / count

        val p = readings.sumOf {
            getDoubleValue(it, "phosphorus", "p", "P", "phos")
        } / count

        val k = readings.sumOf {
            getDoubleValue(it, "potassium", "k", "K", "pot")
        } / count

        val m = readings.sumOf {
            getDoubleValue(it, "moisture", "humidity", "mois")
        } / count

        val ph = readings.sumOf {
            getDoubleValue(it, "ph", "pH", "PH")
        } / count

        val temp = readings.sumOf {
            getDoubleValue(it, "temperature", "temp")
        } / count

        val ec = readings.sumOf {
            getDoubleValue(it, "ec", "EC")
        } / count

        updateUiWithAverages(
            n, p, k, m, ph, temp, ec, newestDate
        )
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
        tvSoilHealth.text = "Soil Health: No readings yet"
        tvRecommendationText.text =
            "Waiting for actual soil readings."

        activeRecommendation = null
        btnSeeMoreRec.isEnabled = false
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
        tvAvgN.text = String.format(Locale.US, "%.0f mg/kg", n)
        tvStatusN.text = getParameterStatus("N", n)

        tvAvgP.text = String.format(Locale.US, "%.0f mg/kg", p)
        tvStatusP.text = getParameterStatus("P", p)

        tvAvgK.text = String.format(Locale.US, "%.0f mg/kg", k)
        tvStatusK.text = getParameterStatus("K", k)

        tvAvgMoisture.text =
            String.format(Locale.US, "%.0f%%", m)
        tvStatusMoisture.text =
            getParameterStatus("MOISTURE", m)

        tvAvgPh.text = String.format(Locale.US, "%.1f", ph)
        tvStatusPh.text = getParameterStatus("PH", ph)

        tvAvgTemp.text =
            String.format(Locale.US, "%.0f°C", temp)
        tvStatusTemp.text =
            getParameterStatus("TEMP", temp)

        tvAvgEc.text =
            String.format(Locale.US, "%.0f us/cm", ec)
        tvStatusEc.text =
            getParameterStatus("EC", ec)

        val score = calculateOverallSoilHealth(
            n, p, k, m, ph
        )

        tvSoilHealth.text = "Soil Health: $score"

        tvLastUpdated.text = if (latestDate != null) {
            val formatter = SimpleDateFormat(
                "MMM dd, yyyy - hh:mm:ss a",
                Locale.US
            )
            "Last updated: ${formatter.format(latestDate)}"
        } else {
            "Last updated: Unknown"
        }

        // Existing recommendation system is unchanged.
        val days = if (dateOfPlanting > 0L) {
            TimeUnit.MILLISECONDS.toDays(
                (System.currentTimeMillis() -
                        dateOfPlanting).coerceAtLeast(0L)
            )
        } else {
            0L
        }

        activeRecommendation =
            CornRecommendationEngine.generateRecommendation(
                n = n,
                p = p,
                k = k,
                ph = ph,
                moisture = m,
                cornAgeDays = days,
                isHarvested = isHarvested,
                zoneAreaSqm = zoneAreaSqm,
                isSubscribed = isSubscribed
            )

        tvRecommendationText.text =
            activeRecommendation?.summaryText
                ?: "No recommendation available."

        btnSeeMoreRec.isEnabled =
            activeRecommendation != null

        NotificationHelper.checkAndTriggerSoilAlerts(
            context = this,
            zoneName = tvManageZoneName.text.toString(),
            n = n,
            p = p,
            k = k,
            moisture = m,
            ph = ph,
            recommendationSummary =
                activeRecommendation?.summaryText ?: ""
        )
    }

    private fun showDetailedRecommendationDialog() {
        val recommendation = activeRecommendation ?: return

        val builder = AlertDialog.Builder(this)
            .setTitle(recommendation.stageName)
            .setMessage(recommendation.detailedAdvice)
            .setPositiveButton("OK", null)

        if (recommendation.isPremiumLocked) {
            builder.setNeutralButton("Subscribe to Pro") { _, _ ->
                startActivity(
                    Intent(this, SubscriptionActivity::class.java)
                )
            }
        }

        builder.show()
    }

    private fun getParameterStatus(
        type: String,
        value: Double
    ): String {
        if (value <= 0.0) return "No Data"

        return try {
            SoilUtils.getStatus(type, value)
        } catch (_: Exception) {
            "Unknown"
        }
    }

    private fun calculateOverallSoilHealth(
        n: Double,
        p: Double,
        k: Double,
        m: Double,
        ph: Double
    ): String {
        var score = 0

        if (n >= 20) score += 20
        if (p >= 10) score += 20
        if (k >= 100) score += 20
        if (m in 40.0..80.0) score += 20
        if (ph in 5.5..7.5) score += 20

        return when {
            score >= 80 -> "Optimal ($score/100)"
            score >= 40 -> "Fair ($score/100)"
            else -> "Poor ($score/100)"
        }
    }

    private fun getDoubleValue(
        doc: DocumentSnapshot,
        vararg keys: String
    ): Double {
        for (key in keys) {
            doc.getDouble(key)?.let { return it }
            doc.getLong(key)?.let { return it.toDouble() }
            doc.getString(key)?.toDoubleOrNull()?.let { return it }
        }

        return 0.0
    }

    private fun parseAnyDate(
        doc: DocumentSnapshot
    ): Date? {
        val keys = arrayOf(
            "timestamp",
            "lastScanTimestamp",
            "createdAt",
            "date",
            "updatedAt"
        )

        for (key in keys) {
            val raw = doc.get(key) ?: continue

            when (raw) {
                is Timestamp -> return raw.toDate()
                is Long -> return Date(raw)
                is Double -> return Date(raw.toLong())

                is String -> {
                    val formats = arrayOf(
                        "yyyy-MM-dd HH:mm:ss",
                        "MMMM dd, yyyy 'at' h:mm:ss a z",
                        "MMMM dd, yyyy - hh:mm:ss a"
                    )

                    for (format in formats) {
                        try {
                            val date = SimpleDateFormat(
                                format,
                                Locale.US
                            ).parse(raw)

                            if (date != null) return date
                        } catch (_: Exception) {
                        }
                    }
                }
            }
        }

        return null
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_LONG
        ).show()
    }
}
