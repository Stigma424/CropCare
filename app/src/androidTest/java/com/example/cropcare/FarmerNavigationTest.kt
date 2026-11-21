package com.example.cropcare

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FarmerNavigationTest {
    @Test fun farmerActivitiesAreRegisteredAndInflatable() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        listOf(LoginActivity::class.java, RegisterActivity::class.java, ForgotPasswordActivity::class.java,
            SubscriptionActivity::class.java, NotificationsActivity::class.java, SensorHistoryActivity::class.java,
            ZoneManagementActivity::class.java, SensorStatusActivity::class.java,
            SensorDetailActivity::class.java, DeviceManagementActivity::class.java).forEach { activity ->
            assertNotNull(context.packageManager.resolveActivity(Intent(context, activity), 0))
        }
        instrumentation.runOnMainSync {
            val themed = android.view.ContextThemeWrapper(context, R.style.Theme_CropCare)
            val inflater = android.view.LayoutInflater.from(themed)
            listOf(R.layout.activity_login, R.layout.activity_register, R.layout.activity_email_verification,
                R.layout.activity_reset_password, R.layout.activity_subscription, R.layout.activity_sensor_history,
                R.layout.activity_zone_management, R.layout.dialog_edit_zone,
                R.layout.activity_dashboard, R.layout.activity_settings,
                R.layout.activity_sensor_status, R.layout.activity_device_management, R.layout.activity_sensor_detail,
                R.layout.item_zone_card,
                R.layout.item_sensor).forEach { layout ->
                assertNotNull(inflater.inflate(layout, null))
            }
        }
    }
}
