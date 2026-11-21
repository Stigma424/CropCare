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
            val details = activity.findViewById<View>(R.id.pairingDetails)
            val deviceId = activity.findViewById<android.widget.EditText>(R.id.etSensorId)
            val sensorName = activity.findViewById<View>(R.id.etSensorName)
            val save = activity.findViewById<View>(R.id.btnSave)
            instrumentation.runOnMainSync {
                assertEquals(View.GONE, details.visibility)
                assertFalse(save.isEnabled)
            }
            capture("pairing-before-scan.png")
            val process = AddSensorActivity::class.java.getDeclaredMethod("processQrResult", String::class.java).apply { isAccessible = true }
            instrumentation.runOnMainSync {
                process.invoke(activity, "CROPCARE1|QR_FORMAT_TEST|0123456789ABCDEF0123456789ABCDEF")
                assertEquals(View.VISIBLE, details.visibility)
                assertEquals(View.VISIBLE, deviceId.visibility)
                assertEquals(View.VISIBLE, sensorName.visibility)
                assertEquals("QR_FORMAT_TEST", deviceId.text.toString())
                assertFalse(deviceId.isEnabled)
                assertTrue(save.isEnabled)
            }
            capture("pairing-valid-format-check.png")
            instrumentation.runOnMainSync {
                process.invoke(activity, "invalid")
                assertEquals(View.GONE, details.visibility)
                assertTrue(deviceId.text.isEmpty())
                assertFalse(save.isEnabled)
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
}
