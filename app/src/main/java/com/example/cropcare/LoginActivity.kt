
package com.example.cropcare

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private lateinit var btnLogin: TextView

    private val askNotificationPermission =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {
            openDashboard()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val username =
            findViewById<EditText>(
                R.id.etLoginUsername
            )

        val password =
            findViewById<EditText>(
                R.id.etLoginPassword
            )

        password.isSaveEnabled = false
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {

            val identifier =
                username.text.toString().trim()

            val passwordValue =
                password.text.toString()

            if (
                identifier.isBlank() ||
                passwordValue.isBlank()
            ) {
                show(
                    "Enter your username/email and password."
                )
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            if (identifier.contains('@')) {
                signIn(identifier, passwordValue)
            } else {
                db.collection("users")
                    .whereEqualTo(
                        "username",
                        identifier
                    )
                    .limit(1)
                    .get()
                    .addOnSuccessListener { docs ->

                        val email =
                            docs.documents
                                .firstOrNull()
                                ?.getString("email")

                        if (email.isNullOrBlank()) {
                            btnLogin.isEnabled = true
                            show(
                                "Invalid username or password."
                            )
                        } else {
                            signIn(
                                email,
                                passwordValue
                            )
                        }
                    }
                    .addOnFailureListener { error ->

                        btnLogin.isEnabled = true

                        show(
                            "Could not resolve username: " +
                                    "${error.message}"
                        )
                    }
            }
        }

        findViewById<TextView>(
            R.id.tvForgotPassword
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )
            )
        }

        findViewById<TextView>(
            R.id.tvRegister
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun signIn(
        email: String,
        password: String
    ) {
        auth.signInWithEmailAndPassword(
            email,
            password
        ).addOnCompleteListener { task ->

            if (!task.isSuccessful) {

                btnLogin.isEnabled = true

                show(
                    "Invalid username/email or password."
                )

                return@addOnCompleteListener
            }

            // Store the FCM token in users/{uid}.
            CropCareFcmRegistration.register()

            if (
                Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                askNotificationPermission.launch(
                    Manifest.permission.POST_NOTIFICATIONS
                )
            } else {
                openDashboard()
            }
        }
    }

    private fun openDashboard() {
        startActivity(
            Intent(
                this,
                DashboardActivity::class.java
            ).apply {
                flags =
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )

        finish()
    }

    private fun show(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
