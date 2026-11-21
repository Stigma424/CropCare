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
import java.util.concurrent.TimeUnit
import android.os.SystemClock

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
        val sensors = JSONArray(get("/api/sensors"))
        assertTrue("The connected account needs a paired sensor", sensors.length() > 0)
        val sensor = sensors.getJSONObject(0)
        val id = sensor.getString("sensorId")
        val current = JSONObject(get("/api/sensors/$id/assessment?programId=HYB-1"))
        val second = JSONObject(get("/api/sensors/$id/assessment?programId=HYB-2"))
        assertEquals(1, current.getJSONArray("sensorQuality").length())
        assertEquals(id, current.getJSONArray("sensorQuality").getJSONObject(0).getString("sensorId"))
        assertEquals("HYB-2", second.getString("programId"))
        assertTrue(current.getDouble("sensorAssignedAreaSqm") in 0.0..2500.0)
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SensorDetailActivity::class.java)
            .putExtra("SENSOR_ID", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val deadline = SystemClock.elapsedRealtime() + 45000
        var ready = false
        while (!ready && SystemClock.elapsedRealtime() < deadline) {
            instrumentation.runOnMainSync {
                ready = activity.findViewById<android.view.View>(R.id.btnSensorAdviceDetails).visibility == android.view.View.VISIBLE
            }
            if (!ready) SystemClock.sleep(250)
        }
        assertTrue("Current sensor recommendation did not load on the phone", ready)
    }
}
