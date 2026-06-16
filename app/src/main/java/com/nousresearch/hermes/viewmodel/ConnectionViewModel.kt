package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.model.ConnectionState
import com.nousresearch.hermes.data.repository.HermesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ConnectionState())
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.connectionConfig.collect { config ->
                if (config != null) {
                    _state.update { it.copy(config = config, isConnected = true) }
                } else {
                    _state.update { it.copy(config = null, isConnected = false) }
                }
            }
        }
    }

    fun connect(gatewayUrl: String, token: String, profile: String = "default") {
        viewModelScope.launch {
            _state.update { it.copy(isConnecting = true, error = null) }
            val config = ConnectionConfig(
                gatewayUrl = gatewayUrl.trimEnd('/'),
                token = token,
                profileName = profile,
            )
            repository.testConnection(config)
                .onSuccess {
                    val profiles = repository.listProfiles(config).getOrDefault(listOf(config.profileName, "default")).distinct()
                    repository.saveConnection(config)
                    _state.update {
                        it.copy(
                            isConnecting = false,
                            isConnected = true,
                            config = config,
                            profiles = profiles,
                        )
                    }
                }
                .onFailure { err ->
                    _state.update { it.copy(isConnecting = false, error = err.message) }
                }
        }
    }

    fun disconnect() {
        viewModelScope.launch {
            repository.clearConnection()
            _state.update { ConnectionState() }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    fun changeProfile(profile: String) {
        val current = _state.value.config ?: return
        val updated = current.copy(profileName = profile)
        viewModelScope.launch {
            repository.saveConnection(updated)
            _state.update { it.copy(config = updated) }
        }
    }

    fun refreshProfiles() {
        val current = _state.value.config ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoadingProfiles = true) }
            repository.listProfiles(current)
                .onSuccess { profiles ->
                    _state.update { it.copy(isLoadingProfiles = false, profiles = profiles) }
                }
                .onFailure { err ->
                    _state.update { it.copy(isLoadingProfiles = false, error = err.message) }
                }
        }
    }
}
