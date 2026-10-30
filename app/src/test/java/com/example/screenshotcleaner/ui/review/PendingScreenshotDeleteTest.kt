package com.example.screenshotcleaner.ui.review

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.example.screenshotcleaner.domain.ScreenshotItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PendingScreenshotDeleteTest {
    @Test
    fun confirmedResultAfterRecreationReturnsOriginalScreenshotOnlyOnce() {
        val original = StateOwner()
        val pending = PendingScreenshotDelete(original.savedStateRegistry)
        pending.item = screenshot

        val recreated = StateOwner(original.save())
        val restored = PendingScreenshotDelete(recreated.savedStateRegistry)

        assertEquals(screenshot, restored.complete(Activity.RESULT_OK))
        assertNull(restored.complete(Activity.RESULT_OK))
        val recreatedAgain = StateOwner(recreated.save())
        assertNull(PendingScreenshotDelete(recreatedAgain.savedStateRegistry).item)
    }

    @Test
    fun cancelledResultAfterRecreationDoesNotReturnADeletion() {
        val original = StateOwner()
        PendingScreenshotDelete(original.savedStateRegistry).item = screenshot
        val recreated = StateOwner(original.save())
        val restored = PendingScreenshotDelete(recreated.savedStateRegistry)

        assertNull(restored.complete(Activity.RESULT_CANCELED))
        assertNull(restored.item)
        val recreatedAgain = StateOwner(recreated.save())
        assertNull(PendingScreenshotDelete(recreatedAgain.savedStateRegistry).item)
    }

    @Test
    fun clearedRequestIsNotRestoredAfterLaunchFailureOrImmediateDeletion() {
        val original = StateOwner()
        val pending = PendingScreenshotDelete(original.savedStateRegistry)
        pending.item = screenshot
        pending.item = null

        val recreated = StateOwner(original.save())

        assertNull(PendingScreenshotDelete(recreated.savedStateRegistry).complete(Activity.RESULT_OK))
    }

    @Test
    fun resultWithoutPendingRequestCannotDeleteAnything() {
        val owner = StateOwner()

        assertNull(PendingScreenshotDelete(owner.savedStateRegistry).complete(Activity.RESULT_OK))
    }

    private val screenshot = ScreenshotItem(
        id = 4294967297L,
        uri = Uri.parse("content://media/external/images/media/4294967297"),
        displayName = "Screenshot original.png",
        dateAddedSeconds = 123456789L,
        dateModifiedSeconds = 234567890L
    )

    private class StateOwner(savedState: Bundle? = null) : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val controller = SavedStateRegistryController.create(this)
        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = controller.savedStateRegistry

        init {
            controller.performAttach()
            controller.performRestore(savedState)
            lifecycleRegistry.currentState = Lifecycle.State.CREATED
        }

        fun save(): Bundle {
            val bundle = Bundle()
            controller.performSave(bundle)
            // Cross a real Bundle serialization boundary, as with process recreation.
            val parcel = Parcel.obtain()
            return try {
                parcel.writeBundle(bundle)
                parcel.setDataPosition(0)
                requireNotNull(parcel.readBundle(javaClass.classLoader))
            } finally {
                parcel.recycle()
            }
        }
    }
}
