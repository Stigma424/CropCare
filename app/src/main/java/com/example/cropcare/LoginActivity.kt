
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var btnLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etUsername =
            findViewById<EditText>(R.id.etLoginUsername)

        val etPassword =
            findViewById<EditText>(R.id.etLoginPassword)

        btnLogin = findViewById(R.id.btnLogin)

        val tvForgotPassword =
            findViewById<TextView>(R.id.tvForgotPassword)

        val tvRegister =
            findViewById<TextView>(R.id.tvRegister)

        btnLogin.setOnClickListener {
            val identifier =
                etUsername.text.toString().trim()

            val password =
                etPassword.text.toString()

            if (identifier.isBlank() ||
                password.isBlank()
            ) {
                showMessage(
                    "Enter your username/email and password."
                )
                return@setOnClickListener
            }

            btnLogin.isEnabled = false

            if (identifier.contains("@")) {
                // Direct Firebase Authentication login.
                signIn(identifier, password)
            } else {
                // Existing username compatibility.
                resolveUsername(identifier, password)
            }
        }

        tvForgotPassword.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    ForgotPasswordActivity::class.java
                )
            )
        }

        tvRegister.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    RegisterActivity::class.java
                )
            )
        }
    }

    private fun resolveUsername(
        username: String,
        password: String
    ) {
        db.collection("users")
            .whereEqualTo("username", username)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->

                if (documents.isEmpty) {
                    btnLogin.isEnabled = true
                    showMessage("Invalid username or password.")
                    return@addOnSuccessListener
                }

                val email = documents.documents.first()
                    .getString("email")

                if (email.isNullOrBlank()) {
                    btnLogin.isEnabled = true
                    showMessage("Account email was not found.")
                    return@addOnSuccessListener
                }

                signIn(email, password)
            }
            .addOnFailureListener { error ->
                btnLogin.isEnabled = true

                showMessage(
                    "Login failed: ${error.message}"
                )
            }
    }

    private fun signIn(
        email: String,
        password: String
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { result ->

                if (result.isSuccessful) {
                    showMessage("Login successful!")

                    val intent = Intent(
                        this,
                        DashboardActivity::class.java
                    )

                    intent.flags =
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TASK

                    startActivity(intent)
                    finish()
                } else {
                    btnLogin.isEnabled = true
                    showMessage("Invalid username/email or password.")
                }
            }
    }

    private fun showMessage(message: String) {
        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}
