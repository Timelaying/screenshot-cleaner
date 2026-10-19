package com.example.screenshotcleaner.ui.review

import android.net.Uri
import com.example.screenshotcleaner.domain.ScreenshotItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReviewQueueTest {
    @Test
    fun repeatedKeepCompletionDoesNotSkipTheNextScreenshot() {
        val kept = screenshot(1)
        val next = screenshot(2)
        val queue = listOf(kept, next)

        val afterFirstSave = queue.pendingAfterKeeping(kept)
        val afterSecondSave = afterFirstSave.pendingAfterKeeping(kept)

        assertEquals(listOf(next), afterFirstSave)
        assertEquals(listOf(next), afterSecondSave)
    }

    @Test
    fun keepCompletionRemovesOnlyItsItemFromARefreshedQueue() {
        val kept = screenshot(1)
        val newlyFound = screenshot(3)
        val next = screenshot(2)
        val refreshedKept = kept.copy(displayName = "Renamed screenshot")
        val refreshedQueue = listOf(newlyFound, refreshedKept, next)

        assertEquals(
            listOf(newlyFound, next),
            refreshedQueue.pendingAfterKeeping(kept)
        )
    }

    @Test
    fun keepCompletionPreservesAQueueThatNoLongerContainsTheItem() {
        val kept = screenshot(1)
        val next = screenshot(2)

        assertEquals(listOf(next), listOf(next).pendingAfterKeeping(kept))
        assertEquals(emptyList<ScreenshotItem>(), emptyList<ScreenshotItem>().pendingAfterKeeping(kept))
    }

    private fun screenshot(id: Long) = ScreenshotItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "Screenshot_$id.png",
        dateAddedSeconds = 0L,
        dateModifiedSeconds = 0L
    )
}
