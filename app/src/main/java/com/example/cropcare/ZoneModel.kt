package com.example.cropcare

data class ZoneModel(
    val zoneId: String = "",
    val zoneName: String = "",
    val zoneAreaSqm: Double = 0.0,
    val dateOfPlanting: Long = 0L,
    val isHarvested: Boolean = false
)