package com.example.screenshotcleaner.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.example.screenshotcleaner.data.settings.AppSettings
import com.example.screenshotcleaner.data.settings.SettingsRepository
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class SettingsWriteTest {
    @Test
    fun failedReminderWriteKeepsSavedValueAndCanBeRetried() = runTest {
        val store = FailingWriteDataStore()
        val repository = SettingsRepository(store)

        var error = saveSettingsChange { repository.setRemindersEnabled(false) }

        assertEquals("Could not save settings. Try again.", error)
        assertEquals(AppSettings(), repository.settings.first())

        store.failure = null
        error = saveSettingsChange { repository.setRemindersEnabled(false) }

        assertNull(error)
        assertEquals(AppSettings(remindersEnabled = false), repository.settings.first())
    }

    @Test
    fun failedAgeWriteKeepsNondefaultThresholdAndCanBeRetried() = runTest {
        val store = FailingWriteDataStore()
        val repository = SettingsRepository(store)
        store.failure = null
        repository.setScreenshotAgeDays(60L)
        store.failure = IOException("Disk full")

        var error = saveSettingsChange { repository.setScreenshotAgeDays(90L) }

        assertEquals("Could not save settings. Try again.", error)
        assertEquals(60L, repository.screenshotAgeDays())

        store.failure = null
        error = saveSettingsChange { repository.setScreenshotAgeDays(90L) }

        assertNull(error)
        assertEquals(90L, repository.screenshotAgeDays())
    }

    @Test
    fun cancelledWritePropagatesAndLeavesSettingsUnchanged() = runTest {
        val cancellation = CancellationException("Screen left")
        val store = FailingWriteDataStore().apply { failure = cancellation }
        val repository = SettingsRepository(store)

        try {
            saveSettingsChange { repository.setRemindersEnabled(false) }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }

        assertEquals(AppSettings(), repository.settings.first())
    }

    private class FailingWriteDataStore : DataStore<Preferences> {
        override val data = MutableStateFlow(emptyPreferences())
        var failure: Exception? = IOException("Disk full")

        override suspend fun updateData(
            transform: suspend (Preferences) -> Preferences
        ): Preferences {
            val updated = transform(data.value)
            failure?.let { throw it }
            data.value = updated
            return updated
        }
    }
}
