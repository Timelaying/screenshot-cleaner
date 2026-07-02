package com.example.screenshotcleaner

import android.app.Application
import androidx.room.Room
import com.example.screenshotcleaner.data.local.ScreenshotCleanerDatabase
import com.example.screenshotcleaner.data.media.ScreenshotScanner
import com.example.screenshotcleaner.data.repository.ScreenshotRepository
import com.example.screenshotcleaner.notification.ScreenshotNotificationManager

class ScreenshotCleanerApplication : Application() {
    val database: ScreenshotCleanerDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            ScreenshotCleanerDatabase::class.java,
            "screenshot-cleaner.db"
        ).build()
    }

    val repository: ScreenshotRepository by lazy {
        ScreenshotRepository(
            scanner = ScreenshotScanner(applicationContext),
            decisionDao = database.screenshotDecisionDao()
        )
    }

    val notificationManager: ScreenshotNotificationManager by lazy {
        ScreenshotNotificationManager(applicationContext)
    }
}

