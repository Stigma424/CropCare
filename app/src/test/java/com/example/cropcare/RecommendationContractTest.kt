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
    @Test fun summaryLeadsWithApplicationAndWithholdsAmountsWhenFieldChecksBlock() {
        val json = JSONObject("""{"assessmentStatus":"assessed","sensorQuality":[],"primaryAction":{"title":"Nutrients","whatToDo":"Review the plan"},"recommendations":[{"code":"NUTRIENT_REVIEW"}],"soilHealth":{"score":60,"label":"Monitor"},"referenceFertilizerPlan":{"entries":[{"product":"Urea","fertilizerGrade":"46-0-0","kgForPlot":5.6,"applicationTiming":"25 DAE"}]}}""")
        val assessment = FarmerAssessment.parse(json)
        assertTrue(assessment.canReviewApplication)
        assertTrue(assessment.summary.startsWith("• What to apply"))
        assertTrue(assessment.summary.contains("5.60 kg"))
        json.getJSONArray("recommendations").put(JSONObject().put("code", "HIGH_MOISTURE"))
        assertFalse(assessment.canReviewApplication)
        assertFalse(assessment.summary.contains("5.60 kg"))
        assertTrue(assessment.whatToApply.contains("Hold"))
    }
    @Test fun pushMustHaveMatchingRecipientAndStableHistoryId() {
        val payload = mapOf("notificationId" to "n1", "recipientUid" to "owner", "title" to "Alert", "body" to "Details")
        assertNotNull(AccountPushEnvelope.parse(payload, "owner"))
        assertNull(AccountPushEnvelope.parse(payload, "other"))
        assertNull(AccountPushEnvelope.parse(payload, null))
        assertNull(AccountPushEnvelope.parse(payload - "notificationId", "owner"))
    }
}
