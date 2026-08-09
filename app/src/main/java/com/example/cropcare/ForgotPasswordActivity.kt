package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail = findViewById<EditText>(R.id.etForgotEmail)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitForgot)

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Directly navigate to verification page
            val intent = Intent(this, EmailVerificationActivity::class.java)
            intent.putExtra("USER_EMAIL", email)
            startActivity(intent)
        }
    }
}