package com.example.screenshotcleaner.data.media

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.example.screenshotcleaner.domain.ScreenshotItem
import java.time.Instant
import java.time.temporal.ChronoUnit

class ScreenshotScanner(
    private val context: Context
) : ScreenshotDataSource {
    override fun findOldScreenshots(ageDays: Long): List<ScreenshotItem> {
        val cutoffSeconds = Instant.now()
            .minus(ageDays, ChronoUnit.DAYS)
            .epochSecond

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val selection = "${MediaStore.Images.Media.DATE_ADDED} <= ?"
        val selectionArgs = arrayOf(cutoffSeconds.toString())
        val sortOrder = "${MediaStore.Images.Media.DATE_ADDED} ASC"

        return context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val addedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val modifiedColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val displayName = cursor.getString(nameColumn).orEmpty()
                    val relativePath = cursor.getString(pathColumn)

                    if (ScreenshotNameMatcher.looksLikeScreenshot(displayName, relativePath)) {
                        add(
                            ScreenshotItem(
                                id = id,
                                uri = ContentUris.withAppendedId(collection, id),
                                displayName = displayName,
                                dateAddedSeconds = cursor.getLong(addedColumn),
                                dateModifiedSeconds = cursor.getLong(modifiedColumn)
                            )
                        )
                    }
                }
            }
        }.orEmpty()
    }
}
