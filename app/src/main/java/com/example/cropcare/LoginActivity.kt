package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.util.Patterns
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val etLoginInput = findViewById<EditText>(R.id.etLoginUsername)
        val etPassword = findViewById<EditText>(R.id.etLoginPassword)
        val ivTogglePassword = findViewById<ImageView>(R.id.ivTogglePassword)
        val btnLogin = findViewById<TextView>(R.id.btnLogin)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val tvRegister = findViewById<TextView>(R.id.tvRegister)

        // Password visibility toggle
        ivTogglePassword.setOnClickListener {
            isPasswordVisible = !isPasswordVisible
            if (isPasswordVisible) {
                etPassword.transformationMethod = HideReturnsTransformationMethod.getInstance()
            } else {
                etPassword.transformationMethod = PasswordTransformationMethod.getInstance()
            }
            etPassword.setSelection(etPassword.text.length)
        }

        btnLogin.setOnClickListener {
            val loginInput = etLoginInput.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (loginInput.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both Email/Username and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if loginInput matches a Username in Firestore
            db.collection("users")
                .whereEqualTo("username", loginInput)
                .get()
                .addOnSuccessListener { usernameDocs ->
                    if (!usernameDocs.isEmpty) {
                        val email = usernameDocs.documents[0].getString("email") ?: loginInput
                        performSignIn(email, password)
                    } else {
                        // Check if loginInput matches an Email in Firestore
                        db.collection("users")
                            .whereEqualTo("email", loginInput)
                            .get()
                            .addOnSuccessListener { emailDocs ->
                                if (!emailDocs.isEmpty) {
                                    val email = emailDocs.documents[0].getString("email") ?: loginInput
                                    performSignIn(email, password)
                                } else {
                                    // If not found in Firestore but looks like an email, attempt direct auth
                                    if (Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                                        performSignIn(loginInput, password)
                                    } else {
                                        Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                if (Patterns.EMAIL_ADDRESS.matcher(loginInput).matches()) {
                                    performSignIn(loginInput, password)
                                } else {
                                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun performSignIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onLoginSuccess()
                } else {
                    // Fallback: Check if entered password matches updated password in Firestore database
                    db.collection("users")
                        .whereEqualTo("email", email)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (!documents.isEmpty) {
                                val savedPassword = documents.documents[0].getString("password")
                                if (savedPassword == pass) {
                                    // Password matches Firestore reset password!
                                    onLoginSuccess()
                                } else {
                                    Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Incorrect Password", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    private fun onLoginSuccess() {
        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}