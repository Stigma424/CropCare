package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class EmailVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        val btnProceedToReset = findViewById<Button>(R.id.btnProceedToReset)

        btnProceedToReset.setOnClickListener {
            // Redirect to Reset Password Page
            val intent = Intent(this, ResetPasswordActivity::class.java)
            intent.putExtra("USER_EMAIL", userEmail)
            startActivity(intent)
            finish()
        }
    }
}