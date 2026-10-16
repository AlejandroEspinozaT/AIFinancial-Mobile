package com.aifinancial.mobile.ui.pairing

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * No password is ever typed here — pairing is scan-only (see
 * specs/architecture/api-integration.md). `deviceName` is a sensible,
 * non-configurable default for Stage 1; letting the user rename it is a
 * Stage 4 (device management/polish) concern, not load-bearing here.
 */
@Composable
fun PairingScreen(
    onPaired: () -> Unit,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val deviceName = remember { Build.MODEL ?: "Android device" }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        val contents = result.contents
        if (contents != null) {
            viewModel.onCodeScanned(contents, deviceName)
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is PairingUiState.Paired) {
            onPaired()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Link this device to your AIFinancial account", style = MaterialTheme.typography.titleMedium)
        Text(
            "Open AIFinancial on the web, go to Settings, and scan the QR code shown there.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 16.dp),
        )

        Button(
            enabled = uiState !is PairingUiState.Pairing,
            onClick = {
                scanLauncher.launch(
                    ScanOptions()
                        .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                        .setPrompt("Scan the pairing QR code from the web app")
                        .setBeepEnabled(false)
                        .setOrientationLocked(false),
                )
            },
        ) {
            Text("Scan QR code")
        }

        when (val state = uiState) {
            is PairingUiState.Pairing -> CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
            is PairingUiState.Error -> Text(
                state.message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )

            else -> Unit
        }
    }
}
