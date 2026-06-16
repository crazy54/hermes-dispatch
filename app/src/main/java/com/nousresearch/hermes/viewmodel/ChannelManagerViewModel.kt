package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.ChannelManagerUiState
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.repository.HermesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChannelManagerViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ChannelManagerUiState())
    val state: StateFlow<ChannelManagerUiState> = _state.asStateFlow()

    fun refresh(config: ConnectionConfig?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.listChannels(config)
                .onSuccess { channels -> _state.update { it.copy(isLoading = false, channels = channels) } }
                .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
        }
    }
}
