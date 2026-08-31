package com.example.screenshotcleaner.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.screenshotcleaner.data.settings.AppSettings
import com.example.screenshotcleaner.data.settings.SUPPORTED_SCREENSHOT_AGE_DAYS

@Composable
fun SettingsScreen(
    settings: AppSettings,
    onRemindersEnabledChange: (Boolean) -> Unit,
    onScreenshotAgeDaysChange: (Long) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Settings", style = MaterialTheme.typography.headlineSmall)
            TextButton(onClick = onBack) {
                Text("Review")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Daily reminders", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (settings.remindersEnabled) "Enabled" else "Paused",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Switch(
                checked = settings.remindersEnabled,
                onCheckedChange = onRemindersEnabledChange
            )
        }

        Column {
            Text(
                text = "Review screenshots older than",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Choose the age threshold for scans and reminders.",
                style = MaterialTheme.typography.bodyMedium
            )
            SUPPORTED_SCREENSHOT_AGE_DAYS.forEach { ageDays ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = settings.screenshotAgeDays == ageDays,
                        onClick = { onScreenshotAgeDaysChange(ageDays) }
                    )
                    Text(text = "$ageDays days")
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = onBack
        ) {
            Text("Done")
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}
