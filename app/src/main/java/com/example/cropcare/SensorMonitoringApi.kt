
package com.example.cropcare

import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object SensorMonitoringApi {

    private val main = Handler(Looper.getMainLooper())

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // Physical phone with ADB reverse.
    private const val BASE = "http://127.0.0.1:5056"

    fun get(
        sensorId: String,
        callback: (JSONObject?, String?) -> Unit
    ) {
        val user =
            FirebaseAuth.getInstance().currentUser

        if (user == null) {
            main.post {
                callback(null, "Please log in again.")
            }
            return
        }

        if (sensorId.isBlank()) {
            main.post {
                callback(null, "Missing sensor ID.")
            }
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { result ->

                val token = result.token

                if (token.isNullOrBlank()) {
                    callback(
                        null,
                        "Firebase token unavailable."
                    )
                    return@addOnSuccessListener
                }

                val url =
                    "$BASE/api/sensors/" +
                            "${Uri.encode(sensorId)}/monitoring"

                val request = Request.Builder()
                    .url(url)
                    .header(
                        "Authorization",
                        "Bearer $token"
                    )
                    .get()
                    .build()

                client.newCall(request)
                    .enqueue(object : Callback {

                        override fun onFailure(
                            call: Call,
                            e: IOException
                        ) {
                            main.post {
                                callback(
                                    null,
                                    "API connection error: ${e.message}"
                                )
                            }
                        }

                        override fun onResponse(
                            call: Call,
                            response: Response
                        ) {
                            response.use { res ->

                                val body =
                                    res.body?.string().orEmpty()

                                val data = runCatching {
                                    JSONObject(body)
                                }.getOrNull()

                                val error =
                                    if (res.isSuccessful) {
                                        null
                                    } else {
                                        data?.optString("error")
                                            ?.takeIf {
                                                it.isNotBlank()
                                            }
                                            ?: "HTTP ${res.code}: " +
                                            "Monitoring request failed."
                                    }

                                main.post {
                                    callback(
                                        if (res.isSuccessful) data
                                        else null,
                                        error
                                    )
                                }
                            }
                        }
                    })
            }
            .addOnFailureListener { error ->
                callback(
                    null,
                    "Authentication failed: ${error.message}"
                )
            }
    }
}
