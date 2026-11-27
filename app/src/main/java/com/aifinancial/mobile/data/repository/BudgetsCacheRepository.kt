package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.BudgetsDao
import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.CacheMetadataEntity
import com.aifinancial.mobile.data.local.CachedBudgetEntity
import com.aifinancial.mobile.data.remote.BudgetsApi
import com.aifinancial.mobile.data.remote.dto.BudgetDto
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Budgets' half of Stage 3's read path — see [TransactionsCacheRepository]'s KDoc. */
@Singleton
class BudgetsCacheRepository @Inject constructor(
    private val dao: BudgetsDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val budgetsApi: BudgetsApi,
) {

    fun observeBudgets(): Flow<List<CachedBudgetEntity>> = dao.observeAll()

    fun observeLastUpdated(): Flow<Instant?> =
        cacheMetadataDao.observe(CACHE_KEY).map { it?.let { entity -> Instant.ofEpochMilli(entity.lastUpdatedEpochMillis) } }

    suspend fun refresh(): Boolean = try {
        val response = budgetsApi.getBudgets()
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            false
        } else {
            dao.replaceAll(body.map { it.toEntity() })
            cacheMetadataDao.upsert(CacheMetadataEntity(CACHE_KEY, Instant.now().toEpochMilli()))
            true
        }
    } catch (_: IOException) {
        false
    }

    private fun BudgetDto.toEntity() = CachedBudgetEntity(
        id = id,
        category = category,
        monthlyLimit = monthlyLimit,
        amountSpent = amountSpent,
        percentUtilized = percentUtilized,
        isExceeded = isExceeded,
    )

    private companion object {
        const val CACHE_KEY = "budgets"
    }
}
