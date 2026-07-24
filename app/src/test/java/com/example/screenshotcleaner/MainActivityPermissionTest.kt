package com.example.screenshotcleaner

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
}
