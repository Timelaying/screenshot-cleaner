package com.example.screenshotcleaner.data.repository

import com.example.screenshotcleaner.data.local.ScreenshotDecisionDao
import com.example.screenshotcleaner.data.local.ScreenshotDecisionEntity
import com.example.screenshotcleaner.data.media.ScreenshotDataSource
import com.example.screenshotcleaner.domain.ScreenshotDecision
import com.example.screenshotcleaner.domain.ScreenshotItem

class ScreenshotRepository(
    private val scanner: ScreenshotDataSource,
    private val decisionDao: ScreenshotDecisionDao
) {
    suspend fun getPendingOldScreenshots(ageDays: Long = 30): List<ScreenshotItem> {
        val decidedIds = decisionDao.mediaIdsForDecisions(
            listOf(ScreenshotDecision.KEPT, ScreenshotDecision.DELETED)
        ).toSet()

        return scanner.findOldScreenshots(ageDays)
            .filterNot { it.id in decidedIds }
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
