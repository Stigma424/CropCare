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

data class ZoneMonitoringSnapshot(val zone: ZoneModel, val sensors: List<SensorMonitoringSnapshot>) {
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
    // Existing five-parameter score, without treating absent measurements as zero.
    val score: Double? get() {
        val a = averages
        val n = a["nitrogen"] ?: return null; val p = a["phosphorus"] ?: return null
        val k = a["potassium"] ?: return null; val m = a["moisture"] ?: return null
        val ph = a["ph"] ?: return null
        return (if (n >= 20) 20.0 else 0.0) + (if (p >= 10) 20 else 0) +
            (if (k >= 100) 20 else 0) + (if (m in 40.0..80.0) 20 else 0) + (if (ph in 5.5..7.5) 20 else 0)
    }
}
