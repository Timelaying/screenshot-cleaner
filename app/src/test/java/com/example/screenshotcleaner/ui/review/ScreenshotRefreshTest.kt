package com.example.screenshotcleaner.ui.review

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScreenshotRefreshTest {
    @Test
    fun olderScanFinishingLastCannotReplaceTheNewAgeFilterResults() = runTest {
        val releaseOldScan = CompletableDeferred<Unit>()
        val publishedAges = mutableListOf<Long>()
        val refresh = ScreenshotRefresh(this) { ageDays ->
            if (ageDays == 30L) withContext(NonCancellable) { releaseOldScan.await() }
            emptyList()
        }

        refresh.refresh(30, { publishedAges += 30L }, { throw AssertionError(it) })
        runCurrent()
        refresh.refresh(60, { publishedAges += 60L }, { throw AssertionError(it) })
        runCurrent()
        assertEquals(listOf(60L), publishedAges)

        releaseOldScan.complete(Unit)
        runCurrent()
        assertEquals(listOf(60L), publishedAges)
    }

    @Test
    fun supersededScanFailureCannotReplaceSuccessfulRefreshWithAnError() = runTest {
        val releaseOldScan = CompletableDeferred<Unit>()
        val errors = mutableListOf<Throwable>()
        var completed = false
        val refresh = ScreenshotRefresh(this) { ageDays ->
            if (ageDays == 30L) {
                withContext(NonCancellable) { releaseOldScan.await() }
                throw IllegalStateException("Old query failed")
            }
            emptyList()
        }

        refresh.refresh(30, { throw AssertionError("Stale result") }, { errors += it })
        runCurrent()
        refresh.refresh(60, { completed = true }, { errors += it })
        runCurrent()
        releaseOldScan.complete(Unit)
        runCurrent()

        assertTrue(completed)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun currentScanFailureIsReportedAndNextRefreshCanRecover() = runTest {
        val failure = SecurityException("Access revoked")
        val errors = mutableListOf<Throwable>()
        var failScan = true
        var completed = false
        val refresh = ScreenshotRefresh(this) {
            if (failScan) throw failure
            emptyList()
        }

        refresh.refresh(30, { completed = true }, { errors += it })
        runCurrent()
        assertEquals(listOf(failure), errors)

        failScan = false
        refresh.refresh(30, { completed = true }, { errors += it })
        runCurrent()
        assertTrue(completed)
        assertEquals(listOf(failure), errors)
    }

    @Test
    fun cancellationIsNotShownAsAReviewError() = runTest {
        val refresh = ScreenshotRefresh(this) { throw CancellationException("Cancelled") }
        refresh.refresh(30, { throw AssertionError("Unexpected result") }, { throw AssertionError(it) })
        runCurrent()
    }

    @Test
    fun leavingCompositionPreventsLateScanCallbacks() = runTest {
        val owner = CoroutineScope(coroutineContext + SupervisorJob(coroutineContext[Job]))
        val releaseScan = CompletableDeferred<Unit>()
        val refresh = ScreenshotRefresh(owner) {
            withContext(NonCancellable) { releaseScan.await() }
            emptyList()
        }

        refresh.refresh(30, { throw AssertionError("Disposed result") }, { throw AssertionError(it) })
        runCurrent()
        owner.cancel()
        releaseScan.complete(Unit)
        runCurrent()
    }
}
