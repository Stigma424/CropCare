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
        adapter = SensorHistoryAdapter(readings)
        recycler.adapter = adapter

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

        CropCareApiClient.getSensorHistory(sensorId, cursor, 20) { result ->
            if (generation != requestGeneration || isFinishing || isDestroyed) {
                return@getSensorHistory
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
                return@getSensorHistory
            }

            val data = result.data
            val page = data.optJSONArray("readings")
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
            adapter.notifyDataSetChanged()

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
            updateLastReadingNotice()
        }
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
            ).format(Date(lastTime))
            tvNotice.visibility = View.VISIBLE
            tvNotice.text = "No recent measurements. Last recorded: $whenText"
        } else {
            tvNotice.visibility = View.GONE
        }
    }
}
