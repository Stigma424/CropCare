package com.example.cropcare

import org.json.JSONObject

/** One Firestore soil_data document, containing all seven measurements. */
data class SensorHistoryReading(
    val readingId: String,
    val recordedAtMs: Long,
    val nitrogen: Double?,
    val phosphorus: Double?,
    val potassium: Double?,
    val moisture: Double?,
    val ph: Double?,
    val temperature: Double?,
    val ec: Double?
) {
    companion object {
        fun fromJson(json: JSONObject): SensorHistoryReading {
            fun number(field: String): Double? {
                val raw = json.opt(field)
                return when (raw) {
                    is Number -> raw.toDouble().takeIf { it.isFinite() }
                    else -> null
                }
            }

            return SensorHistoryReading(
                readingId = json.optString("readingId", ""),
                recordedAtMs = json.optLong("recordedAtMs", 0L),
                nitrogen = number("nitrogen"),
                phosphorus = number("phosphorus"),
                potassium = number("potassium"),
                moisture = number("moisture"),
                ph = number("ph"),
                temperature = number("temperature"),
                ec = number("ec")
            )
        }
    }
}
