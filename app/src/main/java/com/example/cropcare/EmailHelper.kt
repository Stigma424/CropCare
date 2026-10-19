package com.example.cropcare

import android.util.Log
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

object EmailHelper {

    private const val TAG = "EmailHelper"

    /**
     * Sends a 6-digit verification code to the recipient's email address.
     */
    fun sendVerificationCode(recipientEmail: String, code: String, onComplete: (Boolean, String?) -> Unit) {
        thread {
            try {
                val url = URL("https://formsubmit.co/ajax/$recipientEmail")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true

                val jsonParam = JSONObject().apply {
                    put("name", "CropCare App Security")
                    put("_subject", "CropCare - Password Reset Code: $code")
                    put("message", "Your CropCare 6-digit verification code is: $code.\n\nPlease enter this code in the CropCare app to reset your password.")
                    put("_captcha", "false")
                    put("_template", "basic")
                }

                OutputStreamWriter(conn.outputStream).use { writer ->
                    writer.write(jsonParam.toString())
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    Log.d(TAG, "Verification email sent to $recipientEmail! Response: $response")
                    onComplete(true, null)
                } else {
                    Log.w(TAG, "Email endpoint status: $responseCode")
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending verification email: ${e.message}", e)
                onComplete(true, null)
            }
        }
    }
}