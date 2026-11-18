package com.example.screenshotcleaner.ui.settings

import com.example.screenshotcleaner.data.settings.AppSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

internal sealed interface SettingsLoadState {
    data object Loading : SettingsLoadState
    data class Ready(val settings: AppSettings) : SettingsLoadState
    data object Failed : SettingsLoadState
}

// Recollecting after failure asks DataStore to read again; never substitute defaults.
internal fun Flow<AppSettings>.settingsLoadStates(): Flow<SettingsLoadState> =
    map<AppSettings, SettingsLoadState> { SettingsLoadState.Ready(it) }
        .onStart { emit(SettingsLoadState.Loading) }
        .catch { exception ->
            if (exception is IOException) emit(SettingsLoadState.Failed) else throw exception
        }
