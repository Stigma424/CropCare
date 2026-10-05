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
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "cropcare_alerts_channel"

    fun checkAndTriggerSoilAlerts(
        context: Context,
        zoneName: String,
        n: Double,
        p: Double,
        k: Double,
        moisture: Double,
        ph: Double,
        recommendationSummary: String
    ) {
        val prefs = context.getSharedPreferences("CropCarePrefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            return
        }

        // Avoid firing duplicate alerts for same zone within 30 seconds (30,000 ms)
        val lastAlertKey = "last_alert_$zoneName"
        val lastAlertTime = prefs.getLong(lastAlertKey, 0L)
        val now = System.currentTimeMillis()

        if (now - lastAlertTime < 30000L) {
            return
        }

        val alerts = mutableListOf<Pair<String, String>>()

        // Threshold checks
        if (n in 0.1..20.0) {
            alerts.add("Low Nitrogen Warning" to "$zoneName Nitrogen level is low (%.0f mg/kg). Urea side-dressing recommended.".format(Locale.US, n))
        } else if (n > 80.0) {
            alerts.add("High Nitrogen Alert" to "$zoneName Nitrogen level is high (%.0f mg/kg). Pause nitrogen fertilization.".format(Locale.US, n))
        }

        if (p in 0.1..10.0) {
            alerts.add("Low Phosphorus Warning" to "$zoneName Phosphorus level is low (%.0f mg/kg). Complete 14-14-14 recommended.".format(Locale.US, p))
        }

        if (k in 0.1..20.0) {
            alerts.add("Low Potassium Warning" to "$zoneName Potassium level is low (%.0f mg/kg). Muriate of Potash recommended.".format(Locale.US, k))
        }

        if (moisture in 0.1..25.0) {
            alerts.add("Low Moisture Alert" to "$zoneName Soil moisture is LOW (%.0f%%). Irrigation needed immediately.".format(Locale.US, moisture))
        } else if (moisture > 75.0) {
            alerts.add("High Moisture Alert" to "$zoneName Soil moisture is HIGH (%.0f%%). Ensure proper field drainage.".format(Locale.US, moisture))
        }

        if (ph in 0.1..5.5) {
            alerts.add("Acidic Soil Alert" to "$zoneName Soil pH is acidic (%.1f). Apply Agricultural Lime.".format(Locale.US, ph))
        } else if (ph > 7.5) {
            alerts.add("Alkaline Soil Alert" to "$zoneName Soil pH is alkaline (%.1f).".format(Locale.US, ph))
        }

        if (alerts.isEmpty() && recommendationSummary.isNotBlank() && recommendationSummary.contains("Rec:", ignoreCase = true)) {
            alerts.add("New Recommendation" to "$zoneName: $recommendationSummary")
        }

        if (alerts.isNotEmpty()) {
            prefs.edit().putLong(lastAlertKey, now).apply()

            val (title, body) = alerts.first()
            sendSystemNotification(context, title, body)
            saveToFirestoreHistory(title, body)
        }
    }

    private fun sendSystemNotification(context: Context, title: String, body: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CropCare Sensor Warnings & Recommendations",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for low/high sensor data values and fertilizer recommendations"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, NotificationsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
    }

    private fun saveToFirestoreHistory(title: String, body: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val notifData = hashMapOf(
            "title" to title,
            "message" to body,
            "type" to "warning",
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .add(notifData)
            .addOnFailureListener { e ->
                Log.e("NotificationHelper", "Failed to save notification: ${e.message}")
            }
    }

    fun checkAndTriggerOfflineSensorAlert(
        context: Context,
        zoneName: String,
        sensorName: String
    ) {
        val prefs = context.getSharedPreferences("CropCarePrefs", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

        if (!notificationsEnabled) {
            return
        }

        val lastAlertKey = "last_offline_alert_$sensorName"
        val lastAlertTime = prefs.getLong(lastAlertKey, 0L)
        val now = System.currentTimeMillis()

        if (now - lastAlertTime < 300000L) {
            return
        }

        prefs.edit().putLong(lastAlertKey, now).apply()

        val title = "Sensor Offline Warning"
        val body = "Sensor '$sensorName' in $zoneName is currently offline or has not reported data recently."
        sendSystemNotification(context, title, body)
        saveToFirestoreHistory(title, body)
    }
}
