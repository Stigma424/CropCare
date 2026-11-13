
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
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {

    private val auth = FirebaseAuth.getInstance()


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
        PasswordVisibility.bind(this, R.id.etLoginPassword, R.id.ivTogglePassword)

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
                CropCareHttpClient.request("POST", "/api/auth/login",
                    JSONObject().put("username", identifier).put("password", passwordValue),
                    authenticated = false) { result ->
                    val customToken = result.data?.optString("customToken").orEmpty()
                    if (!result.successful || customToken.isBlank()) {
                        btnLogin.isEnabled = true
                        show(if (result.statusCode == 401) "Invalid username or password." else result.message)
                    } else {
                        auth.signInWithCustomToken(customToken).addOnCompleteListener { task ->
                            if (task.isSuccessful) completeSignIn() else {
                                btnLogin.isEnabled = true
                                show("Sign-in is temporarily unavailable. Please try again.")
                            }
                        }
                    }
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

            completeSignIn()
        }
    }

    private fun completeSignIn() {
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
