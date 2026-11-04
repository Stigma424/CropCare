
package com.example.cropcare

import android.net.Uri
import org.json.JSONObject


object CropCareApiClient {

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

    private fun authenticatedRequest(method: String, path: String, jsonBody: JSONObject?, callback: (ApiResult) -> Unit) {
        CropCareHttpClient.request(method, path, jsonBody) { result ->
            callback(ApiResult(result.successful, result.statusCode, result.data, result.message))
        }
    }
}
