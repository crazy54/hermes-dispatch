package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.model.SessionListUiState
import com.nousresearch.hermes.data.repository.HermesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SessionListUiState())
    val state: StateFlow<SessionListUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeSessions().collect { sessions ->
                _state.update { it.copy(sessions = sessions) }
            }
        }
    }

    fun refresh(config: ConnectionConfig?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.refreshSessions(config)
                .onSuccess { _state.update { it.copy(isLoading = false) } }
                .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
        }
    }
}
