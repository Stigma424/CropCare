package com.example.cropcare

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class SubscriptionActivity : AppCompatActivity() {
    private var request: ApiRequestHandle? = null
    private var state = SubscriptionState.UNAVAILABLE
    private lateinit var status: TextView
    private lateinit var description: TextView
    private lateinit var reference: EditText
    private lateinit var submit: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_subscription)
        status = findViewById(R.id.tvSubscriptionStatus); description = findViewById(R.id.tvStatusDescription)
        reference = findViewById(R.id.etGcashRef); reference.isSaveEnabled = false
        submit = findViewById(R.id.btnSubscribe)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnCancelSubscription).visibility = View.GONE
        submit.setOnClickListener {
            if (!state.canSubmit) return@setOnClickListener
            val value = reference.text.toString().trim()
            if (!FarmerValidation.reference(value)) { reference.error = "Enter 8–20 digits"; return@setOnClickListener }
            submit.isEnabled = false
            request = CropCareFarmerApi.submitReference(value) { result ->
                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()
                if (result.successful) reference.text.clear()
                load()
            }
        }
        status.setOnClickListener { load() }
    }
    override fun onStart() { super.onStart(); load() }
    private fun load() {
        request?.cancel(); submit.isEnabled = false; state = SubscriptionState.UNAVAILABLE
        status.text = "Checking subscription…"
        request = CropCareFarmerApi.subscription { result ->
            state = if (result.successful) SubscriptionState.parse(result.data?.optString("status")) else SubscriptionState.UNAVAILABLE
            status.text = "Current plan: ${state.name} · tap to refresh"
            description.text = result.data?.optString("message") ?: result.message
            val lastFour = result.data?.optString("referenceLastFour").orEmpty()
            if (lastFour.isNotBlank()) description.append("\nReference ending $lastFour")
            val submitted = result.data?.optLong("submittedAtMs", 0L) ?: 0L
            if (submitted > 0) description.append("\nSubmitted: " + java.text.SimpleDateFormat("MMM d, yyyy h:mm a", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("Asia/Manila")
            }.format(java.util.Date(submitted)))
            reference.isEnabled = state.canSubmit; submit.isEnabled = state.canSubmit
            findViewById<View>(R.id.layoutPayment).visibility = if (state.canSubmit) View.VISIBLE else View.GONE
        }
    }
    override fun onStop() { request?.cancel(); super.onStop() }
}
