package com.aifinancial.mobile.ui.capture

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.core.queue.PendingDocumentStatus

@Composable
fun CaptureScreen(viewModel: CaptureViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val queue by viewModel.queue.collectAsStateWithLifecycle()

    val captureLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCaptureResult(success)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Capture a document", style = MaterialTheme.typography.titleMedium)
        Text(
            "Works with no connection — captured documents are queued and shown below immediately.",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        Button(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val file = CaptureFileFactory.createCaptureFile(context)
                val uri = CaptureFileFactory.uriFor(context, file)
                viewModel.onCaptureStarted(file.absolutePath)
                captureLauncher.launch(uri)
            },
        ) {
            Text("Take photo")
        }

        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

        Text("Queue (${queue.size})", style = MaterialTheme.typography.titleSmall)

        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(queue, key = { it.localId }) { document ->
                QueueRow(document = document, onRetry = { viewModel.onRetry(document.localId) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun QueueRow(document: PendingDocument, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(document.localFilePath.substringAfterLast('/'), style = MaterialTheme.typography.bodyMedium)
        Text(
            text = statusLabel(document),
            style = MaterialTheme.typography.bodySmall,
            color = when (document.status) {
                PendingDocumentStatus.UPLOADED -> MaterialTheme.colorScheme.primary
                PendingDocumentStatus.FAILED -> MaterialTheme.colorScheme.error
                PendingDocumentStatus.QUEUED -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        if (document.status == PendingDocumentStatus.FAILED) {
            TextButton(onClick = onRetry) {
                Text("Retry")
            }
        }
    }
}

private fun statusLabel(document: PendingDocument): String = when (document.status) {
    // Only QUEUED is reachable until Stage 2 (background sync) exists —
    // UPLOADED/FAILED labels are written now so the UI doesn't need
    // revisiting once that stage lands.
    PendingDocumentStatus.QUEUED -> "Queued — will upload when online"
    PendingDocumentStatus.UPLOADED -> "Uploaded"
    PendingDocumentStatus.FAILED -> "Failed: ${document.failureReason ?: "unknown reason"}"
}
