package com.aifinancial.mobile.core.queue

/**
 * The entire decision logic for what happens to a [PendingDocument] after a
 * sync attempt, or after a user asks to retry a failed one. Deliberately the
 * only place this decision is made — the Android-side sync worker (Stage 2)
 * calls this and persists whatever it returns; it does not re-derive the
 * decision itself.
 */
object QueueStateMachine {

    /**
     * Applies the real result of one sync attempt. Never guesses: an
     * [SyncOutcome.AmbiguousNetworkFailure] leaves the document [PendingDocumentStatus.QUEUED]
     * for a future retry — relying on the backend's own document-hash
     * dedupe (already built into the AIFinancial backend) to make that
     * retry safe, rather than inventing a second idempotency mechanism here.
     */
    fun applyOutcome(document: PendingDocument, outcome: SyncOutcome): PendingDocument = when (outcome) {
        is SyncOutcome.Uploaded -> document.copy(
            status = PendingDocumentStatus.UPLOADED,
            remoteDocumentId = outcome.remoteDocumentId,
            failureReason = null,
        )

        is SyncOutcome.Rejected -> document.copy(
            status = PendingDocumentStatus.FAILED,
            failureReason = outcome.reason,
        )

        SyncOutcome.AmbiguousNetworkFailure -> document.copy(status = PendingDocumentStatus.QUEUED)
    }

    /**
     * The user-initiated "retry" action from a failed document's detail
     * screen (specs/phases/stage-2-background-sync.md). Only a [PendingDocumentStatus.FAILED]
     * document can be retried — retrying a [PendingDocumentStatus.QUEUED] one is redundant
     * (it will be picked up by the next sync pass regardless) and retrying
     * an [PendingDocumentStatus.UPLOADED] one would risk a duplicate upload for no reason,
     * so both are rejected rather than silently allowed.
     */
    fun retry(document: PendingDocument): PendingDocument {
        check(document.status == PendingDocumentStatus.FAILED) {
            "Only a FAILED document can be retried, was ${document.status}"
        }
        return document.copy(status = PendingDocumentStatus.QUEUED, failureReason = null)
    }
}
