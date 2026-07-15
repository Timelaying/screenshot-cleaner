package com.example.screenshotcleaner.data.repository

import android.net.Uri
import com.example.screenshotcleaner.data.local.ScreenshotDecisionDao
import com.example.screenshotcleaner.data.local.ScreenshotDecisionEntity
import com.example.screenshotcleaner.data.media.ScreenshotDataSource
import com.example.screenshotcleaner.domain.ScreenshotDecision
import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ScreenshotRepositoryTest {
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
