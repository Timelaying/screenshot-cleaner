package com.example.screenshotcleaner.data.media

import android.app.RecoverableSecurityException
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.result.IntentSenderRequest

internal class ScreenshotDeletion(private val resolver: ContentResolver) {
    // True means the file was deleted; false means Android is awaiting user consent.
    fun request(uri: Uri, launch: (IntentSenderRequest) -> Unit): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val pendingIntent = MediaStore.createDeleteRequest(resolver, listOf(uri))
            launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            return false
        }

        try {
            deleteDirectly(uri)
            return true
        } catch (exception: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q &&
                exception is RecoverableSecurityException
            ) {
                launch(IntentSenderRequest.Builder(exception.userAction.actionIntent.intentSender).build())
                return false
            }
            throw exception
        }
    }

    // Android 10 grants access only. Android 11+ performs the deletion itself.
    // Call only for RESULT_OK, and finish this before recording a deleted decision.
    fun finishConfirmation(uri: Uri) {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            // Do not reopen consent if the retry fails: surface the error instead.
            deleteDirectly(uri)
        }
    }

    private fun deleteDirectly(uri: Uri) {
        check(resolver.delete(uri, null, null) > 0) {
            "Android did not delete this screenshot."
        }
    }
}
