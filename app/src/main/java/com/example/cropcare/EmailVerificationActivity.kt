package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        val btnSubmit = findViewById<TextView>(R.id.btnSubmit)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        btnSubmit.setOnClickListener {
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
            finish()
        }

        btnCancel.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }
}
