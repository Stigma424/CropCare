package com.example.cropcare

import org.junit.Assert.*
import org.junit.Test

class SensorUnpairingTest {
    @Test fun releaseResultDoesNotInventQrReuseOnLegacyOrConflict() {
        val released = FarmerResponseParser.parse(200, "{\"qrReusable\":true}")
        val legacy = FarmerResponseParser.parse(200, "{\"qrReusable\":false}")
        val conflict = FarmerResponseParser.parse(409, "{\"error\":\"Registry mismatch\"}")
        assertTrue(released.data!!.getBoolean("qrReusable")); assertFalse(legacy.data!!.getBoolean("qrReusable"))
        assertFalse(conflict.successful)
        val delete = CropCareWireClient.build("http://localhost/api/sensors/test", "DELETE", null, "token")
        assertEquals("DELETE", delete.method); assertTrue(delete.body == null || delete.body!!.contentLength() == 0L)
    }
}
