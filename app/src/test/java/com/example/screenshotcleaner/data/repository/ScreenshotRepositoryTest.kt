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
            uri = TestUri,
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

private object TestUri : Uri() {
    override fun isHierarchical(): Boolean = true
    override fun isRelative(): Boolean = false
    override fun getScheme(): String = "content"
    override fun getSchemeSpecificPart(): String = "//screenshots/test"
    override fun getEncodedSchemeSpecificPart(): String = "//screenshots/test"
    override fun getAuthority(): String = "screenshots"
    override fun getEncodedAuthority(): String = "screenshots"
    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null
    override fun getHost(): String = "screenshots"
    override fun getPort(): Int = -1
    override fun getPath(): String = "/test"
    override fun getEncodedPath(): String = "/test"
    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null
    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getPathSegments(): List<String> = listOf("test")
    override fun getLastPathSegment(): String = "test"
    override fun buildUpon(): Builder = Builder()
    override fun toString(): String = "content://screenshots/test"
}
