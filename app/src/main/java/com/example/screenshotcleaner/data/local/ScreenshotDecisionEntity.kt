package com.example.screenshotcleaner.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.screenshotcleaner.domain.ScreenshotDecision

@Entity(tableName = "screenshot_decisions")
data class ScreenshotDecisionEntity(
    @PrimaryKey val mediaId: Long,
    val decision: ScreenshotDecision,
    val decidedAtMillis: Long
)

