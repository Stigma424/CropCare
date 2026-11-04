package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ConfirmLogoutActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_logout)

        auth = FirebaseAuth.getInstance()

        val btnConfirmLogout = findViewById<TextView>(R.id.btnConfirmLogout)
        val btnCancelLogout = findViewById<TextView>(R.id.btnCancelLogout)

        btnConfirmLogout.setOnClickListener {
            btnConfirmLogout.isEnabled = false
            CropCareFcmRegistration.detach { detached ->
            if (isFinishing || isDestroyed) return@detach
            if (!detached) android.widget.Toast.makeText(this, "Signed out, but push-token removal failed. This device may still receive account alerts until token registration is updated.", android.widget.Toast.LENGTH_LONG).show()
            RecoverySession.clear()
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            }
        }

        btnCancelLogout.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            finish()
        }
    }
}
