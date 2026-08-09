package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class EditProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etEditFirstName = findViewById<EditText>(R.id.etEditFirstName)
        val etEditMiddleName = findViewById<EditText>(R.id.etEditMiddleName)
        val etEditLastName = findViewById<EditText>(R.id.etEditLastName)
        val etEditAddress = findViewById<EditText>(R.id.etEditAddress)
        val etEditPhone = findViewById<EditText>(R.id.etEditPhone)
        val btnSaveProfile = findViewById<Button>(R.id.btnSaveProfile)
        val btnBack = findViewById<Button>(R.id.btnBack)

        val userId = auth.currentUser?.uid ?: ""

        // Pre-fill current details
        if (userId.isNotEmpty()) {
            db.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        etEditFirstName.setText(document.getString("firstName") ?: "")
                        etEditMiddleName.setText(document.getString("middleName") ?: "")
                        etEditLastName.setText(document.getString("lastName") ?: "")
                        etEditAddress.setText(document.getString("address") ?: "")
                        etEditPhone.setText(document.getString("phoneNumber") ?: "")
                    }
                }
        }

        btnSaveProfile.setOnClickListener {
            val updatedFName = etEditFirstName.text.toString().trim()
            val updatedMName = etEditMiddleName.text.toString().trim()
            val updatedLName = etEditLastName.text.toString().trim()
            val updatedAddress = etEditAddress.text.toString().trim()
            val updatedPhone = etEditPhone.text.toString().trim()

            if (updatedFName.isEmpty() || updatedLName.isEmpty() || updatedAddress.isEmpty()) {
                Toast.makeText(this, "First Name, Last Name, and Address cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mapOf(
                "firstName" to updatedFName,
                "middleName" to updatedMName,
                "lastName" to updatedLName,
                "address" to updatedAddress,
                "phoneNumber" to updatedPhone
            )

            db.collection("users").document(userId)
                .update(updates)
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}