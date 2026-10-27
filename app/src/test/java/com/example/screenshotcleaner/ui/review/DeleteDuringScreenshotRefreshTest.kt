package com.example.screenshotcleaner.ui.review

import android.net.Uri
import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class DeleteDuringScreenshotRefreshTest {
    @Test
    fun delayedSnapshotCannotRestoreDeletedOrKeptScreenshots() = runTest {
        val deleted = screenshot(1)
        val kept = screenshot(2)
        val remaining = screenshot(3)
        var queue = listOf(deleted, kept, remaining)
        val releaseSnapshot = CompletableDeferred<Unit>()
        val refresh = ScreenshotRefresh(this) {
            val snapshot = queue.toList()
            releaseSnapshot.await()
            snapshot
        }

        refresh.refresh(30, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        // Successful delete and Keep saves may finish while the same scan is in flight.
        for (item in listOf(deleted, kept, deleted)) {
            refresh.onDecisionSaved(item)
            queue = queue.pendingAfterDecision(item)
        }
        releaseSnapshot.complete(Unit)
        runCurrent()

        assertEquals(listOf(remaining), queue)
    }

    @Test
    fun deleteCompletionPreservesTheNewAgeFilterQueue() = runTest {
        val deleted = screenshot(1)
        val onlyInOldFilter = screenshot(2)
        val remaining = screenshot(3)
        val releaseOldScan = CompletableDeferred<Unit>()
        var queue = listOf(deleted, onlyInOldFilter, remaining)
        val refresh = ScreenshotRefresh(this) { ageDays ->
            if (ageDays == 30L) {
                withContext(NonCancellable) { releaseOldScan.await() }
                listOf(deleted, onlyInOldFilter, remaining)
            } else {
                listOf(deleted.copy(displayName = "Renamed"), remaining)
            }
        }

        refresh.refresh(30, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        refresh.refresh(60, { queue = it }, { throw AssertionError(it) })
        runCurrent()
        // The delete save finishes after the user changes the filter.
        refresh.onDecisionSaved(deleted)
        queue = queue.pendingAfterDecision(deleted)
        releaseOldScan.complete(Unit)
        runCurrent()

        assertEquals(listOf(remaining), queue)
    }

    @Test
    fun failedRefreshDoesNotUndoSuccessfulDeleteAndKeep() = runTest {
        val deleted = screenshot(1)
        val kept = screenshot(2)
        val remaining = screenshot(3)
        var queue = listOf(deleted, kept, remaining)
        val releaseScan = CompletableDeferred<Unit>()
        val failure = SecurityException("Access changed")
        val errors = mutableListOf<Throwable>()
        val refresh = ScreenshotRefresh(this) {
            releaseScan.await()
            throw failure
        }

        refresh.refresh(30, { queue = it }, { errors += it })
        runCurrent()
        for (item in listOf(kept, deleted)) {
            refresh.onDecisionSaved(item)
            queue = queue.pendingAfterDecision(item)
        }
        releaseScan.complete(Unit)
        runCurrent()

        assertEquals(listOf(remaining), queue)
        assertEquals(listOf(failure), errors)
    }

    private fun screenshot(id: Long) = ScreenshotItem(
        id = id,
        uri = Uri.parse("content://media/external/images/media/$id"),
        displayName = "Screenshot_$id.png",
        dateAddedSeconds = 0L,
        dateModifiedSeconds = 0L
    )
}
