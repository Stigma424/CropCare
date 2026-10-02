package com.example.cropcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SettingsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val prefs = getSharedPreferences("CropCarePrefs", MODE_PRIVATE)

        val btnHelp = findViewById<Button>(R.id.btnHelp)
        val btnAccountSettings = findViewById<Button>(R.id.btnAccountSettings)
        val btnNotificationsCenter = findViewById<Button>(R.id.btnNotificationsCenter)
        val btnSubscription = findViewById<Button>(R.id.btnSubscription)
        val switchNotification = findViewById<SwitchMaterial>(R.id.switchNotification)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Set initial switch state from preferences
        val isNotifEnabled = prefs.getBoolean("notifications_enabled", true)
        switchNotification.isChecked = isNotifEnabled

        btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        btnAccountSettings.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        btnNotificationsCenter.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        btnSubscription.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("notifications_enabled", isChecked).apply()

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("cropcare_alerts")
                Toast.makeText(this, "Notifications turned ON", Toast.LENGTH_SHORT).show()
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("cropcare_alerts")
                Toast.makeText(this, "Notifications turned OFF", Toast.LENGTH_SHORT).show()
            }
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, ConfirmLogoutActivity::class.java))
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
    }
}
