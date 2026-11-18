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
        assertEquals(FarmerAssessment.parse(json).noReadingsMessage, FarmerAssessment.parse(json).details())
    }
    @Test fun oldContractIsRejected() {
        try { FarmerAssessment.parse(JSONObject("""{"soilHealth":{"score":100}}""")); fail("Old response accepted") } catch (_: IllegalArgumentException) { }
    }
    @Test fun summaryKeepsProgramQuantitiesVisibleAndSeparatesFieldChecks() {
        val json = JSONObject("""{"assessmentStatus":"assessed","sensorQuality":[],"primaryAction":{"title":"Nutrients","whatToDo":"Review the plan"},"recommendations":[{"code":"NUTRIENT_REVIEW"}],"soilHealth":{"score":60,"label":"Monitor"},"referenceFertilizerPlan":{"entries":[{"product":"Urea","fertilizerGrade":"46-0-0","kgForPlot":5.6,"applicationTiming":"25 DAE"}]}}""")
        val assessment = FarmerAssessment.parse(json)
        assertTrue(assessment.canReviewApplication)
        assertTrue(assessment.summary.startsWith("• What to apply"))
        assertTrue(assessment.summary.contains("5.60 kg"))
        json.getJSONArray("recommendations").put(JSONObject().put("code", "HIGH_MOISTURE"))
        assertFalse(assessment.canReviewApplication)
        assertTrue(assessment.summary.contains("5.60 kg"))
        assertTrue(assessment.applicationNote.contains("Before applying"))
        json.getJSONArray("recommendations").remove(1)
        json.getJSONArray("recommendations").remove(0)
        assertTrue(assessment.summary.contains("5.60 kg"))
        assertFalse(assessment.canReviewApplication)
        json.getJSONObject("referenceFertilizerPlan").getJSONArray("entries").remove(0)
        assertFalse(assessment.summary.contains("5.60 kg"))
    }
    @Test fun unavailableReadingsWithRetainedPlanHideAllApplicationAdvice() {
        val json = JSONObject("""{"soilHealth":{"score":null,"validSensors":0},"programFertilizerPlan":{"entries":[{"product":"Urea","kgForPlot":5.6}]},"recommendations":[{"code":"NUTRIENT_REVIEW"}]}""")
        val assessment = FarmerAssessment(json)
        assertTrue(assessment.hasPlan)
        assertFalse(assessment.hasUsableReadings)
        assertFalse(assessment.canReviewApplication)
        assertEquals(assessment.noReadingsMessage, assessment.summary)
        assertEquals(assessment.noReadingsMessage, assessment.details())
        json.getJSONObject("soilHealth").put("score", 0).put("validSensors", 1)
        assertTrue(assessment.hasUsableReadings)
        assertTrue(assessment.summary.contains("What to apply"))
        json.getJSONObject("soilHealth").put("validSensors", 0)
        assertFalse(assessment.hasUsableReadings)
        assertEquals(assessment.noReadingsMessage, assessment.summary)
        json.getJSONObject("soilHealth").put("validSensors", 1).put("score", "NaN")
        assertFalse(assessment.hasUsableReadings)
    }
    @Test fun pushMustHaveMatchingRecipientAndStableHistoryId() {
        val payload = mapOf("notificationId" to "n1", "recipientUid" to "owner", "title" to "Alert", "body" to "Details")
        assertNotNull(AccountPushEnvelope.parse(payload, "owner"))
        assertNull(AccountPushEnvelope.parse(payload, "other"))
        assertNull(AccountPushEnvelope.parse(payload, null))
        assertNull(AccountPushEnvelope.parse(payload - "notificationId", "owner"))
    }
}
