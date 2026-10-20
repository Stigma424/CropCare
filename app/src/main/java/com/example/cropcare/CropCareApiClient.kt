
package com.example.cropcare

import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object CropCareApiClient {

    private val mainHandler = Handler(Looper.getMainLooper())

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonType =
        "application/json; charset=utf-8".toMediaType()

    private val baseUrl: String
        get() {
            val isEmulator =
                Build.FINGERPRINT.startsWith("generic") ||
                        Build.FINGERPRINT.contains("emulator") ||
                        Build.MODEL.contains("sdk_gphone") ||
                        Build.MODEL.contains("Emulator")

            return if (isEmulator) {
                "http://10.0.2.2:5056"
            } else {
                // Physical phone through adb reverse.
                "http://127.0.0.1:5056"
            }
        }

    data class ApiResult(
        val successful: Boolean,
        val statusCode: Int,
        val data: JSONObject?,
        val message: String
    )

    data class ProfileUpdate(
        val firstName: String,
        val middleName: String,
        val lastName: String,
        val address: String,
        val phoneNumber: String
    )

    data class ZoneInput(
        val zoneName: String,
        val zoneAreaSqm: Double,
        val dateOfPlanting: Long
    )

    data class SensorInput(
        val deviceId: String,
        val sensorName: String,
        val zoneId: String
    )

    data class SensorPairingInput(
        val deviceId: String,
        val claimCode: String,
        val sensorName: String,
        val zoneId: String
    )

    // -------------------------------------
    // ACCOUNT
    // -------------------------------------

    fun getMyProfile(callback: (ApiResult) -> Unit) {
        authenticatedRequest(
            "GET",
            "/api/account/me",
            null,
            callback
        )
    }

    fun updateMyProfile(
        profile: ProfileUpdate,
        callback: (ApiResult) -> Unit
    ) {
        val json = JSONObject().apply {
            put("firstName", profile.firstName)
            put("middleName", profile.middleName)
            put("lastName", profile.lastName)
            put("address", profile.address)
            put("phoneNumber", profile.phoneNumber)
        }

        authenticatedRequest(
            "PATCH",
            "/api/account/me",
            json,
            callback
        )
    }

    // -------------------------------------
    // ZONES
    // -------------------------------------

    fun createZone(
        zone: ZoneInput,
        callback: (ApiResult) -> Unit
    ) {
        val json = JSONObject().apply {
            put("zoneName", zone.zoneName)
            put("zoneAreaSqm", zone.zoneAreaSqm)
            put("dateOfPlanting", zone.dateOfPlanting)
        }

        authenticatedRequest(
            "POST",
            "/api/zones",
            json,
            callback
        )
    }

    fun getZone(
        zoneId: String,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "GET",
            "/api/zones/${Uri.encode(zoneId)}",
            null,
            callback
        )
    }

    fun updateZone(
        zoneId: String,
        changes: JSONObject,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "PATCH",
            "/api/zones/${Uri.encode(zoneId)}",
            changes,
            callback
        )
    }

    fun deleteZone(
        zoneId: String,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "DELETE",
            "/api/zones/${Uri.encode(zoneId)}",
            null,
            callback
        )
    }

    // -------------------------------------
    // SENSORS
    // -------------------------------------

    // Older manual-registration method retained for
    // source compatibility. The API now returns HTTP 410.
    fun registerSensor(
        sensor: SensorInput,
        callback: (ApiResult) -> Unit
    ) {
        val json = JSONObject().apply {
            put("deviceId", sensor.deviceId)
            put("sensorName", sensor.sensorName)
            put("zoneId", sensor.zoneId)
        }

        authenticatedRequest(
            "POST",
            "/api/sensors",
            json,
            callback
        )
    }

    // New authenticated QR pairing.
    fun pairSensor(
        sensor: SensorPairingInput,
        callback: (ApiResult) -> Unit
    ) {
        val json = JSONObject().apply {
            put("deviceId", sensor.deviceId)
            put("claimCode", sensor.claimCode)
            put("sensorName", sensor.sensorName)
            put("zoneId", sensor.zoneId)
        }

        authenticatedRequest(
            "POST",
            "/api/sensors/pair",
            json,
            callback
        )
    }

    fun getSensor(
        sensorId: String,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "GET",
            "/api/sensors/${Uri.encode(sensorId)}",
            null,
            callback
        )
    }

    fun renameSensor(
        sensorId: String,
        newName: String,
        callback: (ApiResult) -> Unit
    ) {
        val json = JSONObject().apply {
            put("sensorName", newName)
        }

        authenticatedRequest(
            "PATCH",
            "/api/sensors/${Uri.encode(sensorId)}",
            json,
            callback
        )
    }

    fun deleteSensor(
        sensorId: String,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "DELETE",
            "/api/sensors/${Uri.encode(sensorId)}",
            null,
            callback
        )
    }

    // -------------------------------------
    // FIREBASE AUTHENTICATION
    // -------------------------------------

    // UNIFIED SENSOR HISTORY (BATCH 4)
    fun getSensorHistory(
        sensorId: String,
        cursor: String?,
        limit: Int = 20,
        callback: (ApiResult) -> Unit
    ) {
        var path = "/api/sensors/${Uri.encode(sensorId)}/readings?limit=$limit"
        if (!cursor.isNullOrBlank()) {
            path += "&cursor=${Uri.encode(cursor)}"
        }
        authenticatedRequest("GET", path, null, callback)
    }

    fun getLatestSensorReading(
        sensorId: String,
        callback: (ApiResult) -> Unit
    ) {
        authenticatedRequest(
            "GET",
            "/api/sensors/${Uri.encode(sensorId)}/readings/latest",
            null,
            callback
        )
    }

    private fun authenticatedRequest(
        method: String,
        path: String,
        jsonBody: JSONObject?,
        callback: (ApiResult) -> Unit
    ) {
        val user = FirebaseAuth.getInstance().currentUser

        if (user == null) {
            dispatch(
                callback,
                ApiResult(
                    false,
                    401,
                    null,
                    "Please log in again."
                )
            )
            return
        }

        user.getIdToken(false)
            .addOnSuccessListener { tokenResult ->

                val token = tokenResult.token

                if (token.isNullOrBlank()) {
                    dispatch(
                        callback,
                        ApiResult(
                            false,
                            401,
                            null,
                            "Could not obtain a Firebase ID token."
                        )
                    )
                    return@addOnSuccessListener
                }

                executeRequest(
                    method,
                    path,
                    jsonBody,
                    token,
                    callback
                )
            }
            .addOnFailureListener { error ->
                dispatch(
                    callback,
                    ApiResult(
                        false,
                        0,
                        null,
                        "Firebase authentication error: " +
                                (error.message ?: "Unknown error")
                    )
                )
            }
    }

    // -------------------------------------
    // HTTP REQUESTS
    // -------------------------------------

    private fun executeRequest(
        method: String,
        path: String,
        jsonBody: JSONObject?,
        token: String,
        callback: (ApiResult) -> Unit
    ) {
        try {
            val builder = Request.Builder()
                .url(baseUrl + path)
                .header("Authorization", "Bearer $token")
                .header("Accept", "application/json")

            when (method) {
                "GET" -> builder.get()

                "POST" -> {
                    val body = (jsonBody ?: JSONObject())
                        .toString()
                        .toRequestBody(jsonType)

                    builder.post(body)
                }

                "PATCH" -> {
                    val body = (jsonBody ?: JSONObject())
                        .toString()
                        .toRequestBody(jsonType)

                    builder.patch(body)
                }

                "DELETE" -> builder.delete()

                else -> {
                    dispatch(
                        callback,
                        ApiResult(
                            false,
                            0,
                            null,
                            "Unsupported HTTP method: $method"
                        )
                    )
                    return
                }
            }

            httpClient.newCall(builder.build())
                .enqueue(object : Callback {

                    override fun onFailure(
                        call: Call,
                        e: IOException
                    ) {
                        dispatch(
                            callback,
                            ApiResult(
                                false,
                                0,
                                null,
                                "Cannot reach CropCare API: " +
                                        (e.message ?: "Network error")
                            )
                        )
                    }

                    override fun onResponse(
                        call: Call,
                        response: Response
                    ) {
                        response.use { res ->

                            val rawBody =
                                res.body?.string().orEmpty()

                            val json = try {
                                JSONObject(rawBody)
                            } catch (_: Exception) {
                                null
                            }

                            val message = if (res.isSuccessful) {
                                json?.optString("message")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "Request successful."
                            } else {
                                json?.optString("error")
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "HTTP ${res.code}: Request failed."
                            }

                            dispatch(
                                callback,
                                ApiResult(
                                    successful = res.isSuccessful,
                                    statusCode = res.code,
                                    data = json,
                                    message = message
                                )
                            )
                        }
                    }
                })

        } catch (error: Exception) {
            dispatch(
                callback,
                ApiResult(
                    false,
                    0,
                    null,
                    error.message ?: "Unexpected API error."
                )
            )
        }
    }

    private fun dispatch(
        callback: (ApiResult) -> Unit,
        result: ApiResult
    ) {
        mainHandler.post {
            callback(result)
        }
    }
}
