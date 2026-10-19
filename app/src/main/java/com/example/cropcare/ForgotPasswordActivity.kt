
package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ForgotPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etEmail =
            findViewById<EditText>(R.id.etForgotEmail)

        val btnSubmit =
            findViewById<Button>(R.id.btnSubmitForgot)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        // The previous recovery workflow did not verify
        // email codes or update Firebase Authentication.
        // Disable it until the secure email-code API is ready.
        etEmail.isEnabled = false

        btnSubmit.text = "Email Recovery Coming Soon"
        btnSubmit.isEnabled = false

        btnBack.setOnClickListener {
            finish()
        }

        Toast.makeText(
            this,
            "Email-code password recovery is temporarily unavailable.",
            Toast.LENGTH_LONG
        ).show()
    }
}
