package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_settings)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val tvFullName = findViewById<TextView>(R.id.tvFullName)
        val tvAccountUsername = findViewById<TextView>(R.id.tvAccountUsername)
        val tvAccountEmail = findViewById<TextView>(R.id.tvAccountEmail)
        val tvAccountAddress = findViewById<TextView>(R.id.tvAccountAddress)
        val tvAccountPhone = findViewById<TextView>(R.id.tvAccountPhone)

        val btnEditProfile = findViewById<Button>(R.id.btnEditProfile)
        val btnChangePassword = findViewById<Button>(R.id.btnChangePassword)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val currentUser = auth.currentUser

        currentUser?.uid?.let { uid ->
            db.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstName") ?: ""
                        val middleName = document.getString("middleName") ?: ""
                        val lastName = document.getString("lastName") ?: ""
                        val username = document.getString("username") ?: "N/A"
                        val email = document.getString("email") ?: (currentUser.email ?: "N/A")
                        val address = document.getString("address") ?: "N/A"
                        val phone = document.getString("phoneNumber") ?: "Not set"

                        // Format full name including middle name if present
                        val fullName = if (middleName.isNotBlank()) {
                            "$firstName $middleName $lastName"
                        } else {
                            "$firstName $lastName"
                        }.trim()

                        tvFullName.text = "Name: $fullName"
                        tvAccountUsername.text = "Username: $username"
                        tvAccountEmail.text = "Email: $email"
                        tvAccountAddress.text = "Address: $address"
                        tvAccountPhone.text = "Phone: $phone"
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load details: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnEditProfile.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        btnChangePassword.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}