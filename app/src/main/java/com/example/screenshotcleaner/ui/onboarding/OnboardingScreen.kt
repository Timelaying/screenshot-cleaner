package com.example.screenshotcleaner.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(
    hasImagePermission: Boolean,
    imagePermissionStatus: String,
    hasNotificationPermission: Boolean,
    onGrantImagePermission: () -> Unit,
    onGrantNotificationPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Screenshot Cleaner",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Review screenshots older than 30 days and quickly decide what to keep or delete.",
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionCard(
            title = "Photo access",
            isGranted = hasImagePermission,
            status = imagePermissionStatus,
            actionLabel = "Grant access",
            onClick = onGrantImagePermission
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionCard(
            title = "Notifications",
            isGranted = hasNotificationPermission,
            status = if (hasNotificationPermission) "Ready" else "Required",
            actionLabel = "Enable alerts",
            onClick = onGrantNotificationPermission
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    isGranted: Boolean,
    status: String,
    actionLabel: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (!isGranted) {
                Button(onClick = onClick) {
                    Text(actionLabel)
                }
            }
        }
    }
}
