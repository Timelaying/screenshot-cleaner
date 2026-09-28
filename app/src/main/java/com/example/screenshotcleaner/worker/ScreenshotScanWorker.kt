package com.example.screenshotcleaner.worker

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ListenableWorker.Result
import androidx.work.WorkerParameters
import com.example.screenshotcleaner.ScreenshotCleanerApplication
import com.example.screenshotcleaner.hasRequiredMediaAccess
import kotlinx.coroutines.CancellationException

class ScreenshotScanWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as ScreenshotCleanerApplication
        if (!shouldRunScreenshotScan(
                hasFullImageAccess = applicationContext.hasFullImageAccess(),
                hasNotificationPermission = applicationContext.hasNotificationPermission(),
                remindersEnabled = app.settingsRepository.remindersEnabled()
            )
        ) {
            return Result.success()
        }

        return try {
            val ageDays = app.settingsRepository.screenshotAgeDays()
            val screenshots = app.repository.getPendingOldScreenshots(ageDays)
            if (shouldNotifyForOldScreenshots(screenshots.size)) {
                app.notificationManager.createChannel()
                app.notificationManager.showOldScreenshotsFound(screenshots.size, ageDays)
            }

            Result.success()
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: SecurityException) {
            Log.w(TAG, "Screenshot access changed while scanning; skipping this run.", exception)
            scanFailureAction(exception).result()
        } catch (exception: Exception) {
            Log.w(TAG, "Screenshot scan failed; retrying.", exception)
            scanFailureAction(exception).result()
        }
    }

    companion object {
        const val WORK_NAME = "old-screenshot-scan"
        private const val TAG = "ScreenshotScanWorker"
    }
}

internal fun shouldRunScreenshotScan(
    hasFullImageAccess: Boolean,
    hasNotificationPermission: Boolean,
    remindersEnabled: Boolean
): Boolean = hasFullImageAccess && hasNotificationPermission && remindersEnabled

internal fun shouldNotifyForOldScreenshots(count: Int): Boolean = count > 0

internal enum class ScanFailureAction {
    SKIP,
    RETRY
}

internal fun scanFailureAction(exception: Throwable): ScanFailureAction =
    if (exception is SecurityException) ScanFailureAction.SKIP else ScanFailureAction.RETRY

private fun ScanFailureAction.result(): Result = when (this) {
    ScanFailureAction.SKIP -> Result.success()
    ScanFailureAction.RETRY -> Result.retry()
}

private fun Context.hasFullImageAccess(): Boolean {
    val readPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val hasReadAccess = ContextCompat.checkSelfPermission(this, readPermission) == PackageManager.PERMISSION_GRANTED
    val hasWriteAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED

    return hasRequiredMediaAccess(hasReadAccess, hasWriteAccess, Build.VERSION.SDK_INT)
}

private fun Context.hasNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}
