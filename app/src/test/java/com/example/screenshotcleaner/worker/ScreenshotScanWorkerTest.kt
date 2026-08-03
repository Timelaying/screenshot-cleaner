package com.example.screenshotcleaner.worker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotScanWorkerTest {
    @Test
    fun scanRunsOnlyWhenAllPrerequisitesAreAvailable() {
        assertTrue(
            shouldRunScreenshotScan(
                hasFullImageAccess = true,
                hasNotificationPermission = true,
                remindersEnabled = true
            )
        )
    }

    @Test
    fun scanSkipsWhenImageAccessIsMissing() {
        assertFalse(
            shouldRunScreenshotScan(
                hasFullImageAccess = false,
                hasNotificationPermission = true,
                remindersEnabled = true
            )
        )
    }

    @Test
    fun scanSkipsWhenNotificationsAreUnavailable() {
        assertFalse(
            shouldRunScreenshotScan(
                hasFullImageAccess = true,
                hasNotificationPermission = false,
                remindersEnabled = true
            )
        )
    }

    @Test
    fun scanSkipsWhenRemindersArePaused() {
        assertFalse(
            shouldRunScreenshotScan(
                hasFullImageAccess = true,
                hasNotificationPermission = true,
                remindersEnabled = false
            )
        )
    }
}
