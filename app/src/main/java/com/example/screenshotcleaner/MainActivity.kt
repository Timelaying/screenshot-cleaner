package com.example.screenshotcleaner

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.screenshotcleaner.data.repository.ScreenshotRepository
import com.example.screenshotcleaner.data.settings.AppSettings
import com.example.screenshotcleaner.data.settings.SettingsRepository
import com.example.screenshotcleaner.domain.ScreenshotItem
import com.example.screenshotcleaner.notification.ScreenshotNotificationManager
import com.example.screenshotcleaner.ui.onboarding.OnboardingScreen
import com.example.screenshotcleaner.ui.review.ReviewScreen
import com.example.screenshotcleaner.ui.settings.SettingsScreen
import com.example.screenshotcleaner.worker.ScreenshotScanWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var reviewIntentVersion by mutableIntStateOf(0)
    private var permissionStateVersion by mutableIntStateOf(0)

    private val app: ScreenshotCleanerApplication
        get() = application as ScreenshotCleanerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.isReviewScreenIntent()) {
            reviewIntentVersion = 1
        }
        app.notificationManager.createChannel()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenshotCleanerApp(
                        activity = this,
                        repository = app.repository,
                        settingsRepository = app.settingsRepository,
                        reviewIntentVersion = reviewIntentVersion,
                        permissionStateVersion = permissionStateVersion,
                        onReminderSchedulingChanged = ::setReminderScheduling
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.isReviewScreenIntent()) {
            reviewIntentVersion++
        }
    }

    override fun onResume() {
        super.onResume()
        permissionStateVersion++
    }

    private fun setReminderScheduling(enabled: Boolean) {
        val workManager = WorkManager.getInstance(this)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<ScreenshotScanWorker>(1, TimeUnit.DAYS)
                .build()

            workManager.enqueueUniquePeriodicWork(
                ScreenshotScanWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(ScreenshotScanWorker.WORK_NAME)
        }
    }
}

@Composable
private fun ScreenshotCleanerApp(
    activity: ComponentActivity,
    repository: ScreenshotRepository,
    settingsRepository: SettingsRepository,
    reviewIntentVersion: Int,
    permissionStateVersion: Int,
    onReminderSchedulingChanged: (Boolean) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var imageAccess by remember { mutableStateOf(activity.imageAccessState()) }
    var hasNotificationPermission by remember { mutableStateOf(activity.hasNotificationPermission()) }
    var screenshots by remember { mutableStateOf<List<ScreenshotItem>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<ScreenshotItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var destination by remember { mutableStateOf(AppDestination.REVIEW) }
    val settings by settingsRepository.settings.collectAsState(initial = AppSettings())

    fun refreshScreenshots() {
        coroutineScope.launch {
            errorMessage = null
            screenshots = repository.getPendingOldScreenshots(settings.screenshotAgeDays)
        }
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        imageAccess = activity.imageAccessState()
        if (imageAccess == ImageAccessState.FULL) refreshScreenshots()
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val item = pendingDelete
        pendingDelete = null
        if (result.resultCode == Activity.RESULT_OK && item != null) {
            coroutineScope.markDeleted(
                item,
                repository,
                settings.screenshotAgeDays
            ) { updatedScreenshots ->
                errorMessage = null
                screenshots = updatedScreenshots
            }
        }
    }

    LaunchedEffect(imageAccess, settings.screenshotAgeDays) {
        if (imageAccess == ImageAccessState.FULL) refreshScreenshots()
    }

    LaunchedEffect(reviewIntentVersion) {
        if (reviewIntentVersion > 0) {
            destination = AppDestination.REVIEW
        }
    }

    LaunchedEffect(permissionStateVersion) {
        imageAccess = activity.imageAccessState()
        hasNotificationPermission = activity.hasNotificationPermission()
    }

    LaunchedEffect(settings.remindersEnabled) {
        onReminderSchedulingChanged(settings.remindersEnabled)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (shouldShowOnboarding(imageAccess, hasNotificationPermission)) {
            OnboardingScreen(
                hasImagePermission = imageAccess == ImageAccessState.FULL,
                imagePermissionStatus = imageAccess.statusLabel,
                hasNotificationPermission = hasNotificationPermission,
                screenshotAgeDays = settings.screenshotAgeDays,
                onGrantImagePermission = {
                    imagePermissionLauncher.launch(activity.imagePermissions())
                },
                onGrantNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        hasNotificationPermission = true
                    }
                }
            )
        } else {
            when (destination) {
                AppDestination.REVIEW -> ReviewScreen(
                    screenshots = screenshots,
                    errorMessage = errorMessage,
                    onKeep = { item ->
                        coroutineScope.launch {
                            errorMessage = null
                            repository.keep(item)
                            screenshots = screenshots.drop(1)
                        }
                    },
                    onDelete = { item ->
                        pendingDelete = item
                        try {
                            val deletedImmediately = activity.requestDelete(item.uri, deleteLauncher::launch)
                            if (deletedImmediately) {
                                pendingDelete = null
                                coroutineScope.markDeleted(
                                    item,
                                    repository,
                                    settings.screenshotAgeDays
                                ) { updatedScreenshots ->
                                    errorMessage = null
                                    screenshots = updatedScreenshots
                                }
                            }
                        } catch (exception: RuntimeException) {
                            pendingDelete = null
                            errorMessage = exception.message ?: "Delete request failed."
                        }
                    },
                    onRefresh = { refreshScreenshots() },
                    onOpenSettings = { destination = AppDestination.SETTINGS }
                )

                AppDestination.SETTINGS -> SettingsScreen(
                    settings = settings,
                    onRemindersEnabledChange = { enabled ->
                        coroutineScope.launch {
                            settingsRepository.setRemindersEnabled(enabled)
                        }
                    },
                    onScreenshotAgeDaysChange = { ageDays ->
                        coroutineScope.launch {
                            settingsRepository.setScreenshotAgeDays(ageDays)
                        }
                    },
                    onBack = { destination = AppDestination.REVIEW }
                )
            }
        }
    }
}

internal fun Intent.isReviewScreenIntent(): Boolean =
    action == ScreenshotNotificationManager.ACTION_REVIEW_SCREEN

private fun kotlinx.coroutines.CoroutineScope.markDeleted(
    item: ScreenshotItem,
    repository: ScreenshotRepository,
    ageDays: Long,
    updateScreenshots: (List<ScreenshotItem>) -> Unit
) {
    launch {
        repository.markDeleted(item)
        updateScreenshots(repository.getPendingOldScreenshots(ageDays))
    }
}

private enum class AppDestination {
    REVIEW,
    SETTINGS
}

internal enum class ImageAccessState(val statusLabel: String) {
    FULL("Ready"),
    PARTIAL("Full access required"),
    MISSING("Required")
}

internal fun shouldShowOnboarding(
    imageAccess: ImageAccessState,
    hasNotificationPermission: Boolean
): Boolean = imageAccess != ImageAccessState.FULL || !hasNotificationPermission

private fun ComponentActivity.imageAccessState(): ImageAccessState {
    val hasReadAccess = ContextCompat.checkSelfPermission(
        this,
        imagePermission()
    ) == PackageManager.PERMISSION_GRANTED
    val hasWriteAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    if (hasRequiredMediaAccess(hasReadAccess, hasWriteAccess, Build.VERSION.SDK_INT)) {
        return ImageAccessState.FULL
    }

    val hasPartialAccess = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        ) == PackageManager.PERMISSION_GRANTED

    return if (hasPartialAccess) ImageAccessState.PARTIAL else ImageAccessState.MISSING
}

private fun ComponentActivity.hasNotificationPermission(): Boolean {
    return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
}

private fun ComponentActivity.imagePermission(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
}

private fun ComponentActivity.imagePermissions(): Array<String> {
    return buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            add(Manifest.permission.READ_MEDIA_IMAGES)
            add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
        } else {
            add(imagePermission())
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }.toTypedArray()
}

internal fun hasRequiredMediaAccess(
    hasReadAccess: Boolean,
    hasWriteAccess: Boolean,
    sdkInt: Int
): Boolean = hasReadAccess && (sdkInt >= Build.VERSION_CODES.R || hasWriteAccess)

private fun ComponentActivity.requestDelete(
    uri: Uri,
    launch: (IntentSenderRequest) -> Unit
): Boolean {
    return when (deleteModeForSdk(Build.VERSION.SDK_INT)) {
        DeleteMode.USER_CONFIRMATION -> {
            val pendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
            launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
            false
        }

        DeleteMode.DIRECT -> {
            val deletedRows = contentResolver.delete(uri, null, null)
            if (deletedRows == 0) {
                throw RuntimeException("Android did not delete this screenshot.")
            }
            true
        }
    }
}

internal enum class DeleteMode {
    USER_CONFIRMATION,
    DIRECT
}

internal fun deleteModeForSdk(sdkInt: Int): DeleteMode =
    if (sdkInt >= Build.VERSION_CODES.R) {
        DeleteMode.USER_CONFIRMATION
    } else {
        DeleteMode.DIRECT
    }
