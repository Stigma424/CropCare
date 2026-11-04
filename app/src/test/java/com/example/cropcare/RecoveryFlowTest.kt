package com.example.cropcare

import org.junit.Assert.*
import org.junit.Test

class RecoveryFlowTest {
    @Test fun codesPreserveLeadingZeroesAndTicketsExpire() {
        assertTrue(FarmerValidation.code("001234")); assertFalse(FarmerValidation.code("12345"))
        assertFalse(FarmerValidation.code("1234567")); assertFalse(FarmerValidation.code("12a456"))
        var now = 100L
        RecoverySession.clock = { now }
        try {
            RecoverySession.start("test@example.test"); assertNull(RecoverySession.ticket())
            RecoverySession.verified("ticket"); assertEquals("ticket", RecoverySession.ticket())
            now += 600_000; assertNull(RecoverySession.ticket())
            RecoverySession.start("new@example.test"); assertNull(RecoverySession.ticket())
            RecoverySession.clear(); assertNull(RecoverySession.email)
        } finally { RecoverySession.clear(); RecoverySession.clock = { System.nanoTime() / 1_000_000 } }
    }
}
