package com.aifinancial.mobile.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun SettingsScreen(
    onUnlinked: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lastSynced by viewModel.lastSynced.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState is SettingsUiState.Unlinked) {
            onUnlinked()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text("This device", style = MaterialTheme.typography.titleMedium)
        Text(viewModel.deviceName ?: "Unknown device", style = MaterialTheme.typography.bodyMedium)
        viewModel.email?.let { email ->
            Text("Linked to $email", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = lastSyncedLabel(lastSynced),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text(
            "Unlinking removes this device from your account and clears everything stored on this phone — any photo still queued here will not be uploaded.",
            style = MaterialTheme.typography.bodySmall,
        )

        when (val state = uiState) {
            is SettingsUiState.Unlinking -> CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            is SettingsUiState.Error -> {
                Text(
                    state.message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                UnlinkButton(viewModel::onUnlinkRequested)
            }

            else -> UnlinkButton(viewModel::onUnlinkRequested)
        }
    }
}

@Composable
private fun UnlinkButton(onClick: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }

    if (confirming) {
        Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Unlink this device?", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) { Text("Yes, unlink") }
            OutlinedButton(onClick = { confirming = false }) { Text("Cancel") }
        }
    } else {
        OutlinedButton(onClick = { confirming = true }, modifier = Modifier.padding(top = 8.dp)) {
            Text("Unlink this device")
        }
    }
}

private fun lastSyncedLabel(lastSynced: Instant?): String {
    if (lastSynced == null) return "Last synced: never"
    val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())
    return "Last synced: ${formatter.format(lastSynced)}"
}
