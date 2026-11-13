package com.example.cropcare

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.os.SystemClock
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matchers.startsWith
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File

/** Opt-in device acceptance test. Supply demo credentials as runner arguments, never source. */
class LiveRecommendationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun awaitActivity(name: String): Activity {
        val deadline = SystemClock.elapsedRealtime() + 45000
        while (SystemClock.elapsedRealtime() < deadline) {
            var activity: Activity? = null
            instrumentation.runOnMainSync {
                activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it.javaClass.simpleName == name }
            }
            if (activity != null) return activity!!
            SystemClock.sleep(250)
        }
        error("Activity did not become ready: $name")
    }
    private fun awaitText(value: String) {
        val deadline = SystemClock.elapsedRealtime() + 45000
        var failure: Throwable? = null
        while (SystemClock.elapsedRealtime() < deadline) {
            try { onView(withText(value)).check(matches(isDisplayed())); return }
            catch (e: Throwable) { failure = e; SystemClock.sleep(250) }
        }
        throw AssertionError("Expected live recommendation text: $value", failure)
    }
    private fun screenshot(name: String) {
        instrumentation.waitForIdleSync()
        val image = instrumentation.uiAutomation.takeScreenshot()
        assertNotNull(image)
        File(instrumentation.targetContext.getExternalFilesDir(null), name).outputStream().use {
            image.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        image.recycle()
    }
    @Test fun demoAccountLoadsCurrentRecommendationAndExpandableDetails() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("liveDemo") == "true")
        val username = requireNotNull(args.getString("demoUsername"))
        val password = requireNotNull(args.getString("demoPassword"))
        val zone = requireNotNull(args.getString("demoZoneId"))
        val context = instrumentation.targetContext
        instrumentation.startActivitySync(Intent(context, LoginActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        onView(withId(R.id.etLoginUsername)).perform(replaceText(username))
        onView(withId(R.id.etLoginPassword)).perform(replaceText(password), closeSoftKeyboard())
        onView(withId(R.id.btnLogin)).perform(click())
        awaitActivity("DashboardActivity")
        instrumentation.startActivitySync(Intent(context, ZoneManagementActivity::class.java)
            .putExtra("ZONE_ID", zone).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        awaitActivity("ZoneManagementActivity")
        val deadline = SystemClock.elapsedRealtime() + 45000
        while (SystemClock.elapsedRealtime() < deadline) {
            var ready = false
            instrumentation.runOnMainSync {
                val activity = ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED)
                    .firstOrNull { it is ZoneManagementActivity }
                ready = activity?.findViewById<android.view.View>(R.id.btnSeeMoreRec)?.isEnabled == true
            }
            if (ready) break
            SystemClock.sleep(250)
        }
        onView(withId(R.id.btnSeeMoreRec)).perform(scrollTo(), click())
        awaitText("YOUR NEXT STEP")
        screenshot("recommendation-live-summary.png")
        onView(withText(startsWith("Other checks"))).perform(scrollTo(), click())
        screenshot("recommendation-live-expanded.png")
        onView(withText(startsWith("Other checks"))).perform(scrollTo(), click())
        onView(withText(startsWith("Sensor readings and quality"))).perform(scrollTo(), click())
        onView(withText(startsWith("Sensor 1"))).check(matches(isDisplayed()))
    }
}
