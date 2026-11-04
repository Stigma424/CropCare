package com.example.cropcare

import android.net.Uri
import org.json.JSONObject

object SensorMonitoringApi {
    fun get(sensorId: String, callback: (JSONObject?, String?) -> Unit): ApiRequestHandle =
        CropCareHttpClient.request("GET", "/api/sensors/${Uri.encode(sensorId)}/monitoring") { result ->
            callback(if (result.successful) result.data else null,
                if (result.successful && result.data != null) null else result.message)
        }
}
