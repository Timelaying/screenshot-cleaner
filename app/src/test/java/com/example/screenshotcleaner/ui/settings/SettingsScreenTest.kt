package com.example.screenshotcleaner.ui.settings

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.screenshotcleaner.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 35], application = Application::class)
class SettingsScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val showSettings = mutableStateOf(true)
    private var backCalls = 0
    private var fallbackCalls = 0
    private var settingsWrites = 0

    @Test
    fun systemBackReturnsToReviewAndStopsInterceptingAfterLeavingSettings() {
        showScreen()

        pressSystemBack()
        assertReturnedToReview()
        compose.runOnIdle { assertEquals(0, fallbackCalls) }

        pressSystemBack()
        compose.runOnIdle {
            assertEquals(1, fallbackCalls)
            assertEquals(1, backCalls)
        }
    }

    @Test
    fun doneReturnsToReviewWithoutWritingSettings() {
        showScreen()

        compose.onNodeWithText("Done").performScrollTo().performClick()

        assertReturnedToReview()
    }

    @Test
    fun reviewButtonReturnsToReviewWithoutWritingSettings() {
        showScreen()

        compose.onNodeWithText("Review").performScrollTo().performClick()

        assertReturnedToReview()
    }

    private fun showScreen() {
        compose.runOnUiThread {
            // Stand in for the Activity's normal Back behavior beneath Settings.
            compose.activity.onBackPressedDispatcher.addCallback(
                compose.activity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() {
                        fallbackCalls++
                    }
                }
            )
        }
        compose.setContent {
            MaterialTheme {
                if (showSettings.value) {
                    SettingsScreen(
                        settings = AppSettings(),
                        errorMessage = null,
                        onRemindersEnabledChange = { settingsWrites++ },
                        onScreenshotAgeDaysChange = { settingsWrites++ },
                        onBack = {
                            backCalls++
                            showSettings.value = false
                        }
                    )
                } else {
                    Text("Screenshot review")
                }
            }
        }
    }

    private fun pressSystemBack() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
    }

    private fun assertReturnedToReview() {
        compose.onNodeWithText("Screenshot review").assertIsDisplayed()
        compose.onNodeWithText("Settings").assertDoesNotExist()
        compose.runOnIdle {
            assertEquals(1, backCalls)
            assertEquals(0, settingsWrites)
            assertFalse(compose.activity.isFinishing)
        }
    }
}
