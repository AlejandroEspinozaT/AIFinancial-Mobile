package com.aifinancial.mobile.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinancial.mobile.data.repository.BudgetsCacheRepository
import com.aifinancial.mobile.data.repository.SettingsRepository
import com.aifinancial.mobile.data.repository.TransactionsCacheRepository
import com.aifinancial.mobile.data.repository.UnlinkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SettingsUiState {
    data object Idle : SettingsUiState
    data object Unlinking : SettingsUiState
    data object Unlinked : SettingsUiState
    data class Error(val message: String) : SettingsUiState
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    transactionsCacheRepository: TransactionsCacheRepository,
    budgetsCacheRepository: BudgetsCacheRepository,
) : ViewModel() {

    val deviceName: String? = settingsRepository.deviceName()
    val email: String? = settingsRepository.email()

    private val _uiState = MutableStateFlow<SettingsUiState>(SettingsUiState.Idle)
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    /**
     * "A 'last synced' indicator for the whole app, not just one screen"
     * (specs/phases/stage-4-device-management-and-polish.md) — the most
     * recent of all three independent sync paths this app has, not just
     * whichever one this screen happens to be about.
     */
    val lastSynced: StateFlow<Instant?> = combine(
        settingsRepository.observeDocumentsLastSynced(),
        transactionsCacheRepository.observeLastUpdated(),
        budgetsCacheRepository.observeLastUpdated(),
    ) { documents, transactions, budgets ->
        listOfNotNull(documents, transactions, budgets).maxOrNull()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun onUnlinkRequested() {
        viewModelScope.launch {
            _uiState.value = SettingsUiState.Unlinking
            _uiState.value = when (settingsRepository.unlinkDevice()) {
                UnlinkResult.Success -> SettingsUiState.Unlinked
                UnlinkResult.Failed -> SettingsUiState.Error(
                    "Couldn't unlink this device — check your connection and try again.",
                )
            }
        }
    }
}
