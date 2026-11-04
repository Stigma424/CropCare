package com.example.cropcare

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SensorHistoryActivity : AppCompatActivity() {

    private val readings = mutableListOf<SensorHistoryReading>()
    private lateinit var adapter: SensorHistoryAdapter
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
    private lateinit var tvNotice: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var progress: ProgressBar
    private lateinit var btnRefresh: Button
    private lateinit var btnLoadMore: Button

    private val visibleReadings = mutableListOf<SensorHistoryReading>()
    private var selectedDay: String? = null
    private var selectedMetric = 0
    private var historyRequest: ApiRequestHandle? = null
    private var resumeNeeded = false
    private var sensorId = ""
    private var sensorName = "Sensor"
    private var nextCursor: String? = null
    private var loading = false
    private var requestGeneration = 0
    private var initialLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sensor_history)

        sensorId = intent.getStringExtra("SENSOR_ID").orEmpty()
        sensorName = intent.getStringExtra("SENSOR_NAME") ?: "Sensor"

        tvTitle = findViewById(R.id.tvHistoryTitle)
        tvSubtitle = findViewById(R.id.tvHistorySubtitle)
        tvNotice = findViewById(R.id.tvHistoryNotice)
        tvEmpty = findViewById(R.id.tvHistoryEmpty)
        progress = findViewById(R.id.progressHistory)
        btnRefresh = findViewById(R.id.btnRefreshHistory)
        btnLoadMore = findViewById(R.id.btnLoadMore)

        tvTitle.text = "$sensorName history"

        val recycler = findViewById<RecyclerView>(R.id.rvHistory)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = SensorHistoryAdapter(visibleReadings)
        recycler.adapter = adapter
        val tools = findViewById<View>(R.id.historyTools)
        tools.visibility = if (savedInstanceState?.getBoolean("toolsVisible") == true || intent.hasExtra("METRIC_INDEX")) View.VISIBLE else View.GONE
        findViewById<View>(R.id.btnToggleHistoryTools).setOnClickListener {
            tools.visibility = if (tools.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        }
        selectedDay = savedInstanceState?.getString("selectedDay")
        selectedMetric = savedInstanceState?.getInt("selectedMetric") ?: intent.getIntExtra("METRIC_INDEX", 0).coerceIn(0, 6)
        findViewById<android.widget.Spinner>(R.id.spinnerHistoryMetric).apply {
            adapter = android.widget.ArrayAdapter(this@SensorHistoryActivity, android.R.layout.simple_spinner_dropdown_item,
                listOf("Nitrogen (mg/kg)", "Phosphorus (mg/kg)", "Potassium (mg/kg)", "Moisture (%)", "pH", "Temperature (°C)", "EC (recorded units)"))
            setSelection(selectedMetric)
            onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                    selectedMetric = position; renderSelection()
                }
            }
        }
        findViewById<Button>(R.id.btnHistoryDate).setOnClickListener {
            val today = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Manila"))
            android.app.DatePickerDialog(this, { _, year, month, day ->
                selectedDay = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, day); renderSelection()
            }, today.get(java.util.Calendar.YEAR), today.get(java.util.Calendar.MONTH), today.get(java.util.Calendar.DAY_OF_MONTH)).show()
        }
        findViewById<Button>(R.id.btnClearHistoryDate).setOnClickListener { selectedDay = null; renderSelection() }

        findViewById<ImageButton>(R.id.btnBackHistory)
            .setOnClickListener { finish() }
        btnRefresh.setOnClickListener { loadPage(reset = true) }
        btnLoadMore.setOnClickListener { loadPage(reset = false) }

        if (sensorId.isBlank()) {
            tvEmpty.text = "Missing sensor registration ID."
            tvEmpty.visibility = View.VISIBLE
            btnRefresh.isEnabled = false
        } else {
            loadPage(reset = true)
        }
    }

    private fun loadPage(reset: Boolean) {
        if (loading) return
        if (!reset && nextCursor == null) return

        loading = true
        val generation = ++requestGeneration
        progress.visibility = View.VISIBLE
        btnRefresh.isEnabled = false
        btnLoadMore.visibility = View.GONE

        val cursor = if (reset) null else nextCursor

        val path = "/api/sensors/${android.net.Uri.encode(sensorId)}/readings?limit=20" +
            (cursor?.let { "&cursor=${android.net.Uri.encode(it)}" } ?: "")
        historyRequest = CropCareHttpClient.request("GET", path) { result ->
            if (generation != requestGeneration || isFinishing || isDestroyed) {
                return@request
            }
            loading = false
            progress.visibility = View.GONE
            btnRefresh.isEnabled = true

            if (!result.successful || result.data == null) {
                tvSubtitle.text = "Unable to load history"
                tvNotice.visibility = View.VISIBLE
                tvNotice.text = result.message
                // Preserve already loaded records on a paging error.
                tvEmpty.visibility = if (readings.isEmpty()) View.VISIBLE else View.GONE
                if (readings.isEmpty()) tvEmpty.text = "Could not retrieve readings."
                btnLoadMore.visibility = if (readings.isNotEmpty() && nextCursor != null)
                    View.VISIBLE else View.GONE
                return@request
            }

            val data = result.data ?: return@request
            val page = data.optJSONArray("readings")
            if (page == null) {
                tvNotice.visibility = View.VISIBLE; tvNotice.text = "The API returned an invalid history page. Try refreshing."
                btnLoadMore.visibility = if (nextCursor != null) View.VISIBLE else View.GONE
                return@request
            }
            val received = mutableListOf<SensorHistoryReading>()

            if (page != null) {
                for (index in 0 until page.length()) {
                    val entry = page.optJSONObject(index) ?: continue
                    received.add(SensorHistoryReading.fromJson(entry))
                }
            }

            if (reset) readings.clear()
            val existingIds = readings.map { it.readingId }.toHashSet()
            readings.addAll(received.filter { existingIds.add(it.readingId) })
            renderSelection()

            nextCursor = data.optString("nextCursor", "")
                .takeIf { it.isNotBlank() && it != "null" }
            initialLoaded = true

            val returnedName = data.optString("sensorName", "")
            if (returnedName.isNotBlank()) {
                sensorName = returnedName
                tvTitle.text = "$sensorName history"
            }

            tvSubtitle.text = "${readings.size} reading(s) loaded • newest first"
            tvEmpty.visibility = if (readings.isEmpty()) View.VISIBLE else View.GONE
            tvEmpty.text = "No sensor readings received yet."
            btnLoadMore.visibility = if (nextCursor != null) View.VISIBLE else View.GONE
            renderSelection()
            updateLastReadingNotice()
        }
    }

    private fun renderSelection() {
        visibleReadings.clear(); visibleReadings.addAll(HistoryFilter.apply(readings, selectedDay))
        adapter.notifyDataSetChanged()
        findViewById<SoilHistoryChartView>(R.id.chartHistory).show(visibleReadings, selectedMetric)
        findViewById<TextView>(R.id.tvHistoryCoverage).text =
            "${visibleReadings.size} of ${readings.size} loaded readings shown. " +
            (selectedDay?.let { "Date: $it. " } ?: "") +
            "Chart/filter cover loaded readings only · Asia/Manila. Load More includes older pages."
        tvEmpty.visibility = if (visibleReadings.isEmpty()) View.VISIBLE else View.GONE
        tvEmpty.text = if (selectedDay == null) "No sensor readings received yet." else "No loaded readings match this date. Try Load More."
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("toolsVisible", findViewById<View>(R.id.historyTools).visibility == View.VISIBLE)
        outState.putString("selectedDay", selectedDay); outState.putInt("selectedMetric", selectedMetric)
        super.onSaveInstanceState(outState)
    }
    override fun onStart() {
        super.onStart()
        if (resumeNeeded && sensorId.isNotBlank()) { resumeNeeded = false; loadPage(reset = readings.isEmpty()) }
    }
    override fun onStop() {
        resumeNeeded = loading; requestGeneration++; historyRequest?.cancel(); loading = false
        progress.visibility = View.GONE; btnRefresh.isEnabled = sensorId.isNotBlank()
        btnLoadMore.visibility = if (nextCursor != null) View.VISIBLE else View.GONE
        super.onStop()
    }
    private fun updateLastReadingNotice() {
        if (!initialLoaded || readings.isEmpty()) {
            tvNotice.visibility = View.GONE
            return
        }

        val lastTime = readings.first().recordedAtMs
        val age = System.currentTimeMillis() - lastTime
        if (lastTime <= 0L) {
            tvNotice.visibility = View.VISIBLE
            tvNotice.text = "The latest reading has no valid timestamp."
        } else if (age > 120_000L) {
            val whenText = SimpleDateFormat(
                "MMM dd, yyyy • h:mm a", Locale.getDefault()
            ).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Manila") }.format(Date(lastTime))
            tvNotice.visibility = View.VISIBLE
            tvNotice.text = "No recent measurements. Last recorded: $whenText"
        } else {
            tvNotice.visibility = View.GONE
        }
    }
}
