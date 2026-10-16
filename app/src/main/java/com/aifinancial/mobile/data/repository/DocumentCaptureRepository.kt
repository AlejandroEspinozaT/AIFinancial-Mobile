package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.core.queue.PendingDocumentStatus
import com.aifinancial.mobile.core.queue.QueueStateMachine
import com.aifinancial.mobile.data.local.PendingDocumentDao
import com.aifinancial.mobile.data.local.toDomain
import com.aifinancial.mobile.data.local.toEntity
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stage 1 scope only: capture and durable local queuing. Deliberately does
 * **not** attempt any network call — see
 * specs/phases/stage-1-device-pairing-and-offline-capture.md. Sync is
 * Stage 2's repository method, added once this queue has been proven
 * durable on its own.
 */
@Singleton
class DocumentCaptureRepository @Inject constructor(
    private val dao: PendingDocumentDao,
) {

    fun observeQueue(): Flow<List<PendingDocument>> =
        dao.observeAll().map { entities -> entities.map { it.toDomain() } }

    /**
     * Inserts the queued row and suspends until that insert actually
     * completes — the caller (the capture ViewModel) must not report
     * success to the user before this returns. This ordering is the entire
     * point of the architecture (specs/architecture/overview.md): the local
     * write is unconditional and happens first, with no network dependency
     * in the critical path at all.
     */
    suspend fun enqueue(localFilePath: String): PendingDocument {
        val document = PendingDocument(
            localId = UUID.randomUUID().toString(),
            localFilePath = localFilePath,
            capturedAtUtc = Instant.now(),
            status = PendingDocumentStatus.QUEUED,
        )
        dao.insert(document.toEntity())
        return document
    }

    /** The user-initiated retry action (Stage 2's queue-list "retry" button). */
    suspend fun retry(localId: String) {
        val entity = dao.getById(localId) ?: return
        val retried = QueueStateMachine.retry(entity.toDomain())
        dao.update(retried.toEntity())
    }
}
