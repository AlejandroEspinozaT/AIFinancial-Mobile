package com.aifinancial.mobile.core.queue

import java.time.Instant
import org.junit.Assert.assertThrows
import org.junit.Test

class PendingDocumentTest {

    @Test
    fun `rejects a remoteDocumentId on anything other than UPLOADED`() {
        assertThrows(IllegalArgumentException::class.java) {
            PendingDocument(
                localId = "doc-1",
                localFilePath = "/data/captures/doc-1.jpg",
                capturedAtUtc = Instant.parse("2026-01-01T00:00:00Z"),
                status = PendingDocumentStatus.QUEUED,
                remoteDocumentId = "should-not-be-allowed",
            )
        }
    }

    @Test
    fun `rejects a blank localId`() {
        assertThrows(IllegalArgumentException::class.java) {
            PendingDocument(
                localId = "",
                localFilePath = "/data/captures/doc-1.jpg",
                capturedAtUtc = Instant.parse("2026-01-01T00:00:00Z"),
                status = PendingDocumentStatus.QUEUED,
            )
        }
    }
}
