package com.example.cropcare

import java.text.SimpleDateFormat
import java.util.*

object HistoryFilter {
    fun day(milliseconds: Long): String? = if (milliseconds <= 0) null else
        SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { timeZone = TimeZone.getTimeZone("Asia/Manila") }.format(Date(milliseconds))
    fun apply(records: List<SensorHistoryReading>, selectedDay: String?) = records.filter {
        selectedDay == null || day(it.recordedAtMs) == selectedDay
    }
    fun value(record: SensorHistoryReading, metric: Int): Double? = when (metric) {
        0 -> record.nitrogen; 1 -> record.phosphorus; 2 -> record.potassium
        3 -> record.moisture; 4 -> record.ph; 5 -> record.temperature; else -> record.ec
    }
}
