package com.example.cropcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val prefs = getSharedPreferences("CropCarePrefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            Log.d("FCM", "Notifications disabled by user preference.")
            return
        }

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "CropCare Alert"
        val body = remoteMessage.notification?.body ?: remoteMessage.data["body"] ?: "New notification received."
        val type = remoteMessage.data["type"] ?: "warning"

        // Save to Firestore history for user if logged in
        saveNotificationToHistory(title, body, type)

        showNotification(title, body)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Refreshed token: $token")
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update("fcmToken", token)
    }

    private fun showNotification(title: String, body: String) {
        val channelId = "cropcare_alerts_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CropCare Sensor Warnings & Recommendations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for low/high sensor data values and fertilizer recommendations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun saveNotificationToHistory(title: String, body: String, type: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notifData = hashMapOf(
            "title" to title,
            "message" to body,
            "type" to type,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .add(notifData)
    }
}
