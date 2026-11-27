package com.aifinancial.mobile.data.remote

import com.aifinancial.mobile.data.remote.dto.DocumentUploadResponseDto
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface DocumentsApi {
    /**
     * The `Idempotency-Key` header is this app's answer to
     * specs/phases/stage-2-background-sync.md's idempotency requirement: it
     * uses the backend's own purpose-built `IdempotencyActionFilter`
     * (see AIFinancial's `Api/Filters/IdempotencyActionFilter.cs`), keyed on
     * this document's stable [com.aifinancial.mobile.core.queue.PendingDocument.localId].
     * A retried request with the same key replays the original response
     * instead of re-running the upload — this is strictly more precise than
     * relying on the backend's byte-hash dedupe alone (that's still there as
     * a second, independent backstop, not this app's primary mechanism).
     */
    @Multipart
    @POST("documents")
    suspend fun upload(
        @Part file: MultipartBody.Part,
        @Header("Idempotency-Key") idempotencyKey: String,
    ): Response<DocumentUploadResponseDto>
}
