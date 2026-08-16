package com.example.screenshotcleaner

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MainActivityPermissionTest {
    @Test
    fun onboardingIsHiddenOnlyWhenAllRequiredAccessIsGranted() {
        assertFalse(shouldShowOnboarding(ImageAccessState.FULL, hasNotificationPermission = true))
        assertTrue(shouldShowOnboarding(ImageAccessState.FULL, hasNotificationPermission = false))
        assertTrue(shouldShowOnboarding(ImageAccessState.PARTIAL, hasNotificationPermission = true))
        assertTrue(shouldShowOnboarding(ImageAccessState.MISSING, hasNotificationPermission = true))
    }

    @Test
    fun preAndroidElevenRequiresWriteAccessForMediaOperations() {
        assertTrue(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = true, sdkInt = 28))
        assertFalse(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = false, sdkInt = 28))
    }

    @Test
    fun AndroidElevenAndNewerCanUseUserMediatedDeleteWithReadAccess() {
        assertTrue(hasRequiredMediaAccess(hasReadAccess = true, hasWriteAccess = false, sdkInt = 30))
        assertFalse(hasRequiredMediaAccess(hasReadAccess = false, hasWriteAccess = true, sdkInt = 30))
    }

    @Test
    fun preAndroidElevenUsesDirectDelete() {
        assertEquals(DeleteMode.DIRECT, deleteModeForSdk(sdkInt = 29))
    }

    @Test
    fun AndroidElevenAndNewerUsesUserMediatedDelete() {
        assertEquals(DeleteMode.USER_CONFIRMATION, deleteModeForSdk(sdkInt = 30))
        assertEquals(DeleteMode.USER_CONFIRMATION, deleteModeForSdk(sdkInt = 35))
    }
}
