package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.core.queue.PendingDocumentStatus
import com.aifinancial.mobile.core.queue.QueueStateMachine
import com.aifinancial.mobile.core.queue.SyncOutcome
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.CacheMetadataEntity
import com.aifinancial.mobile.data.local.PendingDocumentDao
import com.aifinancial.mobile.data.local.toDomain
import com.aifinancial.mobile.data.local.toEntity
import com.aifinancial.mobile.data.remote.DocumentsApi
import java.io.File
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

/**
 * The entire queue-draining decision loop for Stage 2
 * (specs/phases/stage-2-background-sync.md). [com.aifinancial.mobile.sync.DocumentSyncWorker]
 * is a thin WorkManager wrapper around [drainQueue] — all the actual logic
 * lives here, where it's testable against a fake [DocumentsApi] with no
 * Android dependency beyond Retrofit's own types.
 */
@Singleton
class DocumentSyncRepository @Inject constructor(
    private val dao: PendingDocumentDao,
    private val documentsApi: DocumentsApi,
    private val cacheMetadataDao: CacheMetadataDao,
) {

    /**
     * Attempts every currently [PendingDocumentStatus.QUEUED] row once.
     * Returns `true` if any row is still [PendingDocumentStatus.QUEUED]
     * afterward (an [SyncOutcome.AmbiguousNetworkFailure] leaves it that
     * way) — the worker uses this to ask WorkManager for a retry with
     * backoff, since a transient failure shouldn't need a new connectivity
     * transition to be tried again.
     */
    suspend fun drainQueue(): Boolean {
        val queued = dao.getByStatus(PendingDocumentStatus.QUEUED.name).map { it.toDomain() }
        var anyStillQueued = false

        for (document in queued) {
            val outcome = attemptUpload(document)
            val updated = QueueStateMachine.applyOutcome(document, outcome)
            dao.update(updated.toEntity())
            if (updated.status == PendingDocumentStatus.QUEUED) {
                anyStillQueued = true
            }
        }

        // Recorded unconditionally, even when the queue was empty: Stage 4's
        // Settings screen shows this as "last synced" for the whole app, and
        // a pass that confirmed there was nothing pending is still a real,
        // just-happened sync attempt, not a no-op worth hiding.
        cacheMetadataDao.upsert(CacheMetadataEntity(DOCUMENTS_CACHE_KEY, Instant.now().toEpochMilli()))

        return anyStillQueued
    }

    /**
     * One upload attempt for one document. Never throws — every failure
     * mode becomes an explicit [SyncOutcome] for [QueueStateMachine] to
     * apply, per specs/architecture/overview.md's "never guesses success
     * from 'the request was sent'".
     */
    internal suspend fun attemptUpload(document: PendingDocument): SyncOutcome {
        val file = File(document.localFilePath)
        if (!file.exists()) {
            // Retrying this is never going to work — the file it would
            // upload no longer exists. A definite, user-actionable failure,
            // not an ambiguous one.
            return SyncOutcome.Rejected("Local file no longer exists")
        }

        val part = MultipartBody.Part.createFormData(
            "file",
            file.name,
            file.asRequestBody("image/jpeg".toMediaType()),
        )

        return try {
            val response = documentsApi.upload(part, idempotencyKey = document.localId)
            when {
                response.isSuccessful -> {
                    val body = response.body()
                        // A 2xx with no body is itself ambiguous: the server
                        // may have accepted it, but this client never
                        // learned the resulting document id. Leaving it
                        // QUEUED relies on the Idempotency-Key to make the
                        // inevitable retry safe rather than guessing success
                        // here without one.
                        ?: return SyncOutcome.AmbiguousNetworkFailure
                    SyncOutcome.Uploaded(remoteDocumentId = body.id)
                }

                response.code() in 400..499 ->
                    SyncOutcome.Rejected("Upload rejected by server (HTTP ${response.code()})")

                else ->
                    // 5xx: the server told us something, but not whether our
                    // specific upload was processed. Treated the same as a
                    // network failure — retryable, never a guessed success.
                    SyncOutcome.AmbiguousNetworkFailure
            }
        } catch (_: IOException) {
            SyncOutcome.AmbiguousNetworkFailure
        }
    }

    private companion object {
        const val DOCUMENTS_CACHE_KEY = "documents"
    }
}
