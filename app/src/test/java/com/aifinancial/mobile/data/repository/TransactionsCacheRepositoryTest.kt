package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.CacheMetadataEntity
import com.aifinancial.mobile.data.local.CachedTransactionEntity
import com.aifinancial.mobile.data.local.TransactionsDao
import com.aifinancial.mobile.data.remote.TransactionsApi
import com.aifinancial.mobile.data.remote.dto.PagedResultDto
import com.aifinancial.mobile.data.remote.dto.TransactionDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

/**
 * Exercises specs/phases/stage-3-offline-read-cache.md's refresh logic: a
 * successful pull atomically replaces the cache and records "last
 * updated"; any failure mode leaves the existing cache untouched rather
 * than guessing or partially overwriting it.
 */
class TransactionsCacheRepositoryTest {

    private val dao = mockk<TransactionsDao>(relaxed = true)
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val transactionsApi = mockk<TransactionsApi>()
    private val repository = TransactionsCacheRepository(dao, cacheMetadataDao, transactionsApi)

    private fun dto() = TransactionDto(
        id = "txn-1",
        documentId = "doc-1",
        date = "2024-01-15",
        description = "Coffee",
        amount = 4.5,
        currency = "USD",
        type = "Debit",
        category = "Dining",
    )

    @Test
    fun `a successful refresh replaces the cache and records when`() = runTest {
        coEvery { transactionsApi.getTransactions(page = 1, pageSize = 50) } returns
            Response.success(PagedResultDto(items = listOf(dto()), page = 1, pageSize = 50, totalCount = 1))

        val result = repository.refresh()

        assertThat(result).isTrue()
        coVerify {
            dao.replaceAll(
                match<List<CachedTransactionEntity>> { it.size == 1 && it.first().id == "txn-1" },
            )
        }
        coVerify { cacheMetadataDao.upsert(match<CacheMetadataEntity> { it.cacheKey == "transactions" }) }
    }

    @Test
    fun `a failed response never touches the cache`() = runTest {
        coEvery { transactionsApi.getTransactions(page = 1, pageSize = 50) } returns
            Response.error(503, "unavailable".toResponseBody(null))

        val result = repository.refresh()

        assertThat(result).isFalse()
        coVerify(exactly = 0) { dao.replaceAll(any()) }
        coVerify(exactly = 0) { cacheMetadataDao.upsert(any()) }
    }

    @Test
    fun `a network exception never touches the cache`() = runTest {
        coEvery { transactionsApi.getTransactions(page = 1, pageSize = 50) } throws IOException("timeout")

        val result = repository.refresh()

        assertThat(result).isFalse()
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }
}
