package com.aifinancial.mobile.core.queue

import java.time.Instant

/**
 * Status of a document captured on this device.
 *
 * This is a 3-state machine, not a boolean "synced" flag, on purpose: a
 * document is never silently dropped on failure (FAILED is explicit and
 * user-retryable, never a deleted row), and a locally queued document is
 * never conflated with a confirmed one (see specs/architecture/overview.md).
 */
enum class PendingDocumentStatus {
    QUEUED,
    UPLOADED,
    FAILED,
}

/**
 * Pure domain representation of a captured document and its upload state.
 * The Android-side Room entity (app/src/main/java/.../data/local) maps to
 * and from this type — this type itself has no persistence or Android
 * dependency, which is exactly why it can be tested in a sandbox with no
 * access to the Android SDK.
 */
data class PendingDocument(
    val localId: String,
    val localFilePath: String,
    val capturedAtUtc: Instant,
    val status: PendingDocumentStatus,
    val remoteDocumentId: String? = null,
    val failureReason: String? = null,
) {
    init {
        require(localId.isNotBlank()) { "localId must not be blank" }
        require(localFilePath.isNotBlank()) { "localFilePath must not be blank" }
        require(status == PendingDocumentStatus.UPLOADED || remoteDocumentId == null) {
            "remoteDocumentId must only be set once status is UPLOADED"
        }
    }
}
