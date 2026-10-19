
package com.example.cropcare

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvSubscriptionStatus: TextView
    private lateinit var tvStatusDescription: TextView
    private lateinit var layoutPayment: LinearLayout
    private lateinit var btnSubscribe: Button
    private lateinit var btnCancelSubscription: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvSubscriptionStatus =
            findViewById(R.id.tvSubscriptionStatus)

        tvStatusDescription =
            findViewById(R.id.tvStatusDescription)

        layoutPayment =
            findViewById(R.id.layoutPayment)

        btnSubscribe =
            findViewById(R.id.btnSubscribe)

        btnCancelSubscription =
            findViewById(R.id.btnCancelSubscription)

        val etGcashRef =
            findViewById<EditText>(R.id.etGcashRef)

        findViewById<ImageButton>(R.id.btnBack)
            .setOnClickListener {
                finish()
            }

        // Subscription approval must not happen in Android.
        // Hide the old GCash form and activation controls.
        layoutPayment.visibility = View.GONE
        btnCancelSubscription.visibility = View.GONE

        btnSubscribe.isEnabled = false
        btnCancelSubscription.isEnabled = false
        etGcashRef.isEnabled = false
    }

    override fun onResume() {
        super.onResume()
        loadSubscriptionStatus()
    }

    private fun loadSubscriptionStatus() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            showUnavailable("Please log in again.")
            return
        }

        tvSubscriptionStatus.text = "Checking Subscription..."
        tvStatusDescription.text = "Loading your subscription status."

        // Read from the server so manually approved changes
        // appear when the farmer reopens this screen.
        db.collection("users")
            .document(userId)
            .get(Source.SERVER)
            .addOnSuccessListener { document ->

                if (isFinishing || isDestroyed) {
                    return@addOnSuccessListener
                }

                if (!document.exists()) {
                    showUnavailable("Your user profile was not found.")
                    return@addOnSuccessListener
                }

                val isSubscribed =
                    document.getBoolean("isSubscribed") == true

                if (isSubscribed) {
                    tvSubscriptionStatus.text =
                        "Current Plan: PRO SUBSCRIBED USER"

                    tvSubscriptionStatus.setTextColor(
                        Color.parseColor("#1B5E20")
                    )

                    tvStatusDescription.text =
                        "Your Pro subscription has been approved " +
                                "by the CropCare project team."

                } else {
                    tvSubscriptionStatus.text =
                        "Current Plan: FREE USER"

                    tvSubscriptionStatus.setTextColor(
                        Color.parseColor("#C62828")
                    )

                    tvStatusDescription.text =
                        "Pro subscription requests are temporarily " +
                                "unavailable. Payment verification will be " +
                                "added in a future update."
                }
            }
            .addOnFailureListener { error ->

                if (isFinishing || isDestroyed) {
                    return@addOnFailureListener
                }

                showUnavailable(
                    error.message ?: "Could not load subscription status."
                )
            }
    }

    private fun showUnavailable(message: String) {
        tvSubscriptionStatus.text =
            "Subscription Status Unavailable"

        tvSubscriptionStatus.setTextColor(
            Color.parseColor("#C62828")
        )

        tvStatusDescription.text = message
    }
}
