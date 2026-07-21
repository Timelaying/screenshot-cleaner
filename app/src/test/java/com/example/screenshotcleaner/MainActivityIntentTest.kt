package com.example.screenshotcleaner

import android.content.Intent
import com.example.screenshotcleaner.notification.ScreenshotNotificationManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MainActivityIntentTest {
    @Test
    fun notificationIntentOpensReviewScreen() {
        val intent = Intent().setAction(ScreenshotNotificationManager.ACTION_REVIEW_SCREEN)

        assertTrue(intent.isReviewScreenIntent())
    }

    @Test
    fun unrelatedIntentDoesNotOpenReviewScreen() {
        val intent = Intent().setAction("com.example.screenshotcleaner.action.OTHER")

        assertFalse(intent.isReviewScreenIntent())
    }
}
