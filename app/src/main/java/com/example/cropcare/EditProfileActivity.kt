
package com.example.cropcare

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    private lateinit var etFirstName: EditText
    private lateinit var etMiddleName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etAddress: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSaveProfile: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_profile)

        etFirstName = findViewById(R.id.etEditFirstName)
        etMiddleName = findViewById(R.id.etEditMiddleName)
        etLastName = findViewById(R.id.etEditLastName)
        etAddress = findViewById(R.id.etEditAddress)
        etPhone = findViewById(R.id.etEditPhone)

        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnBackForm = findViewById<Button>(R.id.btnBackForm)

        btnBack.setOnClickListener {
            finish()
        }

        btnBackForm.setOnClickListener {
            finish()
        }

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        loadExistingProfile()
    }

    private fun loadExistingProfile() {
        if (FirebaseAuth.getInstance().currentUser == null) {
            Toast.makeText(
                this,
                "Please log in again.",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Loading Profile..."

        // Load the existing profile through ASP.NET Core.
        CropCareApiClient.getMyProfile { result ->

            if (isFinishing || isDestroyed) {
                return@getMyProfile
            }

            if (result.successful && result.data != null) {
                displayExistingProfile(result.data)

                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Changes"
            } else {
                btnSaveProfile.isEnabled = false
                btnSaveProfile.text = "Profile Unavailable"

                Toast.makeText(
                    this,
                    "Failed to load profile: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun displayExistingProfile(profile: JSONObject) {
        etFirstName.setText(readField(profile, "firstName"))
        etMiddleName.setText(readField(profile, "middleName"))
        etLastName.setText(readField(profile, "lastName"))
        etAddress.setText(readField(profile, "address"))
        etPhone.setText(readField(profile, "phoneNumber"))
    }

    private fun readField(
        json: JSONObject,
        field: String
    ): String {
        if (json.isNull(field)) return ""
        return json.optString(field, "")
    }

    private fun saveProfile() {
        val firstName = etFirstName.text.toString().trim()
        val middleName = etMiddleName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val address = etAddress.text.toString().trim()
        val phone = etPhone.text.toString().trim()

        // Keep the original CropCare validation requirements.
        if (firstName.isEmpty() ||
            lastName.isEmpty() ||
            address.isEmpty()
        ) {
            Toast.makeText(
                this,
                "First Name, Last Name, and Address cannot be empty.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (firstName.length > 80 ||
            middleName.length > 80 ||
            lastName.length > 80
        ) {
            Toast.makeText(
                this,
                "Name fields cannot exceed 80 characters.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (address.length > 250) {
            Toast.makeText(
                this,
                "Address cannot exceed 250 characters.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (phone.length > 30) {
            Toast.makeText(
                this,
                "Phone number cannot exceed 30 characters.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        val updatedProfile = CropCareApiClient.ProfileUpdate(
            firstName = firstName,
            middleName = middleName,
            lastName = lastName,
            address = address,
            phoneNumber = phone
        )

        btnSaveProfile.isEnabled = false
        btnSaveProfile.text = "Saving..."

        // Save through PATCH /api/account/me.
        CropCareApiClient.updateMyProfile(updatedProfile) { result ->

            if (isFinishing || isDestroyed) {
                return@updateMyProfile
            }

            if (result.successful) {
                Toast.makeText(
                    this,
                    "Profile updated successfully!",
                    Toast.LENGTH_SHORT
                ).show()

                setResult(Activity.RESULT_OK)
                finish()
            } else {
                btnSaveProfile.isEnabled = true
                btnSaveProfile.text = "Save Changes"

                Toast.makeText(
                    this,
                    "Update failed: ${result.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
