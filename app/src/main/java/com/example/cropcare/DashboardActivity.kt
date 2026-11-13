package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale

class DashboardActivity : AppCompatActivity() {
    private val zones = mutableListOf<ZoneMonitoringSnapshot>()
    private lateinit var adapter: ZoneAdapter
    private var request: MonitoringRequest? = null
    private var profile: ApiRequestHandle? = null
    private var generation = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_dashboard)
        window.statusBarColor = android.graphics.Color.parseColor("#176B35")
        window.navigationBarColor = android.graphics.Color.parseColor("#F3F6F1")
        findViewById<TextView>(R.id.tvDashboardDate).text = java.text.SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US).apply {
            timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
        }.format(java.util.Date())
        findViewById<View>(R.id.btnHeaderNotifications).setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<TextView>(R.id.tvDashboardSoilHealth).text = "No readings yet"
        adapter = ZoneAdapter(zones, { refresh() }) { id ->
            startActivity(Intent(this, ZoneManagementActivity::class.java).putExtra("ZONE_ID", id))
        }
        findViewById<RecyclerView>(R.id.rvZones).apply {
            layoutManager = LinearLayoutManager(this@DashboardActivity); adapter = this@DashboardActivity.adapter
        }
        findViewById<View>(R.id.btnRefreshZones).setOnClickListener { refresh() }
        findViewById<View>(R.id.navAddZone).setOnClickListener { startActivity(Intent(this, AddZoneActivity::class.java)) }
        findViewById<View>(R.id.navNotif).setOnClickListener { startActivity(Intent(this, NotificationsActivity::class.java)) }
        findViewById<View>(R.id.navSettings).setOnClickListener { startActivity(Intent(this, SettingsActivity::class.java)) }
    }
    override fun onStart() {
        super.onStart()
        UserPhoto.bind(findViewById(R.id.ivDashboardPhoto), findViewById(R.id.tvAvatarInitials))
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java)); finish(); return
        }
        CropCareFcmRegistration.register()
        profile = CropCareHttpClient.request("GET", "/api/account/me") { result ->
            findViewById<TextView>(R.id.tvWelcome).text = "Welcome, " +
                (result.data?.optString("username")?.takeIf { it.isNotBlank() } ?: "Farmer")
            findViewById<TextView>(R.id.tvAvatarInitials).text = result.data?.optString("firstName")
                ?.firstOrNull()?.uppercaseChar()?.toString() ?: "C"
        }
        refresh()
    }
    private fun refresh() {
        request?.cancel(); val current = ++generation
        findViewById<TextView>(R.id.tvActiveZonesCount).text = "Loading zones…"
        request = ZoneMonitoringRepository.dashboard { result, error ->
            if (current != generation || isFinishing || isDestroyed) return@dashboard
            if (result == null) {
                findViewById<TextView>(R.id.tvActiveZonesCount).text = "Zones unavailable · refresh to retry"
                Toast.makeText(this, error, Toast.LENGTH_LONG).show()
            } else {
                zones.clear(); zones.addAll(result); adapter.notifyDataSetChanged()
                findViewById<TextView>(R.id.tvActiveZonesCount).text = "${zones.count { !it.zone.isHarvested }} zones active"
                val scores = zones.mapNotNull { it.score }
                findViewById<SoilScoreView>(R.id.soilScoreRing).setScore(scores.takeIf { it.isNotEmpty() }?.average())
                findViewById<TextView>(R.id.tvDashboardSoilHealth).text = if (scores.isEmpty()) "Awaiting an available server assessment" else
                    "${scores.size} assessed zone${if (scores.size == 1) "" else "s"} · provisional"
            }
        }
    }
    override fun onStop() { generation++; request?.cancel(); profile?.cancel(); super.onStop() }
}

