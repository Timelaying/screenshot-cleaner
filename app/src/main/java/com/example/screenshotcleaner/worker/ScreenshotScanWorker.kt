package com.example.screenshotcleaner.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.screenshotcleaner.ScreenshotCleanerApplication

class ScreenshotScanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        if (!applicationContext.hasImagePermission() || !applicationContext.hasNotificationPermission()) {
            return Result.success()
        }

        val app = applicationContext as ScreenshotCleanerApplication
        val screenshots = app.repository.getPendingOldScreenshots()
        if (screenshots.isNotEmpty()) {
            app.notificationManager.createChannel()
            app.notificationManager.showOldScreenshotsFound(screenshots.size)
        }

        return Result.success()
    }

    companion object {
        const val WORK_NAME = "old-screenshot-scan"
    }
}

private fun Context.hasImagePermission(): Boolean {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
}

private fun Context.hasNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

