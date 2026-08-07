package com.example.screenshotcleaner.worker

import android.os.Build
import com.example.screenshotcleaner.hasRequiredMediaAccess
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotScanWorkerTest {
    @Test
    fun notificationIsShownWhenOldScreenshotsExist() {
        assertTrue(shouldNotifyForOldScreenshots(count = 1))
    }

    @Test
    fun notificationIsSkippedWhenNoOldScreenshotsExist() {
        assertFalse(shouldNotifyForOldScreenshots(count = 0))
    }

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

    @Test
    fun legacyScanRequiresWriteAccessAlongsideReadAccess() {
        assertTrue(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = true, sdkInt = 29))
        assertFalse(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = false, sdkInt = 29))
    }

    @Test
    fun modernScanDoesNotRequireLegacyWriteAccess() {
        assertTrue(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = false, sdkInt = Build.VERSION_CODES.R))
    }
}
