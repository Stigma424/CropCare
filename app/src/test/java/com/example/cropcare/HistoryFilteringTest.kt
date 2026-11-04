package com.example.cropcare

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class HistoryFilteringTest {
    @Test fun manilaMidnightAndMissingValuesStayDistinct() {
        val before = SensorHistoryReading.fromJson(JSONObject().put("recordedAtMs", 86_399_999L - 28_800_000L))
        val after = SensorHistoryReading.fromJson(JSONObject().put("recordedAtMs", 86_400_000L - 28_800_000L))
        assertEquals("1970-01-01", HistoryFilter.day(before.recordedAtMs))
        assertEquals("1970-01-02", HistoryFilter.day(after.recordedAtMs))
        assertEquals(listOf(after), HistoryFilter.apply(listOf(after, before), "1970-01-02"))
        assertEquals(2, HistoryFilter.apply(listOf(after, before), null).size)
        assertNull(HistoryFilter.value(after, 0)); assertNull(HistoryFilter.day(0))
    }
}
