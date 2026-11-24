package com.example.screenshotcleaner.ui.review

import android.app.Application
import android.net.Uri
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.screenshotcleaner.domain.ScreenshotItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ReviewDeleteConfirmationTest {
    @get:Rule
    val compose = createComposeRule()

    private val original = item(1)
    private val screenshots = mutableStateOf(listOf(original, item(2)))
    private val deleted = mutableListOf<ScreenshotItem>()

    @Test
    fun replacingCurrentScreenshotExpiresConfirmationEvenIfCandidateRemainsInQueue() {
        showReview()
        openConfirmation()
        compose.runOnIdle { screenshots.value = listOf(item(2), original) }
        assertConfirmationExpired()

        // Returning to the old screenshot must not resurrect its previous consent.
        compose.runOnIdle { screenshots.value = listOf(original, item(2)) }
        assertConfirmationExpired()
        openConfirmation()
        confirmDeletion()
        compose.runOnIdle { assertEquals(listOf(original), deleted) }
    }

    @Test
    fun clearingQueueExpiresConfirmation() {
        showReview()
        openConfirmation()
        compose.runOnIdle { screenshots.value = emptyList() }
        assertConfirmationExpired()
        compose.onNodeWithText("All done").assertIsDisplayed()
    }

    @Test
    fun changingCurrentScreenshotMetadataRequiresNewConfirmation() {
        showReview()
        openConfirmation()
        val changed = original.copy(dateModifiedSeconds = 2L)
        compose.runOnIdle { screenshots.value = listOf(changed) }
        assertConfirmationExpired()
        openConfirmation()
        confirmDeletion()
        compose.runOnIdle { assertEquals(listOf(changed), deleted) }
    }

    @Test
    fun unchangedCurrentScreenshotKeepsConfirmationAcrossRefresh() {
        showReview()
        openConfirmation()
        compose.runOnIdle { screenshots.value = listOf(original.copy(), item(3)) }
        compose.onNodeWithText("Delete this screenshot?").assertIsDisplayed()
        confirmDeletion()
        compose.onNodeWithText("Delete this screenshot?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf(original), deleted) }
    }

    @Test
    fun cancellingConfirmationNeverDeletesScreenshot() {
        showReview()
        openConfirmation()
        compose.onNodeWithText("Cancel").performClick()
        assertConfirmationExpired()
    }

    private fun showReview() {
        compose.setContent {
            MaterialTheme {
                ReviewScreen(
                    screenshots = screenshots.value,
                    errorMessage = null,
                    onKeep = {},
                    onDelete = { deleted += it },
                    onRefresh = {},
                    onOpenSettings = {}
                )
            }
        }
    }

    private fun openConfirmation() {
        compose.onNodeWithText("Delete").performClick()
        compose.onNodeWithText("Delete this screenshot?").assertIsDisplayed()
        compose.runOnIdle { assertEquals(emptyList<ScreenshotItem>(), deleted) }
    }

    private fun confirmDeletion() {
        compose.onNode(hasText("Delete") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun assertConfirmationExpired() {
        compose.onNodeWithText("Delete this screenshot?").assertDoesNotExist()
        compose.runOnIdle { assertEquals(emptyList<ScreenshotItem>(), deleted) }
    }

    private fun item(id: Long) = ScreenshotItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "Screenshot_$id.png",
        dateAddedSeconds = 1L,
        dateModifiedSeconds = 1L
    )
}
