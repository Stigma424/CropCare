
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

    // Temporary client-side state tracking.
    // Server-side deduplication will replace this later.
    @Suppress("UNUSED_PARAMETER")
    @Synchronized
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
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        val prefs = context.getSharedPreferences(
            "CropCarePrefs",
            Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean("notifications_enabled", true)) {
            return
        }

        val readings = listOf(n, p, k, moisture, ph)

        // Do not evaluate missing or non-finite measurements.
        if (readings.any { !it.isFinite() }) {
            return
        }

        // Screens currently pass zero values when no data exists.
        // Do not generate alerts or reset an alert state for no data.
        if (readings.all { it <= 0.0 }) {
            return
        }

        val conditions = mutableListOf<Pair<String, String>>()

        if (n in 0.1..20.0) {
            conditions.add(
                "N_LOW" to
                        "Nitrogen reading is low (${format(n)})."
            )
        } else if (n > 80.0) {
            conditions.add(
                "N_HIGH" to
                        "Nitrogen reading is high (${format(n)})."
            )
        }

        if (p in 0.1..10.0) {
            conditions.add(
                "P_LOW" to
                        "Phosphorus reading is low (${format(p)})."
            )
        }

        if (k in 0.1..20.0) {
            conditions.add(
                "K_LOW" to
                        "Potassium reading is low (${format(k)})."
            )
        }

        if (moisture in 0.1..25.0) {
            conditions.add(
                "MOISTURE_LOW" to
                        "Soil moisture is low (${format(moisture)}%)."
            )
        } else if (moisture > 75.0) {
            conditions.add(
                "MOISTURE_HIGH" to
                        "Soil moisture is high (${format(moisture)}%)."
            )
        }

        if (ph in 0.1..5.5) {
            conditions.add(
                "PH_LOW" to
                        "Soil pH is acidic (${format(ph, 1)})."
            )
        } else if (ph > 7.5) {
            conditions.add(
                "PH_HIGH" to
                        "Soil pH is alkaline (${format(ph, 1)})."
            )
        }

        // Separate alert state for each user and zone name.
        val key = "soil_condition_" +
                (userId + "|" + zoneName).hashCode()

        val newState = conditions
            .map { it.first }
            .sorted()
            .joinToString("|")

        val previousState = prefs.getString(key, "") ?: ""

        // Same condition as the last check:
        // never repeat just because a screen reloaded.
        if (previousState == newState) {
            return
        }

        if (conditions.isEmpty()) {
            // Conditions recovered. Allow a future alert
            // if the same warning returns later.
            prefs.edit().remove(key).apply()
            return
        }

        // Remember the new condition before notifying.
        prefs.edit().putString(key, newState).apply()

        val title = if (conditions.size == 1) {
            "Soil Warning: $zoneName"
        } else {
            "Soil Warnings: $zoneName"
        }

        val body = conditions.joinToString(" ") {
            it.second
        }

        sendSystemNotification(context, title, body)
        saveToFirestoreHistory(
            title = title,
            body = body,
            type = "warning"
        )
    }

    // Reduce repeated offline alerts while a device
    // remains disconnected. More precise device-state
    // alerts will be implemented by the backend.
    @Synchronized
    fun checkAndTriggerOfflineSensorAlert(
        context: Context,
        zoneName: String,
        sensorName: String
    ) {
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        val prefs = context.getSharedPreferences(
            "CropCarePrefs",
            Context.MODE_PRIVATE
        )

        if (!prefs.getBoolean("notifications_enabled", true)) {
            return
        }

        val key = "offline_warning_" +
                (userId + "|" + zoneName + "|" + sensorName).hashCode()

        val lastTime = prefs.getLong(key, 0L)
        val now = System.currentTimeMillis()

        // At most once in twelve hours for this sensor.
        val cooldown = 12L * 60L * 60L * 1000L

        if (lastTime > 0 &&
            now >= lastTime &&
            now - lastTime < cooldown
        ) {
            return
        }

        prefs.edit().putLong(key, now).apply()

        val title = "Sensor Offline Warning"
        val body =
            "Sensor '$sensorName' in $zoneName has stopped " +
                    "reporting recent measurements."

        sendSystemNotification(context, title, body)

        saveToFirestoreHistory(
            title,
            body,
            "warning"
        )
    }

    private fun sendSystemNotification(
        context: Context,
        title: String,
        body: String
    ) {
        val manager = context.getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CropCare Sensor Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Soil conditions and sensor warnings"
            }

            manager.createNotificationChannel(channel)
        }

        val intent = Intent(
            context,
            NotificationsActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(body)
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        try {
            manager.notify(
                System.currentTimeMillis().toInt(),
                notification
            )
        } catch (error: SecurityException) {
            Log.w(
                "CropCareNotification",
                "Notification permission not granted.",
                error
            )
        }
    }

    private fun saveToFirestoreHistory(
        title: String,
        body: String,
        type: String
    ) {
        val userId =
            FirebaseAuth.getInstance().currentUser?.uid ?: return

        val data = hashMapOf(
            "title" to title,
            "message" to body,
            "type" to type,
            "timestamp" to FieldValue.serverTimestamp()
        )

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("notifications")
            .add(data)
            .addOnFailureListener { error ->
                Log.e(
                    "CropCareNotification",
                    "Could not save alert: ${error.message}"
                )
            }
    }

    private fun format(
        number: Double,
        decimals: Int = 0
    ): String {
        return String.format(
            Locale.US,
            "%.${decimals}f",
            number
        )
    }
}
