package com.example.screenshotcleaner.domain

import android.net.Uri

data class ScreenshotItem(
    val id: Long,
    val uri: Uri,
    val displayName: String,
    val dateAddedSeconds: Long,
    val dateModifiedSeconds: Long
)

