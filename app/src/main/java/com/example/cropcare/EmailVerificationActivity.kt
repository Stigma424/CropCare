package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EmailVerificationActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        val tvSubtitle = findViewById<TextView>(R.id.tvVerificationSubtitle)
        val etCode = findViewById<EditText>(R.id.etCode)
        val btnSubmit = findViewById<TextView>(R.id.btnSubmit)
        val tvResendCode = findViewById<TextView>(R.id.tvResendCode)
        val tvShowCode = findViewById<TextView>(R.id.tvShowCode)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)

        if (userEmail.isNotEmpty()) {
            tvSubtitle.text = "Verification code sent to $userEmail. Please enter the 6-digit code below."
        }

        btnSubmit.setOnClickListener {
            val enteredCode = etCode.text.toString().trim()

            if (enteredCode.isEmpty()) {
                Toast.makeText(this, "Please enter the verification code", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Email address missing. Please start over.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
                finish()
                return@setOnClickListener
            }

            // Verify code against Firestore
            db.collection("password_resets").document(userEmail)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val savedCode = document.getString("code") ?: ""
                        val createdAt = document.getLong("createdAt") ?: 0L

                        // Code expiration check (15 minutes = 15 * 60 * 1000 ms)
                        val isExpired = (System.currentTimeMillis() - createdAt) > (15 * 60 * 1000)

                        if (isExpired) {
                            Toast.makeText(this, "Verification code expired. Please request a new code.", Toast.LENGTH_LONG).show()
                            return@addOnSuccessListener
                        }

                        if (enteredCode == savedCode) {
                            // Mark reset token as verified
                            db.collection("password_resets").document(userEmail)
                                .update("isVerified", true)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Code verified successfully!", Toast.LENGTH_SHORT).show()

                                    // Navigate to ResetPasswordActivity with verified state
                                    val intent = Intent(this, ResetPasswordActivity::class.java)
                                    intent.putExtra("USER_EMAIL", userEmail)
                                    intent.putExtra("IS_VERIFIED", true)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Verification error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "Invalid verification code. Please check your email and try again.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this, "No verification code requested for this email. Please request a new code.", Toast.LENGTH_LONG).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error validating code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvResendCode.setOnClickListener {
            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Email missing. Please restart forgot password process.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newCode = (100000..999999).random().toString()
            val resetData = hashMapOf(
                "email" to userEmail,
                "code" to newCode,
                "createdAt" to System.currentTimeMillis(),
                "isVerified" to false
            )

            db.collection("password_resets").document(userEmail)
                .set(resetData)
                .addOnSuccessListener {
                    EmailHelper.sendVerificationCode(userEmail, newCode) { _, _ ->
                        runOnUiThread {
                            Toast.makeText(this, "New verification code sent to $userEmail", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to resend code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvShowCode.setOnClickListener {
            if (userEmail.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            db.collection("password_resets").document(userEmail)
                .get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        val code = doc.getString("code") ?: ""
                        etCode.setText(code)
                        Toast.makeText(this, "Verification Code: $code", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "No code generated yet. Tap Resend Code.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error fetching code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnCancel.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }
}