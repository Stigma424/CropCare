package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etForgotEmail)
        val btnSubmit = findViewById<Button>(R.id.btnSubmitForgot)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnSubmit.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Verify email exists in Firestore user database
            db.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        // Generate 6-digit verification code
                        val verificationCode = (100000..999999).random().toString()

                        val resetData = hashMapOf(
                            "email" to email,
                            "code" to verificationCode,
                            "createdAt" to System.currentTimeMillis(),
                            "isVerified" to false
                        )

                        // Save verification code in Firestore
                        db.collection("password_resets").document(email)
                            .set(resetData)
                            .addOnSuccessListener {
                                // Send 6-digit verification code email
                                EmailHelper.sendVerificationCode(email, verificationCode) { success, error ->
                                    runOnUiThread {
                                        if (success) {
                                            Toast.makeText(this, "Verification code sent to $email", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(this, "Email API response: $error", Toast.LENGTH_LONG).show()
                                        }

                                        val intent = Intent(this, EmailVerificationActivity::class.java)
                                        intent.putExtra("USER_EMAIL", email)
                                        startActivity(intent)
                                        finish()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error sending code: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "No account found with this email", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}