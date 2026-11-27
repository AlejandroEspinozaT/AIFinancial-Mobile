package com.aifinancial.mobile.ui.overview

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinancial.mobile.data.local.CachedBudgetEntity
import com.aifinancial.mobile.data.local.CachedTransactionEntity
import com.aifinancial.mobile.data.repository.BudgetsCacheRepository
import com.aifinancial.mobile.data.repository.TransactionsCacheRepository
import com.aifinancial.mobile.sync.ReadCacheSyncScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * Reads only from the two cache repositories' `Flow`s — never calls
 * [com.aifinancial.mobile.data.remote.TransactionsApi]/`BudgetsApi` itself.
 * See specs/architecture/overview.md: the UI layer never talks to the
 * network directly, on the read side exactly as on Stage 1's write side.
 */
@HiltViewModel
class OverviewViewModel @Inject constructor(
    transactionsCacheRepository: TransactionsCacheRepository,
    budgetsCacheRepository: BudgetsCacheRepository,
    private val readCacheSyncScheduler: ReadCacheSyncScheduler,
) : ViewModel() {

    val transactions: StateFlow<List<CachedTransactionEntity>> = transactionsCacheRepository.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val transactionsLastUpdated: StateFlow<Instant?> = transactionsCacheRepository.observeLastUpdated()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val budgets: StateFlow<List<CachedBudgetEntity>> = budgetsCacheRepository.observeBudgets()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val budgetsLastUpdated: StateFlow<Instant?> = budgetsCacheRepository.observeLastUpdated()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    init {
        // Opening this screen is itself a trigger per
        // specs/phases/stage-3-offline-read-cache.md's "periodic/triggered"
        // requirement — if there's connectivity right now, don't make the
        // user wait for the next periodic window.
        readCacheSyncScheduler.scheduleImmediate()
    }
}
