package com.example.cropcare

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class SubscriptionActivity : AppCompatActivity() {
    private var request: ApiRequestHandle? = null
    private lateinit var status: TextView
    private lateinit var description: TextView
    private lateinit var changePlan: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)
        status = findViewById(R.id.tvSubscriptionStatus)
        description = findViewById(R.id.tvStatusDescription)
        changePlan = findViewById(R.id.btnSubscribe)
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        status.setOnClickListener { if (changePlan.isEnabled) load() }
    }

    override fun onStart() {
        super.onStart()
        load()
    }

    private fun load() {
        changePlan.isEnabled = false
        status.text = "Checking subscription…"
        request = CropCareHttpClient.request("GET", "/api/subscriptions/me/demo", callback = ::showStatus)
    }

    private fun showStatus(result: FarmerApiResult) {
        val data = result.data
        if (!result.successful || data == null) {
            status.text = "Subscription unavailable"
            description.text = result.message
            changePlan.text = "Retry"
            changePlan.setOnClickListener { load() }
            changePlan.isEnabled = true
            return
        }
        val pro = data.optBoolean("isSubscribed")
        status.text = if (pro) "Current plan: Pro" else "Current plan: Free"
        description.text = "Switch plans to try CropCare features during the demo."
        changePlan.text = if (pro) "Switch to Free" else "Activate Pro"
        changePlan.isEnabled = true
        changePlan.setOnClickListener {
            changePlan.isEnabled = false
            request = CropCareHttpClient.request("PUT", "/api/subscriptions/me/demo",
                JSONObject().put("isSubscribed", !pro)) { updated ->
                if (updated.successful) showStatus(updated)
                else {
                    Toast.makeText(this, updated.message, Toast.LENGTH_LONG).show()
                    load()
                }
            }
        }
    }

    override fun onStop() {
        request?.cancel()
        super.onStop()
    }
}
