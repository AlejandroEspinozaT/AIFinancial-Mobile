package com.aifinancial.mobile.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aifinancial.mobile.data.repository.BudgetsCacheRepository
import com.aifinancial.mobile.data.repository.TransactionsCacheRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Stage 3's "refresh-on-connectivity" worker
 * (specs/phases/stage-3-offline-read-cache.md). Thin by design — see
 * [DocumentSyncWorker]'s KDoc for the same rule applied here.
 */
@HiltWorker
class ReadCacheSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val transactionsCacheRepository: TransactionsCacheRepository,
    private val budgetsCacheRepository: BudgetsCacheRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val transactionsRefreshed = transactionsCacheRepository.refresh()
        val budgetsRefreshed = budgetsCacheRepository.refresh()
        return if (transactionsRefreshed && budgetsRefreshed) Result.success() else Result.retry()
    }
}
