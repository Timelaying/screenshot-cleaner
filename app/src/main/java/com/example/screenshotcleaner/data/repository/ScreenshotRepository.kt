package com.example.screenshotcleaner.data.repository

import com.example.screenshotcleaner.data.local.ScreenshotDecisionDao
import com.example.screenshotcleaner.data.local.ScreenshotDecisionEntity
import com.example.screenshotcleaner.data.media.ScreenshotDataSource
import com.example.screenshotcleaner.data.settings.DEFAULT_SCREENSHOT_AGE_DAYS
import com.example.screenshotcleaner.domain.ScreenshotDecision
import com.example.screenshotcleaner.domain.ScreenshotItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ScreenshotRepository(
    private val scanner: ScreenshotDataSource,
    private val decisionDao: ScreenshotDecisionDao,
    private val scanDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    suspend fun getPendingOldScreenshots(
        ageDays: Long = DEFAULT_SCREENSHOT_AGE_DAYS
    ): List<ScreenshotItem> = withContext(scanDispatcher) {
        // Keep blocking MediaStore queries and cursor iteration on the scan dispatcher.
        val screenshots = scanner.findOldScreenshots(ageDays)
        // Include decisions saved while the scan was running before returning the review queue.
        val decidedIds = decisionDao.mediaIdsForDecisions(
            listOf(ScreenshotDecision.KEPT, ScreenshotDecision.DELETED)
        ).toSet()

        screenshots.filterNot { it.id in decidedIds }
    }

    suspend fun keep(item: ScreenshotItem) {
        saveDecision(item.id, ScreenshotDecision.KEPT)
    }

    suspend fun markDeleted(item: ScreenshotItem) {
        saveDecision(item.id, ScreenshotDecision.DELETED)
    }

    private suspend fun saveDecision(mediaId: Long, decision: ScreenshotDecision) {
        decisionDao.upsertDecision(
            ScreenshotDecisionEntity(
                mediaId = mediaId,
                decision = decision,
                decidedAtMillis = System.currentTimeMillis()
            )
        )
    }
}
