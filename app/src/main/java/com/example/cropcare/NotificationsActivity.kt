package com.example.cropcare

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class NotificationModel(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L
)

class NotificationsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: NotificationAdapter
    private val notifList = mutableListOf<NotificationModel>()
    private lateinit var tvEmptyNotif: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvEmptyNotif = findViewById(R.id.tvEmptyNotif)
        val rvNotifications = findViewById<RecyclerView>(R.id.rvNotifications)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnClearAll = findViewById<Button>(R.id.btnClearAll)

        rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(notifList)
        rvNotifications.adapter = adapter

        btnBack.setOnClickListener { finish() }

        btnClearAll.setOnClickListener {
            clearNotifications()
        }

        fetchNotifications()
    }

    private fun fetchNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .get()
            .addOnSuccessListener { docs ->
                notifList.clear()
                for (doc in docs) {
                    val rawTime = doc.get("timestamp")
                    val timeMs = when (rawTime) {
                        is Timestamp -> rawTime.toDate().time
                        is Long -> rawTime
                        else -> System.currentTimeMillis()
                    }

                    val notif = NotificationModel(
                        id = doc.id,
                        title = doc.getString("title") ?: "CropCare Alert",
                        message = doc.getString("message") ?: "",
                        timestamp = timeMs
                    )
                    notifList.add(notif)
                }

                notifList.sortByDescending { it.timestamp }

                if (notifList.isEmpty()) {
                    tvEmptyNotif.visibility = View.VISIBLE
                } else {
                    tvEmptyNotif.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                tvEmptyNotif.visibility = View.VISIBLE
            }
    }

    private fun clearNotifications() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("notifications")
            .get()
            .addOnSuccessListener { docs ->
                val batch = db.batch()
                for (doc in docs) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnSuccessListener {
                    Toast.makeText(this, "Notifications cleared", Toast.LENGTH_SHORT).show()
                    fetchNotifications()
                }
            }
    }
}

class NotificationAdapter(private val list: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.NotifViewHolder>() {

    class NotifViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvNotifTitle)
        val tvTime: TextView = itemView.findViewById(R.id.tvNotifTime)
        val tvBody: TextView = itemView.findViewById(R.id.tvNotifBody)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotifViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return NotifViewHolder(view)
    }

    override fun onBindViewHolder(holder: NotifViewHolder, position: Int) {
        val item = list[position]
        holder.tvTitle.text = item.title
        holder.tvBody.text = item.message

        val sdf = SimpleDateFormat("MMM dd, hh:mm a", Locale.US)
        holder.tvTime.text = sdf.format(Date(item.timestamp))
    }

    override fun getItemCount(): Int = list.size
}
