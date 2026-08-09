package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingsActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val btnHelp = findViewById<Button>(R.id.btnHelp)
        val btnAccountSettings = findViewById<Button>(R.id.btnAccountSettings)
        val switchNotification = findViewById<SwitchMaterial>(R.id.switchNotification)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnBack = findViewById<Button>(R.id.btnBack)

        btnHelp.setOnClickListener {
            startActivity(Intent(this, HelpActivity::class.java))
        }

        btnAccountSettings.setOnClickListener {
            startActivity(Intent(this, AccountSettingsActivity::class.java))
        }

        switchNotification.setOnCheckedChangeListener { _, isChecked ->
            val status = if (isChecked) "ON" else "OFF"
            Toast.makeText(this, "Notifications turned $status", Toast.LENGTH_SHORT).show()
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