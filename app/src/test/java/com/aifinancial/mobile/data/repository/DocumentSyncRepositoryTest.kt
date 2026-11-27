package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.core.queue.PendingDocument
import com.aifinancial.mobile.core.queue.PendingDocumentStatus
import com.aifinancial.mobile.core.queue.SyncOutcome
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.PendingDocumentDao
import com.aifinancial.mobile.data.local.PendingDocumentEntity
import com.aifinancial.mobile.data.local.toEntity
import com.aifinancial.mobile.data.remote.DocumentsApi
import com.aifinancial.mobile.data.remote.dto.DocumentUploadResponseDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlinx.coroutines.test.runTest
import okhttp3.MultipartBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import retrofit2.Response

/**
 * Exercises specs/phases/stage-2-background-sync.md's queue-draining logic
 * against a fake [DocumentsApi] — success, a definite rejection, and the
 * ambiguous-network-failure case the idempotency design exists for.
 */
class DocumentSyncRepositoryTest {

    private val dao = mockk<PendingDocumentDao>(relaxed = true)
    private val documentsApi = mockk<DocumentsApi>()
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val repository = DocumentSyncRepository(dao, documentsApi, cacheMetadataDao)
    private lateinit var tempFile: File

    @Before
    fun setUp() {
        tempFile = File.createTempFile("capture", ".jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }
    }

    @After
    fun tearDown() {
        tempFile.delete()
    }

    private fun document(status: PendingDocumentStatus = PendingDocumentStatus.QUEUED) = PendingDocument(
        localId = "local-1",
        localFilePath = tempFile.absolutePath,
        capturedAtUtc = Instant.now(),
        status = status,
    )

    @Test
    fun `successful upload becomes Uploaded with the server's document id`() = runTest {
        coEvery { documentsApi.upload(any(), any()) } returns Response.success(DocumentUploadResponseDto(id = "remote-1"))

        val outcome = repository.attemptUpload(document())

        assertThat(outcome).isEqualTo(SyncOutcome.Uploaded(remoteDocumentId = "remote-1"))
    }

    @Test
    fun `upload sends the document's localId as the Idempotency-Key`() = runTest {
        coEvery { documentsApi.upload(any(), any()) } returns Response.success(DocumentUploadResponseDto(id = "remote-1"))

        repository.attemptUpload(document())

        coVerify { documentsApi.upload(any<MultipartBody.Part>(), idempotencyKey = "local-1") }
    }

    @Test
    fun `a 4xx response is a definite Rejected, not retried automatically`() = runTest {
        coEvery { documentsApi.upload(any(), any()) } returns
            Response.error(413, "too large".toResponseBody(null))

        val outcome = repository.attemptUpload(document())

        assertThat(outcome).isInstanceOf(SyncOutcome.Rejected::class.java)
    }

    @Test
    fun `a 5xx response is treated as ambiguous, not a rejection`() = runTest {
        coEvery { documentsApi.upload(any(), any()) } returns
            Response.error(503, "unavailable".toResponseBody(null))

        val outcome = repository.attemptUpload(document())

        assertThat(outcome).isEqualTo(SyncOutcome.AmbiguousNetworkFailure)
    }

    @Test
    fun `a network exception is ambiguous, never guessed as success or rejection`() = runTest {
        coEvery { documentsApi.upload(any(), any()) } throws IOException("connection reset")

        val outcome = repository.attemptUpload(document())

        assertThat(outcome).isEqualTo(SyncOutcome.AmbiguousNetworkFailure)
    }

    @Test
    fun `a missing local file is Rejected without ever calling the API`() = runTest {
        tempFile.delete()

        val outcome = repository.attemptUpload(document())

        assertThat(outcome).isInstanceOf(SyncOutcome.Rejected::class.java)
        coVerify(exactly = 0) { documentsApi.upload(any(), any()) }
    }

    @Test
    fun `drainQueue persists the Uploaded outcome and reports nothing left queued`() = runTest {
        val entity = document().toEntity()
        coEvery { dao.getByStatus(PendingDocumentStatus.QUEUED.name) } returns listOf(entity)
        coEvery { documentsApi.upload(any(), any()) } returns Response.success(DocumentUploadResponseDto(id = "remote-1"))

        val anyStillQueued = repository.drainQueue()

        assertThat(anyStillQueued).isFalse()
        coVerify {
            dao.update(
                match<PendingDocumentEntity> {
                    it.status == PendingDocumentStatus.UPLOADED.name && it.remoteDocumentId == "remote-1"
                },
            )
        }
    }

    @Test
    fun `drainQueue reports a row still queued after an ambiguous failure`() = runTest {
        val entity = document().toEntity()
        coEvery { dao.getByStatus(PendingDocumentStatus.QUEUED.name) } returns listOf(entity)
        coEvery { documentsApi.upload(any(), any()) } throws IOException("connection reset")

        val anyStillQueued = repository.drainQueue()

        assertThat(anyStillQueued).isTrue()
        coVerify { dao.update(match<PendingDocumentEntity> { it.status == PendingDocumentStatus.QUEUED.name }) }
    }
}
