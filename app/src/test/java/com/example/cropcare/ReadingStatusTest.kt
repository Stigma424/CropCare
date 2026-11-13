package com.example.cropcare
import org.junit.Assert.assertEquals
import org.junit.Test
class ReadingStatusTest {
    @Test fun boundariesAndMissingValues() {
        assertEquals("LOW", ReadingStatus.label("moisture", 39.9))
        assertEquals("GOOD", ReadingStatus.label("moisture", 40.0))
        assertEquals("GOOD", ReadingStatus.label("moisture", 65.0))
        assertEquals("HIGH", ReadingStatus.label("moisture", 65.1))
        assertEquals("CHECK SENSOR", ReadingStatus.label("ph", 20.0))
        assertEquals("NO DATA", ReadingStatus.label("nitrogen", null))
        assertEquals("LOW", ReadingStatus.label("nitrogen", 0.0))
        assertEquals("NO DATA", ReadingStatus.label("nitrogen", Double.NaN))
    }
}
