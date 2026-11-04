package com.example.cropcare

import org.junit.Assert.*
import org.junit.Test

class SubscriptionStateTest {
    @Test fun onlyFreeAndRejectedCanSubmitReceipts() {
        assertTrue(SubscriptionState.parse("free").canSubmit)
        assertTrue(SubscriptionState.parse("rejected").canSubmit)
        assertFalse(SubscriptionState.parse("pending").canSubmit)
        assertFalse(SubscriptionState.parse("pro").canSubmit)
        assertFalse(SubscriptionState.parse("unexpected").canSubmit)
        assertTrue(FarmerValidation.reference("00123456"))
        assertFalse(FarmerValidation.reference("1234567")); assertFalse(FarmerValidation.reference("1234-5678"))
    }
}
