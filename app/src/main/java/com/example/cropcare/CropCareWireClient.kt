package com.example.cropcare

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

/** Android-free wire contract, also exercised against MockWebServer. */
object FarmerResponseParser {
    fun parse(code: Int, raw: String): FarmerApiResult {
        val success = code in 200..299
        val body = if (raw.isBlank()) null else runCatching { JSONTokener(raw).nextValue() }.getOrNull()
        if (success && raw.isNotBlank() && body !is JSONObject && body !is JSONArray)
            return FarmerApiResult(false, code, null, "The API returned an invalid response.")
        val obj = body as? JSONObject
        val message = listOf("error", "detail", "title", "message").firstNotNullOfOrNull { key ->
            obj?.optString(key)?.takeIf { it.isNotBlank() && it != "null" }
        } ?: if (success) "Request successful." else "Request failed (HTTP $code)."
        return FarmerApiResult(success, code, body, message)
    }
}

object CropCareWireClient {
    fun build(url: String, method: String, body: JSONObject?, token: String?): Request {
        val builder = Request.Builder().url(url).header("Accept", "application/json")
        if (token != null) builder.header("Authorization", "Bearer $token")
        val json = (body ?: JSONObject()).toString().toRequestBody("application/json; charset=utf-8".toMediaType())
        when (method) {
            "GET" -> builder.get(); "POST" -> builder.post(json)
            "PATCH" -> builder.patch(json); "DELETE" -> builder.delete()
            else -> error("Unsupported request method")
        }
        return builder.build()
    }
}
