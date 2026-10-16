package com.aifinancial.mobile.core.queue

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class QueueStateMachineTest {

    private fun queuedDocument(status: PendingDocumentStatus = PendingDocumentStatus.QUEUED) = PendingDocument(
        localId = "doc-1",
        localFilePath = "/data/captures/doc-1.jpg",
        capturedAtUtc = Instant.parse("2026-01-01T00:00:00Z"),
        status = status,
    )

    @Test
    fun `applyOutcome marks the document UPLOADED and records the remote id on success`() {
        val result = QueueStateMachine.applyOutcome(
            queuedDocument(),
            SyncOutcome.Uploaded(remoteDocumentId = "remote-42"),
        )

        assertEquals(PendingDocumentStatus.UPLOADED, result.status)
        assertEquals("remote-42", result.remoteDocumentId)
        assertNull(result.failureReason)
    }

    @Test
    fun `applyOutcome marks the document FAILED with a reason on a real rejection`() {
        val result = QueueStateMachine.applyOutcome(
            queuedDocument(),
            SyncOutcome.Rejected(reason = "Unsupported file type"),
        )

        assertEquals(PendingDocumentStatus.FAILED, result.status)
        assertEquals("Unsupported file type", result.failureReason)
        assertNull(result.remoteDocumentId)
    }

    @Test
    fun `applyOutcome leaves an ambiguous result QUEUED instead of guessing success or failure`() {
        val result = QueueStateMachine.applyOutcome(
            queuedDocument(),
            SyncOutcome.AmbiguousNetworkFailure,
        )

        assertEquals(PendingDocumentStatus.QUEUED, result.status)
        assertNull(result.remoteDocumentId)
        assertNull(result.failureReason)
    }

    @Test
    fun `retry moves a FAILED document back to QUEUED and clears the failure reason`() {
        val failed = queuedDocument(PendingDocumentStatus.FAILED).copy(failureReason = "timed out")

        val result = QueueStateMachine.retry(failed)

        assertEquals(PendingDocumentStatus.QUEUED, result.status)
        assertNull(result.failureReason)
    }

    @Test
    fun `retry refuses a document that is not FAILED`() {
        val queued = queuedDocument(PendingDocumentStatus.QUEUED)

        assertThrows(IllegalStateException::class.java) {
            QueueStateMachine.retry(queued)
        }
    }

    @Test
    fun `retry refuses an already UPLOADED document to avoid a duplicate upload`() {
        val uploaded = queuedDocument(PendingDocumentStatus.UPLOADED).copy(remoteDocumentId = "remote-1")

        assertThrows(IllegalStateException::class.java) {
            QueueStateMachine.retry(uploaded)
        }
    }
}
