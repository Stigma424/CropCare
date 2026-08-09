package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class DashboardActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        auth = FirebaseAuth.getInstance()

        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val btnLogout = findViewById<Button>(R.id.btnLogout)

        val currentUser = auth.currentUser
        tvWelcome.text = "Welcome to Dashboard!\nLogged in as: ${currentUser?.email ?: "User"}"

        btnLogout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}