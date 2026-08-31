package com.example.screenshotcleaner.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.screenshotCleanerSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "screenshot-cleaner-settings"
)

class SettingsRepository(
    private val dataStore: DataStore<Preferences>
) {
    val settings: Flow<AppSettings> = dataStore.data.map { preferences ->
        AppSettings(
            remindersEnabled = preferences[REMINDERS_ENABLED] ?: true,
            screenshotAgeDays = preferences[SCREENSHOT_AGE_DAYS] ?: DEFAULT_SCREENSHOT_AGE_DAYS
        )
    }

    suspend fun setRemindersEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[REMINDERS_ENABLED] = enabled
        }
    }

    suspend fun remindersEnabled(): Boolean {
        return settings.first().remindersEnabled
    }

    suspend fun setScreenshotAgeDays(ageDays: Long) {
        require(ageDays in SUPPORTED_SCREENSHOT_AGE_DAYS) {
            "Unsupported screenshot age threshold: $ageDays"
        }
        dataStore.edit { preferences ->
            preferences[SCREENSHOT_AGE_DAYS] = ageDays
        }
    }

    suspend fun screenshotAgeDays(): Long {
        return settings.first().screenshotAgeDays
    }

    private companion object {
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val SCREENSHOT_AGE_DAYS = longPreferencesKey("screenshot_age_days")
    }
}
