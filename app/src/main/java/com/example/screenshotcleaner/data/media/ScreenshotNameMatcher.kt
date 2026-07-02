package com.example.screenshotcleaner.data.media

object ScreenshotNameMatcher {
    private val screenshotMarkers = listOf(
        "screenshots",
        "screenshot",
        "screen_shot"
    )

    fun looksLikeScreenshot(displayName: String?, relativePath: String?): Boolean {
        val searchable = listOfNotNull(displayName, relativePath)
            .joinToString(separator = " ")
            .lowercase()

        return screenshotMarkers.any(searchable::contains)
    }
}

