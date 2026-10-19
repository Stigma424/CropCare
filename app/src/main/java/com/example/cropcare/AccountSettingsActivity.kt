
package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AccountSettingsActivity : AppCompatActivity() {

    private lateinit var tvFirstName: TextView
    private lateinit var tvMiddleName: TextView
    private lateinit var tvLastName: TextView
    private lateinit var tvAccountEmail: TextView
    private lateinit var tvAccountPhone: TextView
    private lateinit var btnEditProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_account_settings)

        tvFirstName = findViewById(R.id.tvFirstName)
        tvMiddleName = findViewById(R.id.tvMiddleName)
        tvLastName = findViewById(R.id.tvLastName)
        tvAccountEmail = findViewById(R.id.tvAccountEmail)
        tvAccountPhone = findViewById(R.id.tvAccountPhone)

        btnEditProfile = findViewById(R.id.btnEditProfile)

        val btnChangePassword =
            findViewById<Button>(R.id.btnChangePassword)

        val btnBack = findViewById<View>(R.id.btnBack)
        val tvDate = findViewById<TextView>(R.id.tvDate)

        tvDate.text = SimpleDateFormat(
            "MMMM d, yyyy",
            Locale.getDefault()
        ).format(Date())

        btnEditProfile.setOnClickListener {
            startActivity(
                Intent(this, EditProfileActivity::class.java)
            )
        }

        btnChangePassword.setOnClickListener {
            startActivity(
                Intent(this, ChangePasswordActivity::class.java)
            )
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()

        // Refresh every time this page becomes visible,
        // including after returning from Edit Profile.
        loadAccountProfile()
    }

    private fun loadAccountProfile() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        btnEditProfile.isEnabled = false

        tvFirstName.text = "Loading..."
        tvMiddleName.text = "Loading..."
        tvLastName.text = "Loading..."
        tvAccountPhone.text = "Loading..."
        tvAccountEmail.text = "Loading..."

        CropCareApiClient.getMyProfile { result ->

            if (isFinishing || isDestroyed) {
                return@getMyProfile
            }

            btnEditProfile.isEnabled = true

            if (result.successful && result.data != null) {
                displayProfile(result.data)
            } else {
                tvFirstName.text = "Unavailable"
                tvMiddleName.text = "—"
                tvLastName.text = "—"
                tvAccountPhone.text = "—"
                tvAccountEmail.text =
                    FirebaseAuth.getInstance().currentUser?.email
                        ?: "Unavailable"

                Toast.makeText(
                    this,
                    result.message,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayProfile(profile: JSONObject) {
        tvFirstName.text = readField(profile, "firstName")
        tvMiddleName.text = readField(profile, "middleName")
        tvLastName.text = readField(profile, "lastName")
        tvAccountPhone.text = readField(profile, "phoneNumber")

        val accountEmail = readField(profile, "email")

        tvAccountEmail.text = if (accountEmail == "Not set") {
            FirebaseAuth.getInstance().currentUser?.email
                ?: "Not set"
        } else {
            accountEmail
        }
    }

    private fun readField(
        json: JSONObject,
        field: String
    ): String {
        if (json.isNull(field)) return "Not set"

        return json.optString(field)
            .takeIf { it.isNotBlank() }
            ?: "Not set"
    }
}
