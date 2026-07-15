package com.example.screenshotcleaner.data.media

import com.example.screenshotcleaner.domain.ScreenshotItem

interface ScreenshotDataSource {
    fun findOldScreenshots(ageDays: Long = 30): List<ScreenshotItem>
}
