package com.example.screenshotcleaner.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

internal class ScreenshotDeletion(
    private val resolver: ContentResolver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    // True means the file was deleted; false means Android is awaiting user consent.
    suspend fun request(uri: Uri, launch: (IntentSenderRequest) -> Unit): Boolean {
        val consentRequest = withContext(ioDispatcher) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
                IntentSenderRequest.Builder(pendingIntent.intentSender).build()
            } else {
                try {
                    deleteDirectly(uri)
                    null
                } catch (exception: SecurityException) {
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                        exception is RecoverableSecurityException
                    ) {
                        IntentSenderRequest.Builder(exception.userAction.actionIntent.intentSender).build()
                    } else {
                        throw exception
                    }
                }
            }
        }
        // Resume on the caller's UI dispatcher before launching Android's consent dialog.
        currentCoroutineContext().ensureActive()
        if (consentRequest != null) launch(consentRequest)
        return consentRequest == null
    }

    // Android 10 grants access only. Android 11+ performs the deletion itself.
    // Call only for RESULT_OK, and finish this before recording a deleted decision.
    suspend fun finishConfirmation(uri: Uri) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Do not reopen consent if the retry fails: surface the error instead.
            withContext(ioDispatcher) { deleteDirectly(uri) }
        }
    }

    private fun deleteDirectly(uri: Uri) {
        check(resolver.delete(uri, null, null) > 0) {
            "Android did not delete this screenshot."
        }
    }
}
