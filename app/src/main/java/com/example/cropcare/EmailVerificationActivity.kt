
package com.example.cropcare

import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class EmailVerificationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_email_verification)

        val etCode = findViewById<EditText>(R.id.etCode)
        val btnSubmit = findViewById<TextView>(R.id.btnSubmit)
        val btnCancel = findViewById<TextView>(R.id.btnCancel)

        etCode.isEnabled = false

        btnSubmit.text = "Verification Unavailable"
        btnSubmit.isEnabled = false

        btnCancel.setOnClickListener {
            finish()
        }

        Toast.makeText(
            this,
            "Secure email verification is not available yet.",
            Toast.LENGTH_LONG
        ).show()
    }
}
