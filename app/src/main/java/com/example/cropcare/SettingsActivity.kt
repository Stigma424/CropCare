package com.example.cropcare

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

        val btnEditProfileHeader = findViewById<View?>(R.id.btnEditProfileHeader)
        val btnHelp = findViewById<View?>(R.id.btnHelp)
        val btnAccountSettings = findViewById<View?>(R.id.btnAccountSettings)
        val btnNotificationsCenter = findViewById<View?>(R.id.btnNotificationsCenter)
        val btnSubscription = findViewById<View?>(R.id.btnSubscription)
        val btnLogout = findViewById<View?>(R.id.btnLogout)
        val btnBack = findViewById<View?>(R.id.btnBack)

        btnEditProfileHeader?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Bottom Navigation
        findViewById<View?>(R.id.navHome)?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }
        findViewById<View?>(R.id.navAddZone)?.setOnClickListener {
            startActivity(Intent(this, AddZoneActivity::class.java))
        }
        findViewById<View?>(R.id.navNotif)?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }
        findViewById<View?>(R.id.navSettings)?.setOnClickListener {
            // Already settings
        }

        btnHelp?.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        btnAccountSettings?.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        btnNotificationsCenter?.setOnClickListener {
            startActivity(Intent(this, NotificationsActivity::class.java))
        }

        btnSubscription?.setOnClickListener {
            startActivity(Intent(this, SubscriptionActivity::class.java))
        }

        btnLogout?.setOnClickListener {
            startActivity(Intent(this, ConfirmLogoutActivity::class.java))
        }

        btnBack?.setOnClickListener {
            startActivity(Intent(this, DashboardActivity::class.java))
            finish()
        }

        loadUserProfile()
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    private fun loadUserProfile() {
        val currentUser = auth.currentUser ?: return
        val tvProfileName = findViewById<TextView?>(R.id.tvProfileName)
        val tvProfileEmail = findViewById<TextView?>(R.id.tvProfileEmail)

        db.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val fName = doc.getString("firstName") ?: ""
                    val mName = doc.getString("middleName") ?: ""
                    val lName = doc.getString("lastName") ?: ""
                    val fullParts = listOf(fName, mName, lName).filter { it.isNotEmpty() }
                    val fullName = if (fullParts.isNotEmpty()) fullParts.joinToString(" ") else (currentUser.displayName ?: "User")

                    val username = doc.getString("username")
                    val email = doc.getString("email") ?: currentUser.email ?: ""
                    val displaySub = if (!username.isNullOrEmpty()) username else email

                    tvProfileName?.text = fullName
                    tvProfileEmail?.text = displaySub
                } else {
                    tvProfileName?.text = currentUser.displayName ?: "User"
                    tvProfileEmail?.text = currentUser.email ?: ""
                }
            }
            .addOnFailureListener {
                tvProfileName?.text = currentUser.displayName ?: "User"
                tvProfileEmail?.text = currentUser.email ?: ""
            }
    }
}
