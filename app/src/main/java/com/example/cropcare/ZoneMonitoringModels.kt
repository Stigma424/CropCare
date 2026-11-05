package com.example.cropcare

import org.json.JSONObject

val soilKeys = listOf("nitrogen", "phosphorus", "potassium", "moisture", "ph", "temperature", "ec")

data class SoilMeasurement(val values: Map<String, Double?>, val recordedAtMs: Long) {
    companion object {
        fun fromJson(json: JSONObject) = SoilMeasurement(soilKeys.associateWith { key ->
            (json.opt(key) as? Number)?.toDouble()?.takeIf { it.isFinite() }
        }, json.optLong("recordedAtMs"))
    }
}

data class SensorMonitoringSnapshot(val sensor: SensorModel, val status: String,
    val reading: SoilMeasurement?, val error: String? = null)

data class ZoneMonitoringSnapshot(val zone: ZoneModel, val sensors: List<SensorMonitoringSnapshot>, val assessment: FarmerAssessment? = null, val assessmentError: String? = null) {
    val averages: Map<String, Double?> get() = soilKeys.associateWith { key ->
        sensors.mapNotNull { it.reading?.values?.get(key) }.takeIf { it.isNotEmpty() }?.average()
    }
    val newestAtMs: Long get() = sensors.mapNotNull { it.reading?.recordedAtMs }.filter { it > 0 }.maxOrNull() ?: 0
    val notice: String get() = when {
        sensors.isEmpty() -> "No sensors registered"
        sensors.all { it.error != null } -> "Monitoring unavailable. Try refreshing."
        sensors.none { it.reading != null } -> "Awaiting first reading"
        sensors.any { it.error != null } -> "Some sensor readings are unavailable"
        averages.values.any { it == null } -> "Partial measurements"
        sensors.any { it.status == "unknown_time" } -> "Some timestamps are unavailable"
        sensors.any { it.status == "stale" } -> "Includes older measurements"
        else -> "Recent recorded measurements"
    }
    val score: Double? get() = assessment?.score
    val scoreLabel: String get() = assessment?.label ?: assessmentError ?: "Assessment unavailable"
}
