
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
        val owner = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        val envelope = AccountPushEnvelope.parse(message.data, owner) ?: return

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

        if (!PersistentPushDelivery.accept(this, owner, envelope.id)) return
        showNotification(envelope.title, envelope.body, envelope.id, owner)
    }
    private fun showNotification(
        title: String,
        body: String,
        deliveryId: String,
        owner: String
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
            action = "com.example.cropcare.NOTIFICATION.$deliveryId"
            putExtra("NOTIFICATION_ID", deliveryId); putExtra("RECIPIENT_UID", owner)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this, deliveryId.hashCode(),
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
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(deliveryId, 0, notification)
    }
}
