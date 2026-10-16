package com.aifinancial.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.core.queue.PendingDocumentStatus
import java.time.Instant

/**
 * Room's persisted shape for a [PendingDocument]. Kept as a thin, separate
 * type from the domain model (see specs/architecture/overview.md) rather
 * than annotating [PendingDocument] itself with Room annotations — that
 * would pull an AndroidX/Room dependency into `:core`, defeating the whole
 * point of keeping that module resolvable without Google's Maven
 * (specs/architecture/technology-choices.md).
 */
@Entity(tableName = "pending_documents")
data class PendingDocumentEntity(
    @PrimaryKey val localId: String,
    val localFilePath: String,
    val capturedAtEpochMillis: Long,
    val status: String,
    val remoteDocumentId: String?,
    val failureReason: String?,
)

fun PendingDocumentEntity.toDomain(): PendingDocument = PendingDocument(
    localId = localId,
    localFilePath = localFilePath,
    capturedAtUtc = Instant.ofEpochMilli(capturedAtEpochMillis),
    status = PendingDocumentStatus.valueOf(status),
    remoteDocumentId = remoteDocumentId,
    failureReason = failureReason,
)

fun PendingDocument.toEntity(): PendingDocumentEntity = PendingDocumentEntity(
    localId = localId,
    localFilePath = localFilePath,
    capturedAtEpochMillis = capturedAtUtc.toEpochMilli(),
    status = status.name,
    remoteDocumentId = remoteDocumentId,
    failureReason = failureReason,
)
