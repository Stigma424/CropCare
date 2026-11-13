package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class ForgotPasswordActivity : AppCompatActivity() {
    private var request: ApiRequestHandle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_forgot_password)
        RecoverySession.clear()
        val email = findViewById<EditText>(R.id.etForgotEmail)
        val submit = findViewById<Button>(R.id.btnSubmitForgot)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        submit.setOnClickListener {
            val value = email.text.toString().trim()
            if (value.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(value).matches()) {
                email.error = "Enter a valid email address"; return@setOnClickListener
            }
            submit.isEnabled = false
            request = CropCareFarmerApi.requestCode(value) { result ->
                submit.isEnabled = true
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                if (result.successful) {
                    if (result.data?.optString("deliveryMode") == "firebase_link") {
                        AlertDialog.Builder(this)
                            .setTitle("Check your email")
                            .setMessage(result.message)
                            .setPositiveButton("Back to sign in") { _, _ ->
                                startActivity(Intent(this, LoginActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                })
                                finish()
                            }.show()
                        return@requestCode
                    }
                    RecoverySession.start(value)
                    startActivity(Intent(this, EmailVerificationActivity::class.java)); finish()
                }
            }
        }
    }
    override fun onDestroy() { request?.cancel(); super.onDestroy() }
}
