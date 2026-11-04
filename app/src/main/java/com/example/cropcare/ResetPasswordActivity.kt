package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResetPasswordActivity : AppCompatActivity() {
    private var request: ApiRequestHandle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_reset_password)
        if (RecoverySession.ticket() == null || RecoverySession.email == null) { restart(); return }
        val password = findViewById<EditText>(R.id.etNewPassword)
        val confirmation = findViewById<EditText>(R.id.etConfirmNewPassword)
        password.isSaveEnabled = false; confirmation.isSaveEnabled = false
        val save = findViewById<Button>(R.id.btnSavePassword)
        findViewById<View>(R.id.btnBack).setOnClickListener { RecoverySession.clear(); finish() }
        save.setOnClickListener {
            val value = password.text.toString()
            if (!FarmerValidation.password(value) || value != confirmation.text.toString()) {
                confirmation.error = "Enter matching passwords of 8–128 characters"; return@setOnClickListener
            }
            val ticket = RecoverySession.ticket(); val email = RecoverySession.email
            if (ticket == null || email == null) { restart(); return@setOnClickListener }
            save.isEnabled = false
            request = CropCareFarmerApi.reset(email, ticket, value) { result ->
                save.isEnabled = true
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                if (result.successful) {
                    RecoverySession.clear(); password.text.clear(); confirmation.text.clear()
                    startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)); finish()
                } else if (result.statusCode == 400 || result.statusCode == 503 || result.statusCode == 0) restart()
            }
        }
    }
    private fun restart() {
        RecoverySession.clear()
        Toast.makeText(this, "Request a new recovery code to continue.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, ForgotPasswordActivity::class.java)); finish()
    }
    override fun onDestroy() { request?.cancel(); super.onDestroy() }
}
