package com.example.cropcare
import com.example.cropcare.AccountPushEnvelope
import com.example.cropcare.FarmerAssessment
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class RecommendationContractTest {
    @Test fun zeroAndMissingScoresRemainDistinct() {
        val json = JSONObject("""{"assessmentStatus":"provisional","sensorQuality":[],"primaryAction":{"title":"Check","whatToDo":"Verify"},"soilHealth":{"score":0,"label":"Provisional"},"referenceFertilizerPlan":{"entries":[]}}""")
        assertEquals(0.0, FarmerAssessment.parse(json).score!!, 0.0)
        json.getJSONObject("soilHealth").put("score", JSONObject.NULL)
        assertNull(FarmerAssessment.parse(json).score)
        assertTrue(FarmerAssessment.parse(json).details().contains("Quantities withheld"))
    }
    @Test fun oldContractIsRejected() {
        try { FarmerAssessment.parse(JSONObject("""{"soilHealth":{"score":100}}""")); fail("Old response accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun pushMustHaveMatchingRecipientAndStableHistoryId() {
        val payload = mapOf("notificationId" to "n1", "recipientUid" to "owner", "title" to "Alert", "body" to "Details")
        assertNotNull(AccountPushEnvelope.parse(payload, "owner"))
        assertNull(AccountPushEnvelope.parse(payload, "other"))
        assertNull(AccountPushEnvelope.parse(payload, null))
        assertNull(AccountPushEnvelope.parse(payload - "notificationId", "owner"))
    }
}
