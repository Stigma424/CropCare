package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""
        val isVerifiedParam = intent.getBooleanExtra("IS_VERIFIED", false)

        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnSavePassword = findViewById<Button>(R.id.btnSavePassword)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Validation guard: Verify user has valid verified session
        if (userEmail.isEmpty()) {
            Toast.makeText(this, "Unauthorized access. Email missing.", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
            return
        }

        if (!isVerifiedParam) {
            // Double check Firestore if isVerified isn't passed in Intent
            db.collection("password_resets").document(userEmail)
                .get()
                .addOnSuccessListener { doc ->
                    val isVerifiedInDb = doc.exists() && (doc.getBoolean("isVerified") == true)
                    if (!isVerifiedInDb) {
                        Toast.makeText(this, "Unauthorized access. Please verify email code first.", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this, ForgotPasswordActivity::class.java))
                        finish()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Verification error. Please start over.", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ForgotPasswordActivity::class.java))
                    finish()
                }
        }

        btnSavePassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all password fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update user password in Firestore and Firebase Auth
            db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val userDoc = documents.documents[0]
                        val docId = userDoc.id
                        val oldPassword = userDoc.getString("password")

                        // 1. Update password in Firestore user document
                        db.collection("users").document(docId)
                            .update("password", newPassword)
                            .addOnSuccessListener {
                                // 2. Clean up reset verification record
                                db.collection("password_resets").document(userEmail).delete()

                                // 3. Try updating Firebase Auth password if old password available
                                if (!oldPassword.isNullOrEmpty()) {
                                    auth.signInWithEmailAndPassword(userEmail, oldPassword)
                                        .addOnSuccessListener {
                                            auth.currentUser?.updatePassword(newPassword)
                                                ?.addOnCompleteListener {
                                                    auth.signOut()
                                                    onPasswordResetComplete()
                                                } ?: run {
                                                auth.signOut()
                                                onPasswordResetComplete()
                                            }
                                        }
                                        .addOnFailureListener {
                                            onPasswordResetComplete()
                                        }
                                } else {
                                    onPasswordResetComplete()
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update password: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "User email not found in database", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
            finish()
        }
    }

    private fun onPasswordResetComplete() {
        Toast.makeText(this, "Password reset successfully! Please login with your new password.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}