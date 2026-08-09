package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etFirstName = findViewById<EditText>(R.id.etFirstName)
        val etLastName = findViewById<EditText>(R.id.etLastName)
        val etAddress = findViewById<EditText>(R.id.etAddress)
        val etUsername = findViewById<EditText>(R.id.etUsername)
        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etReferralCode = findViewById<EditText>(R.id.etReferralCode)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val etConfirmPassword = findViewById<EditText>(R.id.etConfirmPassword)
        val btnRegister = findViewById<Button>(R.id.btnRegister)

        btnRegister.setOnClickListener {
            val firstName = etFirstName.text.toString().trim()
            val lastName = etLastName.text.toString().trim()
            val address = etAddress.text.toString().trim()
            val username = etUsername.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val refCode = etReferralCode.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            // 1. Inputs validation
            if (firstName.isEmpty() || lastName.isEmpty() || address.isEmpty() ||
                username.isEmpty() || email.isEmpty() || refCode.isEmpty() ||
                password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 2. Validate Referral Code in Firestore
            db.collection("referral_codes").document(refCode).get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getBoolean("active") == true) {
                        // Referral code is valid, proceed to register user
                        performFirebaseRegistration(
                            email, password, firstName, lastName, address, username, refCode
                        )
                    } else {
                        Toast.makeText(this, "Invalid or expired referral code", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error checking referral code: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun performFirebaseRegistration(
        email: String, pass: String, fName: String, lName: String,
        addr: String, uname: String, refCode: String
    ) {
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: ""

                    // Save User Details in Firestore using userId as Document Key
                    val userMap = hashMapOf(
                        "userId" to userId,
                        "firstName" to fName,
                        "lastName" to lName,
                        "address" to addr,
                        "username" to uname,
                        "email" to email,
                        "referralCodeUsed" to refCode,
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(userId)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration Successful!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user data: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Auth Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}