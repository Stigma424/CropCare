package com.example.cropcare

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RegistrationFlowTest {
    @Test fun partialRegistrationResumesOnlyForAuthenticatedOwner() {
        assertEquals(OnboardingDecision.CREATE_PROFILE, onboardingDecision(FarmerApiResult(false, 404, null, "Missing"), "owner"))
        assertEquals(OnboardingDecision.COMPLETE, onboardingDecision(FarmerApiResult(true, 200, JSONObject().put("userId", "owner"), ""), "owner"))
        assertEquals(OnboardingDecision.REJECT, onboardingDecision(FarmerApiResult(true, 200, JSONObject().put("userId", "other"), ""), "owner"))
        assertEquals(OnboardingDecision.RETRY, onboardingDecision(FarmerApiResult(false, 503, null, ""), "owner"))
    }
    @Test fun newPasswordAndUsernameBoundaries() {
        assertTrue(FarmerValidation.username("Farmer_01")); assertFalse(FarmerValidation.username("ab"))
        assertFalse(FarmerValidation.username("farmer@example.com"))
        assertTrue(FarmerValidation.password("  secret  ")); assertFalse(FarmerValidation.password("short"))
        assertFalse(FarmerValidation.password("x".repeat(129)))
    }
}
