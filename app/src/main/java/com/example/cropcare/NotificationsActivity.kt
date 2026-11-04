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
    val timestamp: Long, val isRead: Boolean)

class NotificationsActivity : AppCompatActivity() {
    private val items = mutableListOf<NotificationModel>()
    private lateinit var adapter: NotificationAdapter
    private lateinit var empty: TextView
    private lateinit var clear: Button
    private var request: ApiRequestHandle? = null
    private val handler = Handler(Looper.getMainLooper())
    private var busy = false
    private var running = false
    private val refresh = object : Runnable {
        override fun run() { if (running) { if (!busy) load(); handler.postDelayed(this, 60_000) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_notifications)
        empty = findViewById(R.id.tvEmptyNotif); clear = findViewById(R.id.btnClearAll)
        adapter = NotificationAdapter(items) { item ->
            if (!busy) AlertDialog.Builder(this).setTitle(item.title).setMessage(item.message)
                .setPositiveButton("Mark read") { _, _ -> mutate { cb -> CropCareFarmerApi.markRead(item.id, cb) } }
                .setNeutralButton("Delete") { _, _ -> confirmDelete(item.id) }
                .setNegativeButton("Close", null).show()
        }
        findViewById<RecyclerView>(R.id.rvNotifications).apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity); adapter = this@NotificationsActivity.adapter
        }
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        clear.setOnClickListener { if (!busy) confirmDelete(null) }
        empty.setOnClickListener { if (!busy) load() }
        findViewById<View>(R.id.btnRefreshNotifications).setOnClickListener { if (!busy) load() }
    }
    private fun confirmDelete(id: String?) {
        AlertDialog.Builder(this).setTitle(if (id == null) "Clear notification history?" else "Delete notification?")
            .setMessage("This removes saved notification history for your account.")
            .setNegativeButton("Cancel", null).setPositiveButton("Delete") { _, _ ->
                mutate { cb -> CropCareFarmerApi.deleteNotification(id, cb) }
            }.show()
    }
    private fun mutate(action: ((FarmerApiResult) -> Unit) -> ApiRequestHandle) {
        busy = true; clear.isEnabled = false; request?.cancel()
        request = action { result ->
            Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            load()
        }
    }
    private fun load() {
        busy = true; clear.isEnabled = false; request?.cancel()
        request = CropCareFarmerApi.notifications { result ->
            busy = false; clear.isEnabled = true
            if (result.successful && result.array != null) {
                items.clear()
                val array = result.array!!
                for (i in 0 until array.length()) {
                    val row = array.optJSONObject(i) ?: continue
                    items.add(NotificationModel(row.optString("id"), row.optString("title"),
                        row.optString("message"), row.optLong("timestampMs"), row.optBoolean("isRead")))
                }
                adapter.notifyDataSetChanged()
                empty.text = if (items.isEmpty()) "No notifications. Tap to refresh." else "Showing up to 100 latest notifications. Any records remaining after Clear All can be cleared again."
            } else empty.text = result.message + " Tap to retry."
            empty.visibility = View.VISIBLE
        }
    }
    override fun onStart() { super.onStart(); running = true; handler.post(refresh) }
    override fun onStop() { running = false; handler.removeCallbacks(refresh); request?.cancel(); busy = false; super.onStop() }
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
