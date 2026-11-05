package com.example.cropcare

import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.IOException
import java.util.concurrent.TimeUnit

data class FarmerApiResult(val successful: Boolean, val statusCode: Int,
    val body: Any?, val message: String) {
    val data: JSONObject? get() = body as? JSONObject
    val array: JSONArray? get() = body as? JSONArray
}

class ApiRequestHandle {
    @Volatile private var cancelled = false
    private var call: Call? = null
    @Synchronized fun cancel() { cancelled = true; call?.cancel() }
    @Synchronized internal fun attach(value: Call) { call = value; if (cancelled) value.cancel() }
    internal fun active() = !cancelled
}

object CropCareHttpClient {
    private val main = Handler(Looper.getMainLooper())
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS).callTimeout(25, TimeUnit.SECONDS).build()
    val baseUrl: String get() = BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() } ?: if (Build.FINGERPRINT.startsWith("generic") ||
        Build.FINGERPRINT.contains("emulator") || Build.MODEL.contains("sdk_gphone") || Build.MODEL.contains("Emulator")) "http://10.0.2.2:5056" else "http://127.0.0.1:5056"
    fun request(method: String, path: String, body: JSONObject? = null,
        authenticated: Boolean = true, callback: (FarmerApiResult) -> Unit): ApiRequestHandle {
        val owner = if (authenticated) FirebaseAuth.getInstance().currentUser?.uid else null
        val handle = ApiRequestHandle()
        fun deliver(result: FarmerApiResult) { main.post { if (handle.active() && (!authenticated || FirebaseAuth.getInstance().currentUser?.uid == owner)) callback(result) } }
        fun send(token: String?) {
            if (!handle.active()) return
            try {
                if (!BuildConfig.DEBUG && !baseUrl.startsWith("https://")) {
                    deliver(FarmerApiResult(false, 0, null, "A trusted HTTPS API must be configured for release builds."))
                    return
                }
                val call = client.newCall(CropCareWireClient.build(baseUrl + path, method, body, token))
                handle.attach(call)
                call.enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        deliver(FarmerApiResult(false, 0, null, "Cannot reach CropCare API. Check your connection and try again."))
                    }
                    override fun onResponse(call: Call, response: Response) {
                        response.use { res ->
                            val result = runCatching { FarmerResponseParser.parse(res.code, res.body?.string().orEmpty()) }
                                .getOrElse { FarmerApiResult(false, res.code, null, "Could not read the API response.") }
                            deliver(result)
                        }
                    }
                })
            } catch (_: Exception) { deliver(FarmerApiResult(false, 0, null, "Could not start the API request.")) }
        }
        if (!authenticated) send(null) else {
            val user = FirebaseAuth.getInstance().currentUser
            if (user == null) deliver(FarmerApiResult(false, 401, null, "Please log in again."))
            else user.getIdToken(false).addOnSuccessListener { result ->
                val token = result.token
                if (token.isNullOrBlank()) deliver(FarmerApiResult(false, 401, null, "Please log in again.")) else send(token)
            }.addOnFailureListener { deliver(FarmerApiResult(false, 401, null, "Please log in again.")) }
        }
        return handle
    }
}
