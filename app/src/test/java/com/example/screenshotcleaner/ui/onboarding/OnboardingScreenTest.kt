package com.example.screenshotcleaner.ui.onboarding

import android.app.Application
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35], application = Application::class)
class OnboardingScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val imageGranted = mutableStateOf(false)
    private val notificationsGranted = mutableStateOf(false)
    private var imageRequests = 0
    private var notificationRequests = 0

    @Test
    fun missingPhotoAccessCanOpenThisAppsSettings() {
        notificationsGranted.value = true
        showOnboarding()

        openAndCheckSettings()
    }

    @Test
    fun missingNotificationAccessCanOpenThisAppsSettings() {
        imageGranted.value = true
        showOnboarding()

        openAndCheckSettings()
    }

    @Test
    fun permissionButtonsStillRequestAccessAndRecoveryDisappearsAfterGrants() {
        showOnboarding()
        compose.onNodeWithText("Grant access").performScrollTo().performClick()
        compose.onNodeWithText("Enable alerts").performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(1, imageRequests)
            assertEquals(1, notificationRequests)
            assertNull(shadowOf(compose.activity).nextStartedActivity)
            imageGranted.value = true
            notificationsGranted.value = true
        }

        compose.onNodeWithText("Open app settings").assertDoesNotExist()
        compose.onNodeWithText("Grant access").assertDoesNotExist()
        compose.onNodeWithText("Enable alerts").assertDoesNotExist()
    }

    private fun showOnboarding() {
        compose.setContent {
            MaterialTheme {
                OnboardingScreen(
                    hasImagePermission = imageGranted.value,
                    imagePermissionStatus = if (imageGranted.value) "Ready" else "Required",
                    hasNotificationPermission = notificationsGranted.value,
                    screenshotAgeDays = 30,
                    onGrantImagePermission = { imageRequests++ },
                    onGrantNotificationPermission = { notificationRequests++ }
                )
            }
        }
    }

    private fun openAndCheckSettings() {
        compose.onNodeWithText("Open app settings")
            .performScrollTo()
            .assertIsDisplayed()
            .performClick()
        compose.runOnIdle {
            val intent = shadowOf(compose.activity).nextStartedActivity
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
            assertEquals("package:${compose.activity.packageName}", intent.dataString)
            assertEquals(0, imageRequests)
            assertEquals(0, notificationRequests)
        }
    }
}
