package com.example.cropcare

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException

class RegisterActivity : AppCompatActivity() {
    private val auth = FirebaseAuth.getInstance()
    private var request: ApiRequestHandle? = null
    private lateinit var submit: TextView
    private fun field(id: Int) = findViewById<EditText>(id).text.toString()
    private fun show(message: String) { if (!isFinishing && !isDestroyed) {
        submit.isEnabled = true; Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    } }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_register)
        submit = findViewById(R.id.btnRegister)
        listOf(R.id.etPassword, R.id.etConfirmPassword).forEach { findViewById<EditText>(it).isSaveEnabled = false }
        findViewById<TextView>(R.id.btnBack).setOnClickListener { finish() }
        submit.setOnClickListener {
            val first = field(R.id.etFirstName).trim(); val last = field(R.id.etLastName).trim()
            val address = field(R.id.etAddress).trim(); val username = field(R.id.etUsername).trim()
            val email = field(R.id.etEmail).trim(); val password = field(R.id.etPassword)
            if (first.length !in 1..80 || last.length !in 1..80 || address.length !in 1..250 ||
                !FarmerValidation.username(username) || email.length > 254 || !Patterns.EMAIL_ADDRESS.matcher(email).matches() ||
                !FarmerValidation.password(password) || password != field(R.id.etConfirmPassword)) {
                show("Enter valid names, address, username (3–30 letters, digits, . _ -), email and matching passwords (8–128 characters).")
                return@setOnClickListener
            }
            submit.isEnabled = false
            fun finishProfile() {
                if (isFinishing || isDestroyed) return
                request = CropCareHttpClient.request("GET", "/api/account/me") { profile ->
                    if (onboardingDecision(profile, auth.currentUser?.uid) == OnboardingDecision.COMPLETE) {
                        if (profile.data?.optString("userId") == auth.currentUser?.uid) complete()
                        else show("Could not confirm your profile. Please try again.")
                    } else if (onboardingDecision(profile, auth.currentUser?.uid) == OnboardingDecision.CREATE_PROFILE) {
                        request = CropCareFarmerApi.onboarding(first, last, address, username) { result ->
                            if (result.successful) complete()
                            else if (result.statusCode == 409) {
                                request = CropCareHttpClient.request("GET", "/api/account/me") { check ->
                                    if (onboardingDecision(check, auth.currentUser?.uid) == OnboardingDecision.COMPLETE) complete()
                                    else show(result.message + " Your Auth account is retained; retry with the same email/password and an available username.")
                                }
                            } else show(result.message + " Retry with the same email/password to finish your profile.")
                        }
                    } else show(profile.message)
                }
            }
            auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { created ->
                if (created.isSuccessful) finishProfile()
                else if (created.exception is FirebaseAuthUserCollisionException) {
                    auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { signedIn ->
                        if (signedIn.isSuccessful) finishProfile() else show("Unable to create or resume this account. Check your credentials.")
                    }
                } else show("Could not create your account. Check your connection and account details.")
            }
        }
    }
    private fun complete() {
        if (isFinishing || isDestroyed) return
        auth.signOut(); RecoverySession.clear()
        Toast.makeText(this, "Profile ready. Sign in with your username and password.", Toast.LENGTH_LONG).show()
        startActivity(Intent(this, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
        finish()
    }
    override fun onDestroy() { request?.cancel(); super.onDestroy() }
}
