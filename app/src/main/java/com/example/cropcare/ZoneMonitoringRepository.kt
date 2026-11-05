package com.example.cropcare

import android.net.Uri
import org.json.JSONObject

class MonitoringRequest {
    private val requests = mutableListOf<ApiRequestHandle>()
    var active = true
        private set
    fun add(request: ApiRequestHandle) { if (active) requests.add(request) else request.cancel() }
    fun cancel() { active = false; requests.forEach { it.cancel() }; requests.clear() }
}

object ZoneMonitoringRepository {
    fun sensor(row: JSONObject) = SensorModel(sensorId = row.optString("sensorId"),
        deviceId = row.optString("deviceId"), sensorName = row.optString("sensorName", "Sensor"),
        zoneId = row.optString("zoneId"), userId = row.optString("userId"), pairingStatus = row.optString("pairingStatus"))
    fun listSensors(zoneId: String?, callback: (List<SensorModel>?, String?) -> Unit): ApiRequestHandle =
        CropCareHttpClient.request("GET", "/api/sensors" + (zoneId?.let { "?zoneId=${Uri.encode(it)}" } ?: "")) { result ->
            val rows = result.array
            if (!result.successful || rows == null) callback(null, result.message)
            else callback((0 until rows.length()).mapNotNull { rows.optJSONObject(it)?.let(::sensor) }, null)
        }
    fun dashboard(callback: (List<ZoneMonitoringSnapshot>?, String?) -> Unit): MonitoringRequest {
        val scope = MonitoringRequest()
        scope.add(CropCareHttpClient.request("GET", "/api/zones") { result ->
            val rows = result.array
            if (!result.successful || rows == null) callback(null, result.message)
            else {
                val zones = (0 until rows.length()).mapNotNull { rows.optJSONObject(it) }.map { row ->
                    ZoneModel(row.optString("zoneId"), row.optString("zoneName"), row.optDouble("zoneAreaSqm"),
                        row.optLong("dateOfPlanting"), row.optBoolean("isHarvested"))
                }
                scope.add(listSensors(null) { sensors, error ->
                    if (sensors == null) callback(null, error) else monitor(scope, sensors) { snapshots ->
                        assessments(scope, zones.map { zone -> ZoneMonitoringSnapshot(zone, snapshots.filter { it.sensor.zoneId == zone.zoneId }) }) { callback(it, null) }
                    }
                })
            }
        })
        return scope
    }
    fun zone(zone: ZoneModel, callback: (ZoneMonitoringSnapshot?, String?) -> Unit): MonitoringRequest {
        val scope = MonitoringRequest()
        scope.add(listSensors(zone.zoneId) { sensors, error ->
            if (sensors == null) callback(null, error)
            else monitor(scope, sensors) { callback(ZoneMonitoringSnapshot(zone, it), null) }
        })
        return scope
    }
    fun sensors(zoneId: String, callback: (List<SensorMonitoringSnapshot>?, String?) -> Unit): MonitoringRequest {
        val scope = MonitoringRequest()
        scope.add(listSensors(zoneId) { sensors, error ->
            if (sensors == null) callback(null, error) else monitor(scope, sensors) { callback(it, null) }
        })
        return scope
    }
    private fun assessments(scope: MonitoringRequest, zones: List<ZoneMonitoringSnapshot>, done: (List<ZoneMonitoringSnapshot>) -> Unit) {
        if (zones.isEmpty()) { done(zones); return }
        scope.add(CropCareFarmerApi.subscription { membership ->
            if (!scope.active) return@subscription
            val status = membership.data?.optString("status")?.lowercase()
            if (!membership.successful || status != "pro" || membership.data?.optBoolean("isSubscribed") != true) {
                val note = if (membership.successful && status in listOf("free", "pending", "rejected")) "Pro assessment required" else "Membership unavailable"
                done(zones.map { it.copy(assessmentError = note) }); return@subscription
            }
            val results = zones.toTypedArray(); var next = 0; var completed = 0
            fun launch() {
                if (!scope.active || next >= zones.size) return
                val index = next++
                scope.add(CropCareFarmerApi.assessment(zones[index].zone.zoneId) { response ->
                    if (!scope.active) return@assessment
                    val assessment = response.data?.takeIf { response.successful }?.let { runCatching { FarmerAssessment.parse(it) }.getOrNull() }
                    results[index] = zones[index].copy(assessment = assessment, assessmentError = if (assessment == null) "Server assessment unavailable" else null)
                    completed++; if (completed == zones.size) done(results.toList()) else launch()
                })
            }
            repeat(minOf(3, zones.size)) { launch() }
        })
    }    private fun monitor(scope: MonitoringRequest, sensors: List<SensorModel>, done: (List<SensorMonitoringSnapshot>) -> Unit) {
        if (!scope.active) return
        if (sensors.isEmpty()) { done(emptyList()); return }
        val result = arrayOfNulls<SensorMonitoringSnapshot>(sensors.size)
        var next = 0; var completed = 0
        fun launch() {
            if (!scope.active || next >= sensors.size) return
            val index = next++; val sensor = sensors[index]
            scope.add(CropCareHttpClient.request("GET", "/api/sensors/${Uri.encode(sensor.sensorId)}/monitoring") { response ->
                if (!scope.active) return@request
                val data = response.data
                val reading = data?.optJSONObject("reading")?.let(SoilMeasurement::fromJson)
                val status = if (response.successful && data != null) data.optString("status", "unknown_time") else "unavailable"
                result[index] = SensorMonitoringSnapshot(sensor.copy(lastScanTimestamp = data?.optLong("lastReadingAtMs") ?: 0,
                    isOnline = status == "recent", readingStatusUnavailable = status == "unavailable", readingStatus = status),
                    status, reading, if (status == "unavailable") response.message else null)
                completed++
                if (completed == sensors.size) done(result.filterNotNull()) else launch()
            })
        }
        repeat(minOf(4, sensors.size)) { launch() }
    }
}
