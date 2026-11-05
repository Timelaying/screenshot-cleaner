package com.example.screenshotcleaner.ui.review

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.savedstate.SavedStateRegistry
import com.example.screenshotcleaner.domain.ScreenshotItem

// Register after Activity.onCreate restores state, before the result launcher can deliver a result.
internal class PendingScreenshotDelete(registry: SavedStateRegistry) {
    var item: ScreenshotItem? = registry.consumeRestoredStateForKey(STATE_KEY)?.let { state ->
        state.getString("uri")?.let { uri ->
            ScreenshotItem(
                id = state.getLong("id"),
                uri = Uri.parse(uri),
                displayName = state.getString("displayName").orEmpty(),
                dateAddedSeconds = state.getLong("dateAddedSeconds"),
                dateModifiedSeconds = state.getLong("dateModifiedSeconds")
            )
        }
    }

    init {
        registry.registerSavedStateProvider(STATE_KEY) {
            Bundle().apply {
                item?.let { pending ->
                    putLong("id", pending.id)
                    putString("uri", pending.uri.toString())
                    putString("displayName", pending.displayName)
                    putLong("dateAddedSeconds", pending.dateAddedSeconds)
                    putLong("dateModifiedSeconds", pending.dateModifiedSeconds)
                }
            }
        }
    }

    // Cancellation consumes the request too, but must never create a deleted decision.
    fun complete(resultCode: Int): ScreenshotItem? {
        val completed = item
        item = null
        return completed.takeIf { resultCode == Activity.RESULT_OK }
    }

    private companion object {
        const val STATE_KEY = "pendingScreenshotDelete"
    }
}
