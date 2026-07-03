package com.example.screenshotcleaner.ui.review

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.screenshotcleaner.domain.ScreenshotItem

@Composable
fun ReviewScreen(
    screenshots: List<ScreenshotItem>,
    errorMessage: String?,
    onKeep: (ScreenshotItem) -> Unit,
    onDelete: (ScreenshotItem) -> Unit,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val current = screenshots.firstOrNull()
    var deleteCandidate by remember { mutableStateOf<ScreenshotItem?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "Review", style = MaterialTheme.typography.headlineSmall)
                Text(
                    text = "${screenshots.size} old screenshots",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row {
                TextButton(onClick = onRefresh) {
                    Text("Refresh")
                }
                TextButton(onClick = onOpenSettings) {
                    Text("Settings")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "All done", style = MaterialTheme.typography.headlineMedium)
            }
        } else {
            ScreenshotCard(
                item = current,
                onKeep = { onKeep(current) },
                onDelete = { deleteCandidate = current }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FilledTonalButton(onClick = { deleteCandidate = current }) {
                    Text("Delete")
                }
                Button(onClick = { onKeep(current) }) {
                    Text("Keep")
                }
            }
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = errorMessage, color = MaterialTheme.colorScheme.error)
        }
    }

    deleteCandidate?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete this screenshot?") },
            text = { Text("Android may ask you to confirm before the file is removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        deleteCandidate = null
                        onDelete(item)
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ScreenshotCard(
    item: ScreenshotItem,
    onKeep: () -> Unit,
    onDelete: () -> Unit
) {
    var dragAmount by remember { mutableFloatStateOf(0f) }

    AsyncImage(
        model = item.uri,
        contentDescription = item.displayName,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .fillMaxWidth()
            .height(520.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(item.id) {
                detectDragGestures(
                    onDragEnd = {
                        when {
                            dragAmount > 180f -> onKeep()
                            dragAmount < -180f -> onDelete()
                        }
                        dragAmount = 0f
                    },
                    onDragCancel = { dragAmount = 0f },
                    onDrag = { change, drag ->
                        change.consume()
                        dragAmount += drag.x
                    }
                )
            }
    )
}
