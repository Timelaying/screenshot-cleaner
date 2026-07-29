package com.example.screenshotcleaner.data.media

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.test.mock.MockContentResolver
import com.example.screenshotcleaner.domain.ScreenshotItem
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ScreenshotScannerTest {
    @Test
    @Config(sdk = [35])
    fun returnsScreenshotsAtOrBeforeThirtyDayCutoff() {
        val provider = FakeMediaStoreProvider(
            rows = listOf(
                row(id = 1, dateAddedSeconds = CUTOFF_SECONDS - 1),
                row(id = 2, dateAddedSeconds = CUTOFF_SECONDS),
                row(id = 3, dateAddedSeconds = CUTOFF_SECONDS + 1)
            )
        )
        val scanner = scannerWith(provider)

        val screenshots = scanner.findOldScreenshots(ageDays = 30)

        assertEquals(listOf(1L, 2L), screenshots.map(ScreenshotItem::id))
        assertEquals(
            "${MediaStore.Images.Media.DATE_ADDED} <= ?",
            provider.lastSelection
        )
        assertEquals(arrayOf(CUTOFF_SECONDS.toString()).toList(), provider.lastSelectionArgs)
    }

    @Test
    @Config(sdk = [28])
    fun doesNotRequestRelativePathBeforeAndroidTen() {
        val provider = FakeMediaStoreProvider(rows = listOf(row(id = 1, dateAddedSeconds = CUTOFF_SECONDS)))

        scannerWith(provider).findOldScreenshots(ageDays = 30)

        assertFalse(provider.lastProjection!!.contains(MediaStore.Images.Media.RELATIVE_PATH))
    }

    private fun scannerWith(provider: FakeMediaStoreProvider): ScreenshotScanner {
        val resolver = MockContentResolver().apply {
            addProvider(MediaStore.AUTHORITY, provider)
        }
        val context = object : ContextWrapper(RuntimeEnvironment.getApplication()) {
            override fun getContentResolver() = resolver
        }
        return ScreenshotScanner(
            context = context,
            clock = Clock.fixed(CLOCK_INSTANT, ZoneOffset.UTC)
        )
    }

    private fun row(id: Long, dateAddedSeconds: Long): MediaStoreRow {
        return MediaStoreRow(
            id = id,
            displayName = "Screenshot_$id.png",
            dateAddedSeconds = dateAddedSeconds,
            dateModifiedSeconds = dateAddedSeconds,
            relativePath = "Pictures/Screenshots/"
        )
    }

    private companion object {
        val CLOCK_INSTANT: Instant = Instant.parse("2026-07-29T00:00:00Z")
        const val CUTOFF_SECONDS: Long = 1782691200
    }
}

private data class MediaStoreRow(
    val id: Long,
    val displayName: String,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long,
    val relativePath: String
)

private class FakeMediaStoreProvider(
    private val rows: List<MediaStoreRow>
) : ContentProvider() {
    var lastProjection: Array<out String>? = null
        private set
    var lastSelection: String? = null
        private set
    var lastSelectionArgs: Array<out String>? = null
        private set

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        lastProjection = projection
        lastSelection = selection
        lastSelectionArgs = selectionArgs

        val cutoff = selectionArgs?.firstOrNull()?.toLong()
        return MatrixCursor(projection.orEmpty().toTypedArray()).apply {
            rows.filter { cutoff == null || it.dateAddedSeconds <= cutoff }
                .forEach { row ->
                    addRow(projection.orEmpty().map { column ->
                        when (column) {
                            MediaStore.Images.Media._ID -> row.id
                            MediaStore.Images.Media.DISPLAY_NAME -> row.displayName
                            MediaStore.Images.Media.DATE_ADDED -> row.dateAddedSeconds
                            MediaStore.Images.Media.DATE_MODIFIED -> row.dateModifiedSeconds
                            MediaStore.Images.Media.RELATIVE_PATH -> row.relativePath
                            else -> null
                        }
                    }.toTypedArray())
                }
        }
    }

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? = null
}
