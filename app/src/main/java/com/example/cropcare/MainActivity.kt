package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, if (FirebaseAuth.getInstance().currentUser == null)
            LoginActivity::class.java else DashboardActivity::class.java))
        finish()
    }
}
