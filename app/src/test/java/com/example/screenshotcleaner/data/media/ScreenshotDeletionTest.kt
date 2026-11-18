package com.example.screenshotcleaner.data.media

import android.app.Application
import android.app.PendingIntent
import android.app.RecoverableSecurityException
import android.app.RemoteAction
import android.content.ContentProvider
import android.content.ContentValues
import android.content.Intent
import android.content.pm.ProviderInfo
import android.database.Cursor
import android.graphics.drawable.Icon
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29], application = Application::class, manifest = Config.NONE)
class ScreenshotDeletionTest {
    private lateinit var provider: DeleteProvider
    private lateinit var deletion: ScreenshotDeletion
    private val uri = Uri.parse("content://media/external/images/media/42")
    private val requests = mutableListOf<IntentSenderRequest>()

    @Before
    fun setUp() {
        val context = RuntimeEnvironment.getApplication()
        provider = DeleteProvider()
        provider.attachInfo(context, ProviderInfo().apply { authority = MediaStore.AUTHORITY })
        ShadowContentResolver.registerProviderInternal(MediaStore.AUTHORITY, provider)
        deletion = ScreenshotDeletion(context.contentResolver)
    }

    @Test
    fun androidTenConsentGrantsAccessThenRetriesTheOriginalUri() = runBlocking<Unit> {
        val exception = consentRequired()
        provider.failure = exception

        assertFalse(deletion.request(uri, requests::add))
        assertEquals(exception.userAction.actionIntent.intentSender, requests.single().intentSender)
        assertEquals(0, provider.deletedCount)

        // RESULT_OK grants access but has not removed the file on Android 10.
        provider.failure = null
        deletion.finishConfirmation(uri)

        assertEquals(listOf(uri, uri), provider.attemptedUris)
        assertEquals(1, provider.deletedCount)
        assertEquals(1, requests.size)
    }

    @Test
    fun failedRetryCannotBeReportedAsACompletedDeletionOrReopenConsent() = runBlocking<Unit> {
        val exception = consentRequired()
        provider.failure = exception
        assertFalse(deletion.request(uri, requests::add))

        assertSame(exception, assertThrows(SecurityException::class.java) {
            runBlocking { deletion.finishConfirmation(uri) }
        })
        assertEquals(0, provider.deletedCount)
        assertEquals(1, requests.size)
    }

    @Test
    fun retryThatDeletesNoRowsFails() = runBlocking<Unit> {
        provider.failure = consentRequired()
        assertFalse(deletion.request(uri, requests::add))
        provider.failure = null
        provider.rowsDeleted = 0

        assertThrows(IllegalStateException::class.java) { runBlocking { deletion.finishConfirmation(uri) } }
        assertEquals(0, provider.deletedCount)
    }

    @Test
    @Config(sdk = [26, 28, 29])
    fun writableFileIsDeletedImmediatelyWithoutConsent() = runBlocking<Unit> {
        assertTrue(deletion.request(uri, requests::add))
        assertEquals(listOf(uri), provider.attemptedUris)
        assertEquals(1, provider.deletedCount)
        assertTrue(requests.isEmpty())
    }

    @Test
    @Config(sdk = [26, 28, 29])
    fun ordinarySecurityFailurePropagatesWithoutLaunchingConsent() = runBlocking<Unit> {
        val exception = SecurityException("Read permission revoked")
        provider.failure = exception

        assertSame(exception, assertThrows(SecurityException::class.java) {
            runBlocking { deletion.request(uri, requests::add) }
        })
        assertTrue(requests.isEmpty())
        assertEquals(0, provider.deletedCount)
    }

    @Test
    fun consentLauncherFailurePropagatesWithoutDeleting() = runBlocking<Unit> {
        provider.failure = consentRequired()
        val exception = IllegalStateException("Cannot launch consent")

        assertSame(exception, assertThrows(IllegalStateException::class.java) {
            runBlocking { deletion.request(uri) { throw exception } }
        })
        assertEquals(0, provider.deletedCount)
    }

    @Test
    @Config(sdk = [30, 35])
    fun androidElevenConfirmationDoesNotDeleteAgain() = runBlocking<Unit> {
        provider.failure = SecurityException("Must not issue another delete")

        deletion.finishConfirmation(uri)

        assertTrue(provider.attemptedUris.isEmpty())
    }

    @Test
    fun deleteAndConfirmationRetryRunOffCallerWhileConsentReturnsToCaller() = runBlocking<Unit> {
        val callerThread = Thread.currentThread()
        provider.failure = consentRequired()
        var consentThread: Thread? = null

        assertFalse(deletion.request(uri) { consentThread = Thread.currentThread() })
        assertSame(callerThread, consentThread)
        provider.failure = null
        deletion.finishConfirmation(uri)

        assertEquals(2, provider.deleteThreads.size)
        provider.deleteThreads.forEach { assertNotSame(callerThread, it) }
        assertEquals(1, provider.deletedCount)
    }

    @Test
    @Config(sdk = [26, 28, 29])
    fun directDeletionRunsOffCallerThread() = runBlocking<Unit> {
        val callerThread = Thread.currentThread()
        assertTrue(deletion.request(uri, requests::add))
        assertNotSame(callerThread, provider.deleteThreads.single())
    }

    @Test
    fun cancellationDuringProviderCallDoesNotLaunchLateConsent() = runBlocking<Unit> {
        provider.failure = consentRequired()
        val requestJob = launch {
            val callerJob = coroutineContext.job
            provider.beforeDelete = { callerJob.cancel() }
            deletion.request(uri, requests::add)
        }
        requestJob.join()

        assertTrue(requestJob.isCancelled)
        assertEquals(listOf(uri), provider.attemptedUris)
        assertTrue(requests.isEmpty())
        assertEquals(0, provider.deletedCount)
    }

    private fun consentRequired(): RecoverableSecurityException {
        val context = RuntimeEnvironment.getApplication()
        val action = PendingIntent.getActivity(
            context, 42, Intent("test.DELETE_CONSENT"), PendingIntent.FLAG_IMMUTABLE
        )
        return RecoverableSecurityException(
            SecurityException("Consent required"),
            "Allow deletion",
            RemoteAction(
                Icon.createWithResource(context, android.R.drawable.ic_menu_delete),
                "Allow", "Allow deletion", action
            )
        )
    }
}

private class DeleteProvider : ContentProvider() {
    var failure: SecurityException? = null
    var rowsDeleted = 1
    var deletedCount = 0
    val attemptedUris = mutableListOf<Uri>()
    val deleteThreads = mutableListOf<Thread>()
    var beforeDelete: () -> Unit = {}

    override fun onCreate(): Boolean = true
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        attemptedUris += uri
        deleteThreads += Thread.currentThread()
        beforeDelete()
        failure?.let { throw it }
        deletedCount += rowsDeleted
        return rowsDeleted
    }

    override fun query(
        uri: Uri, projection: Array<out String>?, selection: String?,
        selectionArgs: Array<out String>?, sortOrder: String?
    ): Cursor? = error("Unexpected query")
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = error("Unexpected insert")
    override fun update(
        uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?
    ): Int = error("Unexpected update")
}
