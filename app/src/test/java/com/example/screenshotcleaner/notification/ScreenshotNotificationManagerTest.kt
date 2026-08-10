package com.example.screenshotcleaner.notification

import android.content.Intent
import com.example.screenshotcleaner.MainActivity
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotNotificationManagerTest {
    @Test
    fun reviewIntentTargetsMainActivityWithReuseFlags() {
        val context = RuntimeEnvironment.getApplication()

        val intent = reviewIntent(context)

        assertEquals(ScreenshotNotificationManager.ACTION_REVIEW_SCREEN, intent.action)
        assertEquals(MainActivity::class.java.name, intent.component?.className)
        assertEquals(
            Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP,
            intent.flags
        )
    }
}
