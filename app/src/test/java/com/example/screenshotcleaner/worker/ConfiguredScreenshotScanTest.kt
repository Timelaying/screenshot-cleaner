package com.example.screenshotcleaner.worker

import androidx.work.ListenableWorker.Result
import com.example.screenshotcleaner.data.settings.AppSettings
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ConfiguredScreenshotScanTest {
    @Test
    fun settingsReadFailureRetriesWithoutScanning() = runTest {
        val settings = flow<AppSettings> { throw IOException("Settings unavailable") }

        val result = runConfiguredScreenshotScan(settings, true, true) {
            fail("Must not scan using default settings after a read failure")
        }

        assertEquals(Result.retry(), result)
    }

    @Test
    fun settingsReadCancellationPropagates() = runTest {
        val cancellation = CancellationException("Worker stopped")
        val settings = flow<AppSettings> { throw cancellation }

        try {
            runConfiguredScreenshotScan(settings, true, true) {
                fail("Cancelled work must not scan")
            }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }

    @Test
    fun pausedRemindersSkipScanning() = runTest {
        val settings = flowOf(AppSettings(remindersEnabled = false))

        val result = runConfiguredScreenshotScan(settings, true, true) {
            fail("Paused reminders must not scan")
        }

        assertEquals(Result.success(), result)
    }

    @Test
    fun missingPermissionsSkipScanning() = runTest {
        for ((images, notifications) in listOf(false to true, true to false)) {
            val result = runConfiguredScreenshotScan(flowOf(AppSettings()), images, notifications) {
                fail("Missing permissions must prevent scanning")
            }
            assertEquals(Result.success(), result)
        }
    }

    @Test
    fun scanUsesOneSettingsSnapshotForItsThreshold() = runTest {
        var reads = 0
        val settings = flow {
            reads++
            emit(AppSettings(remindersEnabled = true, screenshotAgeDays = 90L))
        }
        val scannedAges = mutableListOf<Long>()

        val result = runConfiguredScreenshotScan(settings, true, true) { scannedAges += it }

        assertEquals(Result.success(), result)
        assertEquals(1, reads)
        assertEquals(listOf(90L), scannedAges)
    }

    @Test
    fun scanFailuresRetainRetryAndPermissionPolicies() = runTest {
        for ((exception, expected) in listOf(
            IOException("MediaStore unavailable") to Result.retry(),
            SecurityException("Access revoked") to Result.success()
        )) {
            val result = runConfiguredScreenshotScan(flowOf(AppSettings()), true, true) {
                throw exception
            }
            assertEquals(expected, result)
        }
    }

    @Test
    fun scanCancellationPropagates() = runTest {
        val cancellation = CancellationException("Worker stopped during scan")

        try {
            runConfiguredScreenshotScan(flowOf(AppSettings()), true, true) { throw cancellation }
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertSame(cancellation, actual)
        }
    }
}
