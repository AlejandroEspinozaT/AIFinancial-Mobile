package com.aifinancial.mobile.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Matches the subset of AIFinancial backend's `DocumentDto` this app needs
 * from `POST /api/documents`. Returned on both a fresh upload (202) and a
 * byte-identical duplicate (200, the backend's existing hash-based dedupe) —
 * this app treats both as [com.aifinancial.mobile.core.queue.SyncOutcome.Uploaded],
 * never re-derives which one happened.
 */
@Serializable
data class DocumentUploadResponseDto(val id: String)
