package com.example.screenshotcleaner.data.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotNameMatcherTest {
    @Test
    fun matchesScreenshotFolder() {
        assertTrue(
            ScreenshotNameMatcher.looksLikeScreenshot(
                displayName = "IMG_1234.png",
                relativePath = "Pictures/Screenshots/"
            )
        )
    }

    @Test
    fun matchesScreenshotFileName() {
        assertTrue(
            ScreenshotNameMatcher.looksLikeScreenshot(
                displayName = "Screenshot_20260702-114200.png",
                relativePath = "Pictures/"
            )
        )
    }

    @Test
    fun ignoresCameraImages() {
        assertFalse(
            ScreenshotNameMatcher.looksLikeScreenshot(
                displayName = "IMG_1234.jpg",
                relativePath = "DCIM/Camera/"
            )
        )
    }
}

