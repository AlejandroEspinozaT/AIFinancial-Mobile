package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.BudgetsDao
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.CacheMetadataEntity
import com.aifinancial.mobile.data.local.CachedBudgetEntity
import com.aifinancial.mobile.data.remote.BudgetsApi
import com.aifinancial.mobile.data.remote.dto.BudgetDto
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import retrofit2.Response

class BudgetsCacheRepositoryTest {

    private val dao = mockk<BudgetsDao>(relaxed = true)
    private val cacheMetadataDao = mockk<CacheMetadataDao>(relaxed = true)
    private val budgetsApi = mockk<BudgetsApi>()
    private val repository = BudgetsCacheRepository(dao, cacheMetadataDao, budgetsApi)

    private fun dto() = BudgetDto(
        id = "budget-1",
        category = "Dining",
        monthlyLimit = 200.0,
        amountSpent = 150.0,
        percentUtilized = 75.0,
        isExceeded = false,
    )

    @Test
    fun `a successful refresh replaces the cache and records when`() = runTest {
        coEvery { budgetsApi.getBudgets() } returns Response.success(listOf(dto()))

        val result = repository.refresh()

        assertThat(result).isTrue()
        coVerify {
            dao.replaceAll(match<List<CachedBudgetEntity>> { it.size == 1 && it.first().id == "budget-1" })
        }
        coVerify { cacheMetadataDao.upsert(match<CacheMetadataEntity> { it.cacheKey == "budgets" }) }
    }

    @Test
    fun `a failed response never touches the cache`() = runTest {
        coEvery { budgetsApi.getBudgets() } returns Response.error(500, "error".toResponseBody(null))

        val result = repository.refresh()

        assertThat(result).isFalse()
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }

    @Test
    fun `a network exception never touches the cache`() = runTest {
        coEvery { budgetsApi.getBudgets() } throws IOException("timeout")

        val result = repository.refresh()

        assertThat(result).isFalse()
        coVerify(exactly = 0) { dao.replaceAll(any()) }
    }
}
