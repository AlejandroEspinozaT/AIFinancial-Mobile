package com.aifinancial.mobile.core.queue

/**
 * The three — and only three — ways a sync attempt for one [PendingDocument]
 * can end. [AmbiguousNetworkFailure] exists specifically because a client
 * can genuinely fail to learn whether the backend received a request (the
 * process dies mid-request, a timeout with no response body). Collapsing
 * that case into [Rejected] would mark a possibly-successful upload as
 * failed; collapsing it into [Uploaded] would be worse — claiming success
 * never confirmed. See specs/phases/stage-2-background-sync.md's
 * idempotency requirement.
 */
sealed interface SyncOutcome {
    data class Uploaded(val remoteDocumentId: String) : SyncOutcome

    data class Rejected(val reason: String) : SyncOutcome

    data object AmbiguousNetworkFailure : SyncOutcome
}
