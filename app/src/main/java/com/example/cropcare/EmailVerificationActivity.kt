package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
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
        val btnProceedToReset = findViewById<Button>(R.id.btnProceedToReset)
        val btnBack = findViewById<Button>(R.id.btnBack)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        btnProceedToReset.setOnClickListener {
            // Redirect to Reset Password Page
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
            finish()
        }
        btnBack.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }
}