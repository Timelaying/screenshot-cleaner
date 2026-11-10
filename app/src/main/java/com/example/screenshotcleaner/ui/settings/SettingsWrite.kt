package com.example.screenshotcleaner.ui.settings

import java.io.IOException

internal suspend fun saveSettingsChange(update: suspend () -> Unit): String? {
    return try {
        update()
        null
    } catch (_: IOException) {
        "Could not save settings. Try again."
    }
}
