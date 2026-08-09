package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        db = FirebaseFirestore.getInstance()
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        val etNewPassword = findViewById<EditText>(R.id.etNewPassword)
        val etConfirmNewPassword = findViewById<EditText>(R.id.etConfirmNewPassword)
        val btnSavePassword = findViewById<Button>(R.id.btnSavePassword)

        btnSavePassword.setOnClickListener {
            val newPassword = etNewPassword.text.toString().trim()
            val confirmNewPassword = etConfirmNewPassword.text.toString().trim()

            if (newPassword.isEmpty() || confirmNewPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmNewPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Find user in Firestore by email and update password field in document
            db.collection("users")
                .whereEqualTo("email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val docId = documents.documents[0].id

                        db.collection("users").document(docId)
                            .update("password", newPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Password updated successfully in database!", Toast.LENGTH_LONG).show()
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "User email not found in database", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Database error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}