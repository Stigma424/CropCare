
package com.example.cropcare

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ResetPasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        val etNewPassword =
            findViewById<EditText>(R.id.etNewPassword)

        val etConfirmNewPassword =
            findViewById<EditText>(R.id.etConfirmNewPassword)

        val btnSavePassword =
            findViewById<Button>(R.id.btnSavePassword)

        val btnBack =
            findViewById<ImageView>(R.id.btnBack)

        // Do not allow a password change without
        // successful server-side identity verification.
        etNewPassword.isEnabled = false
        etConfirmNewPassword.isEnabled = false

        btnSavePassword.text = "Reset Unavailable"
        btnSavePassword.isEnabled = false

        btnBack.setOnClickListener {
            finish()
        }

        Toast.makeText(
            this,
            "Password recovery requires email verification. " +
                    "This feature is being updated.",
            Toast.LENGTH_LONG
        ).show()
    }
}
