package com.example.screenshotcleaner.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun remindersAreEnabledByDefault() = runTest {
        val repository = SettingsRepository(
            testDataStore(
                fileName = "default.preferences_pb",
                scope = backgroundScope
            )
        )

        assertTrue(repository.settings.first().remindersEnabled)
        assertTrue(repository.remindersEnabled())
    }

    @Test
    fun updatesReminderPreference() = runTest {
        val repository = SettingsRepository(
            testDataStore(
                fileName = "updated.preferences_pb",
                scope = backgroundScope
            )
        )

        repository.setRemindersEnabled(false)

        assertFalse(repository.settings.first().remindersEnabled)
        assertFalse(repository.remindersEnabled())
    }

    private fun testDataStore(
        fileName: String,
        scope: CoroutineScope
    ): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            scope = scope,
            produceFile = { File(temporaryFolder.root, fileName) }
        )
    }
}
