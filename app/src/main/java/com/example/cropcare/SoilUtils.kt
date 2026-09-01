package com.example.cropcare

object SoilUtils {
    // Returns status label: "Normal", "Low", or "High"
    fun getStatus(parameter: String, value: Double): String {
        return when (parameter.uppercase()) {
            "N" -> if (value < 30) "Low" else if (value > 80) "High" else "Normal"
            "P" -> if (value < 15) "Low" else if (value > 50) "High" else "Normal"
            "K" -> if (value < 20) "Low" else if (value > 60) "High" else "Normal"
            "MOISTURE" -> if (value < 20) "Low" else if (value > 60) "High" else "Normal"
            "PH" -> if (value < 5.5) "Low" else if (value > 7.5) "High" else "Normal"
            "TEMP" -> if (value < 18) "Low" else if (value > 35) "High" else "Normal"
            "EC" -> if (value < 10) "Low" else if (value > 50) "High" else "Normal"
            else -> "Normal"
        }
    }
}