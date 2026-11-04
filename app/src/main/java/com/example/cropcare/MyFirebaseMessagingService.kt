
package com.example.cropcare

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService :
    FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        CropCareFcmRegistration.save(token)
    }

    override fun onMessageReceived(
        message: RemoteMessage
    ) {
        super.onMessageReceived(message)
        if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser == null) return
        if (!PushDeliveryCache.accept(message.messageId)) return

        val enabled = getSharedPreferences(
            "CropCarePrefs",
            Context.MODE_PRIVATE
        ).getBoolean(
            "notifications_enabled",
            true
        )

        if (!enabled) {
            return
        }

        val title =
            message.data["title"]
                ?: message.notification?.title
                ?: "CropCare Sensor Alert"

        val body =
            message.data["body"]
                ?: message.notification?.body
                ?: "A sensor update is available."

        // The API already saved this notification
        // into Firestore history. Do not save it twice.
        showNotification(title, body, message.messageId)
    }

    private fun showNotification(
        title: String,
        body: String,
        deliveryId: String?
    ) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val manager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager

        val channelId = "cropcare_alerts_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "CropCare Sensor Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                )
            )
        }

        val intent = Intent(
            this,
            NotificationsActivity::class.java
        ).apply {
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            this,
            channelId
        )
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(body)
            )
            .setPriority(
                NotificationCompat.PRIORITY_HIGH
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(deliveryId ?: java.util.UUID.randomUUID().toString(), 0, notification)
    }
}
