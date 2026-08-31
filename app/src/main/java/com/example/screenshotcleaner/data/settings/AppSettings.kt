package com.example.screenshotcleaner.data.settings

data class AppSettings(
    val remindersEnabled: Boolean = true,
    val screenshotAgeDays: Long = DEFAULT_SCREENSHOT_AGE_DAYS
)

const val DEFAULT_SCREENSHOT_AGE_DAYS = 30L

val SUPPORTED_SCREENSHOT_AGE_DAYS = listOf(7L, DEFAULT_SCREENSHOT_AGE_DAYS, 60L, 90L)
