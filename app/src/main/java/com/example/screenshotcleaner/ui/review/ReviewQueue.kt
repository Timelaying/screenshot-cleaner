package com.example.screenshotcleaner.ui.review

import com.example.screenshotcleaner.domain.ScreenshotItem

// A save can finish after another action or refresh has changed the queue.
internal fun List<ScreenshotItem>.pendingAfterDecision(
    decidedItem: ScreenshotItem
): List<ScreenshotItem> = filterNot { it.id == decidedItem.id }
