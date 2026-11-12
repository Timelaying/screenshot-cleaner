package com.example.screenshotcleaner.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.preferencesOf
import com.example.screenshotcleaner.data.settings.AppSettings
import com.example.screenshotcleaner.data.settings.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SettingsLoadTest {
    private val savedSettings = AppSettings(remindersEnabled = false, screenshotAgeDays = 90L)

    @Test
    fun delayedReadDoesNotExposeDefaultSettings() = runTest {
        val read = CompletableDeferred<AppSettings>()
        val states = mutableListOf<SettingsLoadState>()
        val job = launch {
            flow { emit(read.await()) }.settingsLoadStates().toList(states)
        }
        yield()
        assertEquals(listOf(SettingsLoadState.Loading), states)

        read.complete(savedSettings)
        job.join()
        assertEquals(listOf(SettingsLoadState.Loading, SettingsLoadState.Ready(savedSettings)), states)
    }

    @Test
    fun failedRepositoryReadCanBeRetriedWithoutChangingSavedSettings() = runTest {
        var readAttempts = 0
        val preferences = preferencesOf(
            booleanPreferencesKey("reminders_enabled") to false,
            longPreferencesKey("screenshot_age_days") to 90L
        )
        val store = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow {
                readAttempts++
                if (readAttempts == 1) throw IOException("Storage unavailable")
                emit(preferences)
            }

            override suspend fun updateData(
                transform: suspend (Preferences) -> Preferences
            ): Preferences = error("Loading must not write settings")
        }
        val states = SettingsRepository(store).settings.settingsLoadStates()

        assertEquals(listOf(SettingsLoadState.Loading, SettingsLoadState.Failed), states.toList())
        assertEquals(
            listOf(SettingsLoadState.Loading, SettingsLoadState.Ready(savedSettings)),
            states.toList()
        )
        assertEquals(2, readAttempts)
    }

    @Test
    fun readFailureAfterSuccessReplacesReadyState() = runTest {
        val states = flow {
            emit(savedSettings)
            throw IOException("Storage disconnected")
        }.settingsLoadStates().toList()

        assertEquals(
            listOf(SettingsLoadState.Loading, SettingsLoadState.Ready(savedSettings), SettingsLoadState.Failed),
            states
        )
    }

    @Test
    fun successfulCollectionContinuesToObserveSettingsChanges() = runTest {
        val changedSettings = savedSettings.copy(screenshotAgeDays = 60L)

        assertEquals(
            listOf(
                SettingsLoadState.Loading,
                SettingsLoadState.Ready(savedSettings),
                SettingsLoadState.Ready(changedSettings)
            ),
            flowOf(savedSettings, changedSettings).settingsLoadStates().toList()
        )
    }

    @Test
    fun cancellationIsNotConvertedToReadFailure() = runTest {
        assertPropagates(CancellationException("Screen left"))
    }

    @Test
    fun programmingErrorsAreNotConvertedToReadFailure() = runTest {
        assertPropagates(IllegalStateException("Invalid store configuration"))
    }

    private suspend fun assertPropagates(failure: Exception) {
        val states = mutableListOf<SettingsLoadState>()
        try {
            flow<AppSettings> { throw failure }.settingsLoadStates().toList(states)
            fail("Expected failure to propagate")
        } catch (actual: Exception) {
            assertSame(failure, actual)
        }
        assertEquals(listOf(SettingsLoadState.Loading), states)
    }
}
