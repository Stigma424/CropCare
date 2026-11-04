package com.example.cropcare

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        auth = FirebaseAuth.getInstance()

        val etCurrentPassword = findViewById<EditText>(R.id.EditOldPassword)
        val etNewPassword = findViewById<EditText>(R.id.EditNewPassword)
        val btnSave = findViewById<Button>(R.id.btnUpdatePassword)
        val btnCancel = findViewById<Button>(R.id.cancelButton)
        val btnBack = findViewById<View>(R.id.btnBack)

        listOf(R.id.EditOldPassword, R.id.EditNewPassword, R.id.EditConfirmNewPassword).forEach { findViewById<EditText>(it).isSaveEnabled = false }
        btnSave.setOnClickListener {
            val currentPass = etCurrentPassword.text.toString()
            val newPass = etNewPassword.text.toString()

            if (currentPass.isEmpty() || newPass.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!FarmerValidation.password(newPass) || newPass != findViewById<EditText>(R.id.EditConfirmNewPassword).text.toString()) {
                Toast.makeText(this, "Enter matching new passwords of 8–128 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = auth.currentUser
            if (user != null && user.email != null) {
                btnSave.isEnabled = false
                val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

                user.reauthenticate(credential)
                    .addOnCompleteListener reauth@ { reauthTask ->
                        if (isFinishing || isDestroyed) return@reauth
                        if (reauthTask.isSuccessful) {
                            user.updatePassword(newPass)
                                .addOnCompleteListener update@ { updateTask ->
                                    if (isFinishing || isDestroyed) return@update
                                    btnSave.isEnabled = true
                                    if (updateTask.isSuccessful) {
                                        Toast.makeText(this, "Password updated successfully!", Toast.LENGTH_SHORT).show()
                                        finish()
                                    } else {
                                        Toast.makeText(this, "Failed to update password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } else {
                            btnSave.isEnabled = true
                            Toast.makeText(this, "Incorrect current password", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please log in again.", Toast.LENGTH_LONG).show()
            }
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
