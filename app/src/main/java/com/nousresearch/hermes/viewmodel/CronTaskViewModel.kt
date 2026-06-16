package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.model.CronTaskUiState
import com.nousresearch.hermes.data.repository.HermesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CronTaskViewModel @Inject constructor(
    private val repository: HermesRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CronTaskUiState())
    val state: StateFlow<CronTaskUiState> = _state.asStateFlow()

    fun refresh(config: ConnectionConfig?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repository.listCronJobs(config)
                .onSuccess { jobs -> _state.update { it.copy(isLoading = false, jobs = jobs) } }
                .onFailure { err -> _state.update { it.copy(isLoading = false, error = err.message) } }
        }
    }

    fun create(config: ConnectionConfig?, name: String, schedule: String, prompt: String, deliver: String?) {
        if (config == null) return
        viewModelScope.launch {
            _state.update { it.copy(isCreating = true, error = null) }
            repository.createCronJob(config, schedule, prompt, name, deliver)
                .onSuccess { refresh(config) }
                .onFailure { err -> _state.update { it.copy(isCreating = false, error = err.message) } }
        }
    }

    fun runNow(config: ConnectionConfig?, id: String) {
        if (config == null) return
        viewModelScope.launch {
            repository.runCronJob(config, id)
                .onSuccess { refresh(config) }
                .onFailure { err -> _state.update { it.copy(error = err.message) } }
        }
    }

    fun delete(config: ConnectionConfig?, id: String) {
        if (config == null) return
        viewModelScope.launch {
            repository.deleteCronJob(config, id)
                .onSuccess { refresh(config) }
                .onFailure { err -> _state.update { it.copy(error = err.message) } }
        }
    }
}
