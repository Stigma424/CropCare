package com.example.cropcare

import android.content.Intent
import android.graphics.Bitmap
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/** QR-format checks only: never submits a registration or claims a physical device. */
class PairingVisibilityTest {
    @Test fun deviceFieldsAppearOnlyAfterValidQrAndHideAfterInvalidQr() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, AddSensorActivity::class.java)
            .putExtra("ZONE_ID", "pairing-ui-check").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as AddSensorActivity
        fun capture(name: String) {
            instrumentation.waitForIdleSync()
            val image = instrumentation.uiAutomation.takeScreenshot()
            File(activity.getExternalFilesDir(null), name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
            image.recycle()
        }
        try {
            instrumentation.runOnMainSync {
                assertFalse(activity.findViewById<View>(R.id.pairingDetails).isShown)
                assertFalse(activity.findViewById<View>(R.id.etSensorId).isShown)
                assertFalse(activity.findViewById<View>(R.id.etSensorName).isShown)
                assertFalse(activity.findViewById<View>(R.id.btnSave).isShown)
            }
            capture("pairing-before-scan.png")
            val process = AddSensorActivity::class.java.getDeclaredMethod("processQrResult", String::class.java).apply { isAccessible = true }
            instrumentation.runOnMainSync {
                process.invoke(activity, "CROPCARE1|QR_FORMAT_TEST|0123456789ABCDEF0123456789ABCDEF")
                assertTrue(activity.findViewById<View>(R.id.etSensorId).isShown)
                assertTrue(activity.findViewById<View>(R.id.etSensorName).isShown)
                assertTrue(activity.findViewById<View>(R.id.btnSave).isShown)
            }
            capture("pairing-valid-format-check.png")
            instrumentation.runOnMainSync {
                process.invoke(activity, "invalid")
                assertFalse(activity.findViewById<View>(R.id.pairingDetails).isShown)
                assertFalse(activity.findViewById<View>(R.id.btnSave).isShown)
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
