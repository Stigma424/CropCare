package com.example.cropcare

data class SensorModel(
    val sensorId: String = "",
    val deviceId: String = "",
    val sensorName: String = "",
    val zoneId: String = "",
    val userId: String = "",
    val lastScanTimestamp: Long = 0L,
    val isOnline: Boolean = true
)