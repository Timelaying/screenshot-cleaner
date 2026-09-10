package com.example.screenshotcleaner.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.screenshotcleaner.MainActivity
import com.example.screenshotcleaner.R

class ScreenshotNotificationManager(
    private val context: Context
) {
    fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screenshot reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showOldScreenshotsFound(count: Int, ageDays: Long) {
        val intent = reviewIntent(context)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = screenshotNotificationContent(count, ageDays)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(OLD_SCREENSHOTS_NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_REVIEW_SCREEN = "com.example.screenshotcleaner.action.REVIEW_SCREEN"
        const val CHANNEL_ID = "screenshot_reminders"
        private const val OLD_SCREENSHOTS_NOTIFICATION_ID = 1001
    }
}

internal data class ScreenshotNotificationContent(
    val title: String,
    val text: String
)

internal fun screenshotNotificationContent(
    count: Int,
    ageDays: Long
): ScreenshotNotificationContent = ScreenshotNotificationContent(
    title = "$count old screenshots found",
    text = "Review screenshots older than $ageDays days."
)

internal fun reviewIntent(context: Context): Intent {
    return Intent(context, MainActivity::class.java)
        .setAction(ScreenshotNotificationManager.ACTION_REVIEW_SCREEN)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
}
