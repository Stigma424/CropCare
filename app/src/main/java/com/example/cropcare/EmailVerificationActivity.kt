package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmailVerificationActivity : AppCompatActivity() {
    private var request: ApiRequestHandle? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_email_verification)
        val email = RecoverySession.email
        if (email == null) { finish(); return }
        val code = findViewById<EditText>(R.id.etCode)
        code.isSaveEnabled = false; code.inputType = InputType.TYPE_CLASS_NUMBER
        code.filters = arrayOf(InputFilter.LengthFilter(6))
        val submit = findViewById<TextView>(R.id.btnSubmit)
        val resend = Button(this).apply { text = "Resend code" }
        (code.parent as LinearLayout).addView(resend)
        fun busy(value: Boolean) { submit.isEnabled = !value; resend.isEnabled = !value }
        submit.setOnClickListener {
            val value = code.text.toString()
            if (!FarmerValidation.code(value)) { code.error = "Enter the six-digit code"; return@setOnClickListener }
            busy(true)
            request = CropCareFarmerApi.verifyCode(email, value) { result ->
                busy(false)
                val ticket = result.data?.optString("resetTicket").orEmpty()
                if (result.successful && Regex("^[A-Fa-f0-9]{64}$").matches(ticket)) {
                    RecoverySession.verified(ticket); code.text.clear()
                    startActivity(Intent(this, ResetPasswordActivity::class.java)); finish()
                } else Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
        resend.setOnClickListener {
            busy(true)
            request = CropCareFarmerApi.requestCode(email) { result ->
                busy(false); code.text.clear(); Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
            }
        }
        findViewById<TextView>(R.id.btnCancel).setOnClickListener { RecoverySession.clear(); finish() }
    }
    override fun onDestroy() { request?.cancel(); super.onDestroy() }
}
