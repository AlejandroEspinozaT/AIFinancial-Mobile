package com.aifinancial.mobile.data.repository

import com.aifinancial.mobile.data.local.CacheMetadataDao
import com.aifinancial.mobile.data.local.CacheMetadataEntity
import com.aifinancial.mobile.data.local.CachedTransactionEntity
import com.aifinancial.mobile.data.local.TransactionsDao
import com.aifinancial.mobile.data.remote.TransactionsApi
import com.aifinancial.mobile.data.remote.dto.TransactionDto
import java.io.IOException
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Stage 3's read path (specs/phases/stage-3-offline-read-cache.md): reads
 * always come from Room ([observeTransactions]), exactly like the write
 * path's rule in specs/architecture/overview.md — the UI layer never talks
 * to [TransactionsApi] directly. [refresh] is the only place that does, and
 * only [com.aifinancial.mobile.sync.ReadCacheSyncWorker] calls it.
 */
@Singleton
class TransactionsCacheRepository @Inject constructor(
    private val dao: TransactionsDao,
    private val cacheMetadataDao: CacheMetadataDao,
    private val transactionsApi: TransactionsApi,
) {

    fun observeTransactions(): Flow<List<CachedTransactionEntity>> = dao.observeAll()

    fun observeLastUpdated(): Flow<Instant?> =
        cacheMetadataDao.observe(CACHE_KEY).map { it?.let { entity -> Instant.ofEpochMilli(entity.lastUpdatedEpochMillis) } }

    /**
     * Pulls the most recent page and atomically replaces the local cache —
     * never a guessed partial success. Returns `false` (never throws) on any
     * failure, so [com.aifinancial.mobile.sync.ReadCacheSyncWorker] can ask
     * WorkManager to retry without this becoming an unhandled crash.
     */
    suspend fun refresh(): Boolean = try {
        val response = transactionsApi.getTransactions(page = FIRST_PAGE, pageSize = RECENT_PAGE_SIZE)
        val body = response.body()
        if (!response.isSuccessful || body == null) {
            false
        } else {
            dao.replaceAll(body.items.map { it.toEntity() })
            cacheMetadataDao.upsert(CacheMetadataEntity(CACHE_KEY, Instant.now().toEpochMilli()))
            true
        }
    } catch (_: IOException) {
        false
    }

    private fun TransactionDto.toEntity() = CachedTransactionEntity(
        id = id,
        documentId = documentId,
        date = date,
        description = description,
        amount = amount,
        currency = currency,
        type = type,
        category = category,
    )

    private companion object {
        const val CACHE_KEY = "transactions"
        const val FIRST_PAGE = 1

        // A "simple list/summary view" per the spec, not a full paginated
        // replica — the most recent 50 is enough to be useful offline
        // without this cache growing unbounded.
        const val RECENT_PAGE_SIZE = 50
    }
}
