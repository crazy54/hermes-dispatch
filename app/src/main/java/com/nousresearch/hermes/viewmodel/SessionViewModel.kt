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

    /** Load profiles + sessions for the current profile. */
    fun initialize(config: ConnectionConfig?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // Load profiles from gateway
            repository.listProfiles(config)
                .onSuccess { profiles ->
                    val current = _state.value.selectedProfile
                    val valid   = if (current in profiles) current else profiles.firstOrNull() ?: "default"
                    _state.update { it.copy(profiles = profiles, selectedProfile = valid) }
                    // Now load sessions for the active profile
                    loadSessionsForProfile(config, valid)
                }
                .onFailure {
                    // Profiles failed — still try loading sessions
                    loadSessionsForProfile(config, _state.value.selectedProfile)
                }
        }
    }

    fun selectProfile(config: ConnectionConfig?, profile: String) {
        _state.update { it.copy(selectedProfile = profile) }
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadSessionsForProfile(config, profile)
        }
    }

    fun refresh(config: ConnectionConfig?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            loadSessionsForProfile(config, _state.value.selectedProfile)
        }
    }

    private suspend fun loadSessionsForProfile(config: ConnectionConfig, profile: String) {
        // Use a config copy with the chosen profile so the API filters correctly
        val profileConfig = config.copy(profileName = profile)
        repository.refreshSessions(profileConfig)
            .onSuccess { _state.update { it.copy(isLoading = false) } }
            .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
    }
}
