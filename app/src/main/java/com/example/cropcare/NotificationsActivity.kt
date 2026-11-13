package com.example.cropcare

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

data class NotificationModel(val id: String, val title: String, val message: String,
    val timestamp: Long, val isRead: Boolean, val zoneId: String = "", val sensorId: String = "")

class NotificationsActivity : AppCompatActivity() {
    private val items = mutableListOf<NotificationModel>()
    private lateinit var adapter: NotificationAdapter
    private lateinit var empty: TextView
    private lateinit var clear: Button
    private var request: ApiRequestHandle? = null
    private val handler = Handler(Looper.getMainLooper())
    private var requestedNotification: String? = null
    private var clearRequested = false
    private var generation = 0
    private var busy = false
    private var running = false
    private val refresh = object : Runnable {
        override fun run() { if (running) { if (!busy) load(); handler.postDelayed(this, 60_000) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_notifications)
        requestedNotification = intent.getStringExtra("NOTIFICATION_ID")?.takeIf { intent.getStringExtra("RECIPIENT_UID") == com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid }
        empty = findViewById(R.id.tvEmptyNotif); clear = findViewById(R.id.btnClearAll)
        adapter = NotificationAdapter(items) { item -> if (!busy) open(item) }
        findViewById<RecyclerView>(R.id.rvNotifications).apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity); adapter = this@NotificationsActivity.adapter
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnTestPush).setOnClickListener {
            CropCareHttpClient.request("POST", "/api/notifications/test") { result ->
                Toast.makeText(this, if(result.successful) "Test queued. Check your phone notification tray." else result.message, Toast.LENGTH_LONG).show()
            }
        }
        clear.setOnClickListener { if (!busy) confirmDelete(null) }
        empty.setOnClickListener { if (!busy) { CropCareFcmRegistration.register(); load() } }
        findViewById<View>(R.id.btnRefreshNotifications).setOnClickListener { if (!busy) { CropCareFcmRegistration.register(); load() } }
    }
    private fun confirmDelete(id: String?) {
        AlertDialog.Builder(this).setTitle(if (id == null) "Clear notification history?" else "Delete notification?")
            .setMessage("This removes saved notification history for your account.")
            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                clearRequested = id == null
                mutate { cb -> CropCareFarmerApi.deleteNotification(id, cb) }
            }.show()
    }
    private fun mutate(action: ((FarmerApiResult) -> Unit) -> ApiRequestHandle) {
        busy = true; clear.isEnabled = false; request?.cancel()
        request = action { result ->
            if (!result.successful) clearRequested = false
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            load()
        }
    }
    private fun load() {
        busy = true; clear.isEnabled = false; request?.cancel()
        val current = ++generation
        request = CropCareFarmerApi.notifications { result ->
            if (current != generation || !running) return@notifications
            busy = false; clear.isEnabled = true
            if (result.successful && result.array != null) {
                items.clear()
                val array = result.array!!
                for (i in 0 until array.length()) {
                    val row = array.optJSONObject(i) ?: continue
                    items.add(NotificationModel(row.optString("id"), row.optString("title"),
                        row.optString("message"), row.optLong("timestampMs"), row.optBoolean("isRead"), row.optString("zoneId"), row.optString("sensorId")))
                }
                adapter.notifyDataSetChanged()
                if (clearRequested) { clearRequested = false; Toast.makeText(this, if (items.isEmpty()) "Notification history cleared" else "Clear completed but history remains; each request removes at most 5,000 records", Toast.LENGTH_LONG).show() }
                requestedNotification?.let { id -> requestedNotification = null; val item = items.find { it.id == id }; if (item != null) open(item) else Toast.makeText(this, "Notification is no longer in the latest history", Toast.LENGTH_LONG).show() }
                empty.text = if (items.isEmpty()) "No notifications. Tap to refresh." else "Showing up to 100 latest notifications. Any records remaining after Clear All can be cleared again."
            } else empty.text = result.message + " Tap to retry."
            empty.append("\n" + permissionStatus() + " · " + CropCareFcmRegistration.status)
            empty.visibility = View.VISIBLE
        }
    }
    private fun permissionStatus(): String = if (androidx.core.app.NotificationManagerCompat.from(this).areNotificationsEnabled()) "Phone alerts allowed" else "Phone alerts blocked in system settings"
    private fun open(item: NotificationModel) {
        val builder = AlertDialog.Builder(this).setTitle(item.title).setMessage(item.message)
            .setPositiveButton("Mark read") { _, _ -> mutate { cb -> CropCareFarmerApi.markRead(item.id, cb) } }
            .setNegativeButton("Close", null)
        if (item.zoneId.isNotBlank()) builder.setNeutralButton("More…") { _, _ -> AlertDialog.Builder(this).setItems(arrayOf("Open zone", "Delete")) { _, index -> if (index == 0) startActivity(android.content.Intent(this, ZoneManagementActivity::class.java).putExtra("ZONE_ID", item.zoneId)) else confirmDelete(item.id) }.show() }
        else builder.setNeutralButton("Delete") { _, _ -> confirmDelete(item.id) }
        builder.show()
    }
    override fun onStart() { super.onStart(); running = true; handler.post(refresh) }
    override fun onStop() { running = false; generation++; handler.removeCallbacks(refresh); request?.cancel(); busy = false; super.onStop() }
}

class NotificationAdapter(private val items: List<NotificationModel>, private val open: (NotificationModel) -> Unit) :
    RecyclerView.Adapter<NotificationAdapter.Holder>() {
    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvNotifTitle)
        val time: TextView = view.findViewById(R.id.tvNotifTime)
        val body: TextView = view.findViewById(R.id.tvNotifBody)
    }
    override fun onCreateViewHolder(parent: ViewGroup, type: Int) = Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false))
    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.title.text = (if (item.isRead) "" else "● ") + item.title
        holder.title.setTypeface(null, if (item.isRead) Typeface.NORMAL else Typeface.BOLD)
        holder.body.text = item.message
        holder.time.text = if (item.timestamp > 0) SimpleDateFormat("MMM dd, h:mm a", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("Asia/Manila")
        }.format(Date(item.timestamp)) else "Time unavailable"
        holder.itemView.setOnClickListener { open(item) }
    }
}

