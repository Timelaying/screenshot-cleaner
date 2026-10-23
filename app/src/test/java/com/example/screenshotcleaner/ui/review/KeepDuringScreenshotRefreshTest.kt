package com.example.screenshotcleaner.ui.review

import android.net.Uri
import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class KeepDuringScreenshotRefreshTest {
    @Test
    fun scanSnapshotCannotRestoreItemsKeptBeforePublication() = runTest {
        val first = screenshot(1)
        val second = screenshot(2)
        val remaining = screenshot(3)
        val releaseSnapshot = CompletableDeferred<Unit>()
        var queue = listOf(first, second, remaining)
        val refresh = ScreenshotRefresh(this) {
            val snapshot = listOf(first.copy(displayName = "Renamed"), second, remaining)
            releaseSnapshot.await()
            snapshot
        }

        refresh.refresh(30, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        for (item in listOf(first, first, second)) {
            refresh.onKeepSaved(item)
            queue = queue.pendingAfterKeeping(item)
        }
        assertEquals(listOf(remaining), queue)

        releaseSnapshot.complete(Unit)
        runCurrent()
        assertEquals(listOf(remaining), queue)
    }

    @Test
    fun scanPublishedBeforeKeepIsStillUpdatedByIdentity() = runTest {
        val kept = screenshot(1)
        val remaining = screenshot(2)
        var queue = emptyList<ScreenshotItem>()
        val refresh = ScreenshotRefresh(this) { listOf(kept, remaining) }

        refresh.refresh(30, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        assertEquals(listOf(kept, remaining), queue)
        refresh.onKeepSaved(kept)
        queue = queue.pendingAfterKeeping(kept)

        assertEquals(listOf(remaining), queue)
    }

    @Test
    fun unsuccessfulKeepDoesNotHideTheItemFromAnOverlappingScan() = runTest {
        val item = screenshot(1)
        val releaseSnapshot = CompletableDeferred<Unit>()
        var queue = emptyList<ScreenshotItem>()
        val refresh = ScreenshotRefresh(this) {
            releaseSnapshot.await()
            listOf(item)
        }

        refresh.refresh(30, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        // The failure path does not call onKeepSaved because no Room decision was saved.
        releaseSnapshot.complete(Unit)
        runCurrent()

        assertEquals(listOf(item), queue)
    }

    private fun screenshot(id: Long) = ScreenshotItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "Screenshot_$id.png",
        dateAddedSeconds = 0L,
        dateModifiedSeconds = 0L
    )
}
