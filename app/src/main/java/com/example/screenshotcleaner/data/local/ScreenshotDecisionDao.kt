package com.example.screenshotcleaner.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.screenshotcleaner.domain.ScreenshotDecision

@Dao
interface ScreenshotDecisionDao {
    @Query("SELECT mediaId FROM screenshot_decisions WHERE decision IN (:decisions)")
    suspend fun mediaIdsForDecisions(decisions: List<ScreenshotDecision>): List<Long>

    @Upsert
    suspend fun upsertDecision(decision: ScreenshotDecisionEntity)
}

