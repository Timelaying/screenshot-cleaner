package com.example.screenshotcleaner.data.media

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.provider.MediaStore
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28, 30, 35], application = Application::class, manifest = Config.NONE)
class ScreenshotScannerQueryTest {
    private lateinit var provider: QueryResultProvider
    private lateinit var scanner: ScreenshotScanner

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        provider = QueryResultProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = MediaStore.AUTHORITY })
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        scanner = ScreenshotScanner(
            context,
            Clock.fixed(Instant.parse("2026-10-10T12:00:00Z"), ZoneOffset.UTC)
        )
    }

    @Test
    fun nullQueryResultIsReportedAsFailure() {
        provider.result = null

        assertThrows(IllegalStateException::class.java) {
            scanner.findOldScreenshots(ageDays = 30)
        }
    }

    @Test
    fun successfulEmptyQueryReturnsNoScreenshotsAndClosesCursor() {
        val cursor = screenshotCursor()
        provider.result = cursor

        assertTrue(scanner.findOldScreenshots(ageDays = 30).isEmpty())
        assertTrue(cursor.isClosed)
    }

    @Test
    fun successfulQueryReturnsScreenshotAndClosesCursor() {
        val cursor = screenshotCursor().apply {
            addRow(arrayOf(42L, "Screenshot_42.png", 1L, 1L))
        }
        provider.result = cursor

        assertEquals(listOf(42L), scanner.findOldScreenshots(ageDays = 30).map { it.id })
        assertTrue(cursor.isClosed)
    }

    @Test
    fun malformedQueryResultPropagatesFailureAndClosesCursor() {
        val cursor = MatrixCursor(arrayOf(MediaStore.Images.Media._ID))
        provider.result = cursor

        assertThrows(IllegalArgumentException::class.java) {
            scanner.findOldScreenshots(ageDays = 30)
        }
        assertTrue(cursor.isClosed)
    }

    private fun screenshotCursor() = MatrixCursor(
        arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED
        )
    )
}

private class QueryResultProvider : ContentProvider() {
    var result: Cursor? = null

    override fun onCreate(): Boolean = true

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = result

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Unexpected insert")
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        error("Unexpected delete")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = error("Unexpected update")
}
