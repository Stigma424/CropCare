package com.example.cropcare

import android.content.Intent
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import androidx.test.platform.app.InstrumentationRegistry
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import android.graphics.Bitmap
import android.os.SystemClock
import android.widget.TextView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.assertion.ViewAssertions.matches

/** Opt-in review of the phone's existing account; no credentials or tokens are written. */
class ConnectedAccountReviewTest {
    @Test fun inspectCurrentAccountAssessment() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("reviewAccount") == "true")
        val user = FirebaseAuth.getInstance().currentUser
        assertNotNull("Sign in to CropCare on the connected phone first", user)
        val token = Tasks.await(user!!.getIdToken(false), 30, TimeUnit.SECONDS).token!!
        val client = OkHttpClient.Builder().callTimeout(45, TimeUnit.SECONDS).build()
        fun get(path: String): String = client.newCall(Request.Builder().url(BuildConfig.API_BASE_URL.trimEnd('/') + path)
            .header("Authorization", "Bearer $token").build()).execute().use {
                assertTrue("API $path returned HTTP ${it.code}", it.isSuccessful)
                it.body!!.string()
            }
        val zones = JSONArray(get("/api/zones"))
        val report = JSONObject().put("checkedAtMs", System.currentTimeMillis()).put("zoneCount", zones.length())
        val assessments = JSONArray()
        var firstZone: String? = null
        for (i in 0 until minOf(zones.length(), 5)) {
            val zone = zones.getJSONObject(i)
            if (zone.optBoolean("isHarvested")) continue
            val id = zone.getString("zoneId")
            if (firstZone == null) firstZone = id
            assessments.put(JSONObject(get("/api/zones/$id/assessment")))
        }
        report.put("assessments", assessments)
        File(instrumentation.targetContext.getExternalFilesDir(null), "connected-account-review.json").writeText(report.toString(2))
        println("CONNECTED_ACCOUNT_REVIEW zones=${zones.length()} assessments=${assessments.length()}")
        firstZone?.let {
            val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, ZoneManagementActivity::class.java)
                .putExtra("ZONE_ID", it).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val deadline = SystemClock.elapsedRealtime() + 45000
            var ready = false
            while (!ready && SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync { ready = activity.findViewById<android.view.View>(R.id.btnSeeMoreRec).isEnabled }
                if (!ready) SystemClock.sleep(250)
            }
            assertTrue("Current recommendation did not load on the phone", ready)
            onView(withId(R.id.btnSeeMoreRec)).perform(scrollTo(), click())
            val current = assessments.getJSONObject(0)
            val amountText = "• " + FarmerAssessment.parse(current).howMuchToApply
            onView(withText(amountText)).inRoot(isDialog()).perform(scrollTo()).check(matches(isDisplayed()))
            instrumentation.waitForIdleSync()
            instrumentation.uiAutomation.waitForIdle(500, 5000)
            val image = instrumentation.uiAutomation.takeScreenshot()
            File(activity.getExternalFilesDir(null), "recommendation-amounts-on-phone.png").outputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            image.recycle()
            assertEquals("cropcare-hybrid-advisory-v2.1", current.getString("rulesetVersion"))
            assertTrue(current.getJSONObject("programFertilizerPlan").getJSONArray("entries").length() > 0)
            assertFalse(current.getJSONArray("sensorQuality").toString().contains("accuracy remains unverified"))
        }
    }
}
