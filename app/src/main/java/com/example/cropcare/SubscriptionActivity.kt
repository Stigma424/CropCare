package com.example.cropcare

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private lateinit var tvSubscriptionStatus: TextView
    private lateinit var tvStatusDescription: TextView
    private lateinit var layoutPayment: LinearLayout
    private lateinit var etGcashRef: EditText
    private lateinit var btnSubscribe: Button
    private lateinit var btnCancelSubscription: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        tvSubscriptionStatus = findViewById(R.id.tvSubscriptionStatus)
        tvStatusDescription = findViewById(R.id.tvStatusDescription)
        layoutPayment = findViewById(R.id.layoutPayment)
        etGcashRef = findViewById(R.id.etGcashRef)
        btnSubscribe = findViewById(R.id.btnSubscribe)
        btnCancelSubscription = findViewById(R.id.btnCancelSubscription)

        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { finish() }

        btnSubscribe.setOnClickListener {
            val refNo = etGcashRef.text.toString().trim()
            if (refNo.length < 6) {
                Toast.makeText(this, "Please enter a valid GCash reference number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            activateSubscription(refNo)
        }

        btnCancelSubscription.setOnClickListener {
            cancelSubscription()
        }

        checkSubscriptionStatus()
    }

    private fun checkSubscriptionStatus() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val isSubscribed = doc.getBoolean("isSubscribed") ?: false

                if (isSubscribed) {
                    tvSubscriptionStatus.text = "Current Plan: PRO SUBSCRIBED USER"
                    tvSubscriptionStatus.setTextColor(Color.parseColor("#1B5E20"))
                    tvStatusDescription.text = "You have full access to all customized corn fertilizer kg formulas, growth-stage guides, and automated warnings."
                    layoutPayment.visibility = View.GONE
                    btnCancelSubscription.visibility = View.VISIBLE
                } else {
                    tvSubscriptionStatus.text = "Current Plan: FREE USER"
                    tvSubscriptionStatus.setTextColor(Color.parseColor("#C62828"))
                    tvStatusDescription.text = "Upgrade to unlock exact corn fertilization weight calculations (Urea/Complete/Potash), soil pH Lime guidance, and priority alerts!"
                    layoutPayment.visibility = View.VISIBLE
                    btnCancelSubscription.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                tvSubscriptionStatus.text = "Current Plan: FREE USER"
            }
    }

    private fun activateSubscription(refNo: String) {
        val userId = auth.currentUser?.uid ?: return

        val updateData = hashMapOf<String, Any>(
            "isSubscribed" to true,
            "gcashRefNo" to refNo,
            "subscribedAt" to System.currentTimeMillis()
        )

        db.collection("users").document(userId)
            .set(updateData, SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Subscription Activated! You are now a PRO user.", Toast.LENGTH_LONG).show()
                checkSubscriptionStatus()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Activation failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelSubscription() {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("isSubscribed", false)
            .addOnSuccessListener {
                Toast.makeText(this, "Subscription cancelled.", Toast.LENGTH_SHORT).show()
                checkSubscriptionStatus()
            }
    }
}
