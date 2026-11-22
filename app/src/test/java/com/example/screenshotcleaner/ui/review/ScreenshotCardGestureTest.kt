package com.example.screenshotcleaner.ui.review

import android.app.Application
import android.net.Uri
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.example.screenshotcleaner.domain.ScreenshotItem
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class ScreenshotCardGestureTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun rightSwipeUsesLatestCallbackAfterRecomposition() {
        assertLatestCallback(direction = 1f)
    }

    @Test
    fun leftSwipeUsesLatestCallbackAfterRecomposition() {
        assertLatestCallback(direction = -1f)
    }

    private fun assertLatestCallback(direction: Float) {
        val revision = mutableStateOf(0)
        val decisions = mutableListOf<String>()
        compose.setContent {
            val renderedRevision = revision.value
            ScreenshotCard(
                item = item(1),
                modifier = Modifier.size(300.dp).testTag("card"),
                onKeep = { decisions += "keep-$renderedRevision" },
                onDelete = { decisions += "delete-$renderedRevision" }
            )
        }
        // Keep the same media ID and active gesture while the callbacks are replaced.
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveBy(Offset(direction * 100f, 0f))
        }
        compose.runOnIdle { revision.value = 1 }
        compose.onNodeWithTag("card").performTouchInput {
            moveBy(Offset(direction * 200f, 0f))
            up()
        }
        compose.runOnIdle {
            assertEquals(listOf(if (direction > 0) "keep-1" else "delete-1"), decisions)
        }
    }

    @Test
    fun replacingScreenshotCancelsOldGestureAndStartsFresh() {
        val current = mutableStateOf(item(1))
        val decisions = mutableListOf<Long>()
        compose.setContent {
            val renderedItem = current.value
            ScreenshotCard(
                item = renderedItem,
                modifier = Modifier.size(300.dp).testTag("card"),
                onKeep = { decisions += renderedItem.id },
                onDelete = { decisions += -renderedItem.id }
            )
        }
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
        }
        compose.runOnIdle { current.value = item(2) }
        compose.onNodeWithTag("card").performTouchInput { up() }
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveBy(Offset(100f, 0f))
            up()
        }
        compose.runOnIdle { assertEquals(emptyList<Long>(), decisions) }
        compose.onNodeWithTag("card").performTouchInput {
            down(center)
            moveBy(Offset(300f, 0f))
            up()
        }
        compose.runOnIdle { assertEquals(listOf(2L), decisions) }
    }

    private fun item(id: Long) = ScreenshotItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "Screenshot_$id.png",
        dateAddedSeconds = 1L,
        dateModifiedSeconds = 1L
    )
}
