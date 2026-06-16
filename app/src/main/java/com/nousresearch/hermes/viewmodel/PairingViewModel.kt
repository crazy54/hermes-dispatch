package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.repository.HermesRepository
import com.nousresearch.hermes.data.model.PairRedeemResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PairingUiState(
    val isRedeeming: Boolean = false,
    val error:       String?  = null,
    val success:     Boolean  = false,
    val token:       String?  = null,
    val gatewayUrl:  String?  = null,
    val profile:     String   = "default",
)

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PairingUiState())
    val state: StateFlow<PairingUiState> = _state

    /**
     * Called when the QR scanner or manual entry resolves to a gateway URL + code.
     * Calls POST /api/pair/redeem and gets back a token.
     */
    fun redeemCode(gatewayUrl: String, code: String, profile: String = "default") {
        _state.update { it.copy(isRedeeming = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.redeemPairingCode(gatewayUrl.trimEnd('/'), code.trim())
                _state.update {
                    it.copy(
                        isRedeeming = false,
                        success     = true,
                        token       = result.token,
                        gatewayUrl  = gatewayUrl,
                        profile     = result.profile ?: profile,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRedeeming = false,
                        error       = e.message ?: "Failed to redeem pairing code",
                    )
                }
            }
        }
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
