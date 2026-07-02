package com.example.screenshotcleaner

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
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
import androidx.compose.runtime.getValue
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
import com.example.screenshotcleaner.domain.ScreenshotItem
import com.example.screenshotcleaner.ui.onboarding.OnboardingScreen
import com.example.screenshotcleaner.ui.review.ReviewScreen
import com.example.screenshotcleaner.worker.ScreenshotScanWorker
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val app: ScreenshotCleanerApplication
        get() = application as ScreenshotCleanerApplication

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        app.notificationManager.createChannel()
        scheduleScreenshotScan()

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScreenshotCleanerApp(
                        activity = this,
                        repository = app.repository
                    )
                }
            }
        }
    }

    private fun scheduleScreenshotScan() {
        val request = PeriodicWorkRequestBuilder<ScreenshotScanWorker>(1, TimeUnit.DAYS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            ScreenshotScanWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}

@Composable
private fun ScreenshotCleanerApp(
    activity: ComponentActivity,
    repository: ScreenshotRepository
) {
    val coroutineScope = rememberCoroutineScope()
    var hasImagePermission by remember { mutableStateOf(activity.hasImagePermission()) }
    var hasNotificationPermission by remember { mutableStateOf(activity.hasNotificationPermission()) }
    var screenshots by remember { mutableStateOf<List<ScreenshotItem>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<ScreenshotItem?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun refreshScreenshots() {
        coroutineScope.launch {
            screenshots = repository.getPendingOldScreenshots()
        }
    }

    val imagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasImagePermission = granted
        if (granted) refreshScreenshots()
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
            coroutineScope.launch {
                repository.markDeleted(item)
                screenshots = screenshots.drop(1)
            }
        }
    }

    LaunchedEffect(hasImagePermission) {
        if (hasImagePermission) refreshScreenshots()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!hasImagePermission || !hasNotificationPermission) {
            OnboardingScreen(
                hasImagePermission = hasImagePermission,
                hasNotificationPermission = hasNotificationPermission,
                onGrantImagePermission = {
                    imagePermissionLauncher.launch(activity.imagePermission())
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
            ReviewScreen(
                screenshots = screenshots,
                errorMessage = errorMessage,
                onKeep = { item ->
                    coroutineScope.launch {
                        repository.keep(item)
                        screenshots = screenshots.drop(1)
                    }
                },
                onDelete = { item ->
                    pendingDelete = item
                    try {
                        activity.requestDelete(item.uri, deleteLauncher::launch)
                    } catch (exception: RuntimeException) {
                        errorMessage = exception.message ?: "Delete request failed."
                    }
                },
                onRefresh = { refreshScreenshots() }
            )
        }
    }
}

private fun ComponentActivity.hasImagePermission(): Boolean {
    return ContextCompat.checkSelfPermission(this, imagePermission()) == PackageManager.PERMISSION_GRANTED
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

private fun ComponentActivity.requestDelete(
    uri: Uri,
    launch: (IntentSenderRequest) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val pendingIntent: PendingIntent = MediaStore.createDeleteRequest(contentResolver, listOf(uri))
        launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
    } else {
        val deletedRows = contentResolver.delete(uri, null, null)
        if (deletedRows == 0) {
            throw RuntimeException("Android did not delete this screenshot.")
        }
    }
}
