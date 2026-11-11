package com.example.cropcare

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ZoneMonitoringRepositoryTest {
    @Test fun missingMeasurementsAreNotZerosAndRealZeroScoreIsIncluded() {
        val empty = ZoneMonitoringSnapshot(ZoneModel(), emptyList())
        assertNull(empty.score); assertTrue(empty.averages.values.all { it == null })
        val zeros = SoilMeasurement.fromJson(JSONObject().put("nitrogen", 0).put("phosphorus", 0)
            .put("potassium", 0).put("moisture", 0).put("ph", 0))
        val measured = ZoneMonitoringSnapshot(ZoneModel(), listOf(SensorMonitoringSnapshot(SensorModel(), "recent", zeros)))
        assertEquals(0.0, measured.averages["nitrogen"]!!, 0.0)
        assertNull(measured.score) // Raw readings do not substitute for the server assessment.
        assertNull(measured.averages["temperature"])
        val assessed = measured.copy(assessment = FarmerAssessment(JSONObject().put("soilHealth", JSONObject().put("score", 0))))
        assertEquals(0.0, assessed.score!!, 0.0)
        val partial = ZoneMonitoringSnapshot(ZoneModel(), listOf(SensorMonitoringSnapshot(SensorModel(), "recent",
            SoilMeasurement.fromJson(JSONObject().put("nitrogen", 10)))))
        assertNull(partial.score); assertEquals("Partial measurements", partial.notice)
    }
    @Test fun readingsRetainAllSevenFieldsAcrossManySensors() {
        val json = JSONObject(); soilKeys.forEachIndexed { i, key -> json.put(key, i + 1) }
        val snapshots = (1..35).map { SensorMonitoringSnapshot(SensorModel(sensorId = "$it"), "recent", SoilMeasurement.fromJson(json)) }
        val zone = ZoneMonitoringSnapshot(ZoneModel(), snapshots)
        soilKeys.forEachIndexed { i, key -> assertEquals((i + 1).toDouble(), zone.averages[key]!!, 0.0) }
    }
}
