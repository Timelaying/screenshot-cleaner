package com.example.screenshotcleaner.data.media

import android.provider.MediaStore
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotScannerTest {
    @Test
    fun cutoffIncludesTheThirtyDayBoundary() {
        val now = Instant.parse("2026-07-29T00:00:00Z")

        assertEquals(1782691200L, screenshotCutoffSeconds(now, ageDays = 30))
    }

    @Test
    fun preAndroidTenProjectionOmitsRelativePath() {
        val projection = screenshotProjection(includeRelativePath = false)

        assertFalse(projection.contains(MediaStore.Images.Media.RELATIVE_PATH))
    }

    @Test
    fun modernProjectionIncludesRelativePath() {
        val projection = screenshotProjection(includeRelativePath = true)

        assertTrue(projection.contains(MediaStore.Images.Media.RELATIVE_PATH))
    }
}
