package com.example.screenshotcleaner.data.repository

import android.net.Uri
import com.example.screenshotcleaner.data.local.ScreenshotDecisionDao
import com.example.screenshotcleaner.data.local.ScreenshotDecisionEntity
import com.example.screenshotcleaner.data.media.ScreenshotDataSource
import com.example.screenshotcleaner.domain.ScreenshotDecision
import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotRepositoryTest {
    @Test
    fun blockingScanRunsOffTheCallingThreadByDefault() = runTest {
        val callerThread = Thread.currentThread()
        var scannerThread: Thread? = null
        val scanner = object : ScreenshotDataSource {
            override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> {
                scannerThread = Thread.currentThread()
                return listOf(screenshot(id = 1))
            }
        }

        val pending = ScreenshotRepository(scanner, FakeScreenshotDecisionDao())
            .getPendingOldScreenshots()

        assertEquals(listOf(1L), pending.map { it.id })
        assertNotSame(callerThread, scannerThread)
    }

    @Test
    fun backgroundScanFailureReachesTheCaller() = runTest {
        val failure = SecurityException("Media access revoked")
        val scanner = object : ScreenshotDataSource {
            override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> = throw failure
        }

        try {
            ScreenshotRepository(scanner, FakeScreenshotDecisionDao()).getPendingOldScreenshots()
            fail("Expected the scan failure")
        } catch (exception: SecurityException) {
            assertEquals(failure.message, exception.message)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun cancelledQueuedScanDoesNotQueryMediaStore() = runTest {
        var scanned = false
        val scanner = object : ScreenshotDataSource {
            override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> {
                scanned = true
                return emptyList()
            }
        }
        val repository = ScreenshotRepository(
            scanner,
            FakeScreenshotDecisionDao(),
            StandardTestDispatcher(testScheduler)
        )

        val job = launch(start = CoroutineStart.UNDISPATCHED) {
            repository.getPendingOldScreenshots()
        }
        assertFalse(scanned)
        job.cancel()
        runCurrent()
        job.cancelAndJoin()

        assertFalse(scanned)
    }

    @Test
    fun pendingScreenshotsExcludeKeptAndDeletedItems() = runTest {
        val scanner = FakeScreenshotDataSource(
            listOf(
                screenshot(id = 1),
                screenshot(id = 2),
                screenshot(id = 3)
            )
        )
        val dao = FakeScreenshotDecisionDao(
            ScreenshotDecisionEntity(1, ScreenshotDecision.KEPT, decidedAtMillis = 10),
            ScreenshotDecisionEntity(2, ScreenshotDecision.DELETED, decidedAtMillis = 20)
        )
        val repository = ScreenshotRepository(scanner, dao)

        val pending = repository.getPendingOldScreenshots()

        assertEquals(listOf(3L), pending.map { it.id })
    }

    @Test
    fun screenshotKeptDuringScanDoesNotReappear() = runTest {
        assertDecisionSavedDuringScanIsExcluded(ScreenshotDecision.KEPT)
    }

    @Test
    fun screenshotDeletedDuringScanDoesNotReappear() = runTest {
        assertDecisionSavedDuringScanIsExcluded(ScreenshotDecision.DELETED)
    }

    private suspend fun assertDecisionSavedDuringScanIsExcluded(decision: ScreenshotDecision) {
        val decidedItem = screenshot(id = 1)
        val undecidedItem = screenshot(id = 2)
        val dao = FakeScreenshotDecisionDao()
        val scanner = object : ScreenshotDataSource {
            override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> {
                // Model a decision committed after scanning starts but before it returns.
                dao.decisions[decidedItem.id] = ScreenshotDecisionEntity(
                    mediaId = decidedItem.id,
                    decision = decision,
                    decidedAtMillis = 10
                )
                return listOf(decidedItem, undecidedItem)
            }
        }

        val pending = ScreenshotRepository(scanner, dao).getPendingOldScreenshots()

        assertEquals(listOf(undecidedItem), pending)
    }

    @Test
    fun keepStoresDecisionAndRemovesItemFromPendingResults() = runTest {
        val item = screenshot(id = 42)
        val scanner = FakeScreenshotDataSource(listOf(item))
        val dao = FakeScreenshotDecisionDao()
        val repository = ScreenshotRepository(scanner, dao)

        repository.keep(item)

        assertEquals(emptyList<ScreenshotItem>(), repository.getPendingOldScreenshots())
        assertEquals(ScreenshotDecision.KEPT, dao.decisions.getValue(42).decision)
    }

    @Test
    fun markDeletedStoresDecisionAndRemovesItemFromPendingResults() = runTest {
        val item = screenshot(id = 7)
        val scanner = FakeScreenshotDataSource(listOf(item))
        val dao = FakeScreenshotDecisionDao()
        val repository = ScreenshotRepository(scanner, dao)

        repository.markDeleted(item)

        assertEquals(emptyList<ScreenshotItem>(), repository.getPendingOldScreenshots())
        assertEquals(ScreenshotDecision.DELETED, dao.decisions.getValue(7).decision)
    }

    @Test
    fun forwardsConfiguredAgeThresholdToScanner() = runTest {
        val scanner = RecordingScreenshotDataSource()
        val repository = ScreenshotRepository(scanner, FakeScreenshotDecisionDao())

        repository.getPendingOldScreenshots(ageDays = 90L)

        assertEquals(90L, scanner.requestedAgeDays)
    }

    private fun screenshot(id: Long): ScreenshotItem {
        return ScreenshotItem(
            id = id,
            uri = Uri.parse("content://screenshots/$id"),
            displayName = "Screenshot_$id.png",
            dateAddedSeconds = id,
            dateModifiedSeconds = id
        )
    }
}

private class FakeScreenshotDataSource(
    private val screenshots: List<ScreenshotItem>
) : ScreenshotDataSource {
    override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> = screenshots
}

private class RecordingScreenshotDataSource : ScreenshotDataSource {
    var requestedAgeDays: Long? = null

    override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> {
        requestedAgeDays = ageDays
        return emptyList()
    }
}

private class FakeScreenshotDecisionDao(
    vararg seedDecisions: ScreenshotDecisionEntity
) : ScreenshotDecisionDao {
    val decisions = seedDecisions.associateBy { it.mediaId }.toMutableMap()

    override suspend fun mediaIdsForDecisions(decisions: List<ScreenshotDecision>): List<Long> {
        return this.decisions.values
            .filter { it.decision in decisions }
            .map { it.mediaId }
    }

    override suspend fun upsertDecision(decision: ScreenshotDecisionEntity) {
        decisions[decision.mediaId] = decision
    }
}
