package com.aifinancial.mobile.ui.pairing

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Debug builds only (gated by `BuildConfig.DEBUG` at the call site): type the
 * pairing code instead of scanning it. The emulator's virtual camera can't
 * practically be pointed at a QR code from automation, which blocked running
 * offline-review's device checklist on an emulator. The typed code goes
 * through exactly the same `redeem` path as a scanned one — this only swaps
 * the input method, not any pairing logic.
 */
@Composable
fun DebugManualCodeEntry(
    enabled: Boolean,
    onSubmit: (String) -> Unit,
) {
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 32.dp),
    ) {
        Text("Debug build: enter the code manually", style = MaterialTheme.typography.labelMedium)
        OutlinedTextField(
            value = code,
            onValueChange = { code = it },
            singleLine = true,
            label = { Text("Pairing code") },
            modifier = Modifier.padding(vertical = 8.dp),
        )
        OutlinedButton(
            enabled = enabled && code.isNotBlank(),
            onClick = { onSubmit(code.trim()) },
        ) {
            Text("Link with code")
        }
    }
}
