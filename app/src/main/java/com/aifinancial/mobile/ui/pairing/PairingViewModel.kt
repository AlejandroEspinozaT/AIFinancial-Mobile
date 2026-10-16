package com.aifinancial.mobile.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aifinancial.mobile.core.pairing.PairingResult
import com.aifinancial.mobile.data.repository.PairingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface PairingUiState {
    data object Idle : PairingUiState
    data object Pairing : PairingUiState
    data object Paired : PairingUiState
    data class Error(val message: String) : PairingUiState
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val pairingRepository: PairingRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PairingUiState>(PairingUiState.Idle)
    val uiState: StateFlow<PairingUiState> = _uiState.asStateFlow()

    fun onCodeScanned(code: String, deviceName: String) {
        viewModelScope.launch {
            _uiState.value = PairingUiState.Pairing
            when (pairingRepository.redeem(code, deviceName)) {
                is PairingResult.Paired -> _uiState.value = PairingUiState.Paired
                PairingResult.InvalidOrExpired -> _uiState.value = PairingUiState.Error(
                    // Deliberately generic — see PairingRepository/specs/architecture/api-integration.md:
                    // this app does not guess at *why* a code failed beyond what the backend says.
                    "Code expired or already used — generate a new one in the web app",
                )
            }
        }
    }
}
