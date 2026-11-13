
package com.example.cropcare

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class ApiConnectionActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var detailsText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var retryButton: Button

    private val mainHandler = Handler(Looper.getMainLooper())

    private val apiBaseUrl: String
        get() = CropCareHttpClient.baseUrl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!BuildConfig.DEBUG) { finish(); return }

        createScreen()
        startConnectionTest()
    }

    private fun createScreen() {
        val padding = (24 * resources.displayMetrics.density).toInt()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
        }

        val title = TextView(this).apply {
            text = "CropCare API Connection"
            textSize = 24f
            setPadding(0, 0, 0, 16)
        }

        val subtitle = TextView(this).apply {
            text = "Checking the connection between CropCare, ASP.NET Core, and Firebase."
            textSize = 15f
            setPadding(0, 0, 0, 24)
        }

        progressBar = ProgressBar(this).apply {
            visibility = View.VISIBLE
        }

        statusText = TextView(this).apply {
            text = "Starting connection test..."
            textSize = 19f
            setPadding(0, 24, 0, 12)
        }

        detailsText = TextView(this).apply {
            text = "API address: $apiBaseUrl"
            textSize = 14f
            setTextIsSelectable(true)
            setPadding(0, 0, 0, 24)
        }

        retryButton = Button(this).apply {
            text = "Retry Connection"
            isEnabled = false
            setOnClickListener {
                startConnectionTest()
            }
        }

        val continueButton = Button(this).apply {
            text = "Continue to Dashboard (Testing Only)"
            setOnClickListener {
                openDashboard()
            }
        }

        layout.addView(title)
        layout.addView(subtitle)
        layout.addView(progressBar)
        layout.addView(statusText)
        layout.addView(detailsText)
        layout.addView(retryButton)
        layout.addView(continueButton)

        val scrollView = ScrollView(this).apply {
            addView(layout)
        }

        setContentView(scrollView)
    }

    private fun startConnectionTest() {
        setLoading(
            "Step 1/2: Connecting to ASP.NET Core...",
            "Testing GET $apiBaseUrl/api/health"
        )

        Thread {
            val result = performGet(
                "$apiBaseUrl/api/health",
                null
            )

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (result.status == 200) {
                    verifyFirebaseAccount()
                } else {
                    showFailure(
                        "API Health Check Failed",
                        "HTTP: ${result.status}\n" +
                                "Address: $apiBaseUrl\n\n" +
                                result.body
                    )
                }
            }
        }.start()
    }

    private fun verifyFirebaseAccount() {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            showFailure(
                "Firebase Login Missing",
                "No Firebase user is currently signed in."
            )
            return
        }

        setLoading(
            "Step 2/2: Verifying Firebase account...",
            "Requesting Firebase ID token for the ASP.NET Core API."
        )

        user.getIdToken(false)
            .addOnSuccessListener { result ->
                val token = result.token

                if (token.isNullOrBlank()) {
                    showFailure(
                        "Token Error",
                        "Firebase returned an empty ID token."
                    )
                    return@addOnSuccessListener
                }

                requestProfile(token)
            }
            .addOnFailureListener { error ->
                showFailure(
                    "Firebase Token Error",
                    error.message ?: "Could not obtain a Firebase ID token."
                )
            }
    }

    private fun requestProfile(token: String) {
        Thread {
            val result = performGet(
                "$apiBaseUrl/api/account/me",
                token
            )

            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread

                if (result.status == 200) {
                    val profile = try {
                        JSONObject(result.body)
                    } catch (_: Exception) {
                        null
                    }

                    val firstName = profile
                        ?.optString("firstName")
                        ?.takeIf { it.isNotBlank() }
                        ?: "Farmer"

                    progressBar.visibility = View.GONE
                    retryButton.isEnabled = false

                    statusText.text = "API Connection Successful!"

                    detailsText.text =
                        "ASP.NET Core: Connected\n" +
                                "Firebase Authentication: Verified\n" +
                                "Cloud Firestore Profile: Retrieved\n\n" +
                                "Welcome, $firstName!\n\n" +
                                "Opening Dashboard..."

                    mainHandler.postDelayed({
                        if (!isFinishing && !isDestroyed) {
                            openDashboard()
                        }
                    }, 1800)
                } else {
                    showFailure(
                        "Firebase Profile Check Failed",
                        "HTTP: ${result.status}\n\n" +
                                result.body
                    )
                }
            }
        }.start()
    }

    private fun performGet(
        endpoint: String,
        firebaseToken: String?
    ): ApiResult {
        var connection: HttpURLConnection? = null

        return try {
            connection = URL(endpoint).openConnection()
                    as HttpURLConnection

            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            if (!firebaseToken.isNullOrBlank()) {
                connection.setRequestProperty(
                    "Authorization",
                    "Bearer $firebaseToken"
                )
            }

            val status = connection.responseCode

            val responseStream = if (status in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }

            val body = responseStream
                ?.bufferedReader()
                ?.use { it.readText() }
                ?: "No response body."

            ApiResult(status, body)

        } catch (error: Exception) {
            ApiResult(
                -1,
                "${error.javaClass.simpleName}: ${error.message}"
            )
        } finally {
            connection?.disconnect()
        }
    }

    private fun setLoading(
        status: String,
        details: String
    ) {
        progressBar.visibility = View.VISIBLE
        retryButton.isEnabled = false

        statusText.text = status
        detailsText.text = details
    }

    private fun showFailure(
        status: String,
        details: String
    ) {
        progressBar.visibility = View.GONE
        retryButton.isEnabled = true

        statusText.text = status
        detailsText.text = details
    }

    private fun openDashboard() {
        val intent = Intent(
            this,
            DashboardActivity::class.java
        )

        intent.flags =
            Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK

        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        mainHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private data class ApiResult(
        val status: Int,
        val body: String
    )
}
