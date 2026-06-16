package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.*
import com.nousresearch.hermes.data.repository.HermesRepository
import com.nousresearch.hermes.util.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: HermesRepository,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {

    private val _state = MutableStateFlow(ChatUiState())
    val state: StateFlow<ChatUiState> = _state.asStateFlow()

    private var streamJob: Job? = null
    private val activeToolCallIds = mutableMapOf<String, String>()

    // ── Session loading ───────────────────────────────────────────────────────

    fun loadSession(sessionId: String, config: ConnectionConfig) {
        _state.update { it.copy(currentSessionId = sessionId, isLoading = true, error = null) }

        // Observe local DB messages immediately
        viewModelScope.launch {
            repository.observeMessages(sessionId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }

        // Fetch from gateway and persist
        viewModelScope.launch {
            repository.loadSessionMessages(config, sessionId)
                .onFailure { err ->
                    _state.update { it.copy(error = "Could not load messages: ${err.message}") }
                }
            _state.update { it.copy(isLoading = false) }
        }
    }

    // ── New session ───────────────────────────────────────────────────────────

    fun startNewSession(config: ConnectionConfig) {
        _state.update { ChatUiState(isLoading = true) }
        activeToolCallIds.clear()

        viewModelScope.launch {
            repository.createSession(config)
                .onSuccess { session ->
                    _state.update { it.copy(
                        currentSessionId = session.sessionId,
                        isLoading = false,
                    )}
                }
                .onFailure { err ->
                    _state.update { it.copy(
                        isLoading = false,
                        error = "Could not create session: ${err.message}",
                    )}
                }
        }
    }

    // ── Send message ──────────────────────────────────────────────────────────

    fun sendMessage(text: String, config: ConnectionConfig) {
        val sessionId = _state.value.currentSessionId
        if (sessionId == null) {
            // No session yet — create one first, then send
            _state.update { ChatUiState(isLoading = true) }
            viewModelScope.launch {
                repository.createSession(config)
                    .onSuccess { session ->
                        _state.update { it.copy(currentSessionId = session.sessionId, isLoading = false) }
                        doSendMessage(text, config, session.sessionId)
                    }
                    .onFailure { err ->
                        _state.update { it.copy(isLoading = false, error = "Could not create session: ${err.message}") }
                    }
            }
            return
        }
        doSendMessage(text, config, sessionId)
    }

    private fun doSendMessage(text: String, config: ConnectionConfig, sessionId: String) {
        val userMsgId = UUID.randomUUID().toString()
        val userMessage = ChatMessage(
            id        = userMsgId,
            sessionId = sessionId,
            role      = MessageRole.USER,
            content   = text,
            timestamp = System.currentTimeMillis(),
        )

        _state.update { it.copy(
            messages         = it.messages + userMessage,
            currentSessionId = sessionId,
            isLoading        = true,
            error            = null,
        )}

        viewModelScope.launch { repository.saveMessage(userMessage) }

        val assistantMsgId = UUID.randomUUID().toString()
        _state.update { it.copy(
            messages           = it.messages + ChatMessage(
                id         = assistantMsgId,
                sessionId  = sessionId,
                role       = MessageRole.ASSISTANT,
                content    = "",
                timestamp  = System.currentTimeMillis(),
                isStreaming = true,
            ),
            streamingMessageId = assistantMsgId,
        )}

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            repository.streamMessage(config, sessionId, text)
                .catch { err ->
                    _state.update { it.copy(
                        isLoading          = false,
                        streamingMessageId = null,
                        error              = err.message ?: "Stream error",
                        messages           = it.messages.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                        },
                    )}
                }
                .collect { event -> handleStreamEvent(event, assistantMsgId, sessionId) }
        }
    }

    // ── Stream event handler ──────────────────────────────────────────────────

    private suspend fun handleStreamEvent(
        event: StreamEvent,
        assistantMsgId: String,
        sessionId: String,
    ) {
        when (event) {
            is StreamEvent.SessionInfo -> {
                _state.update { it.copy(currentSessionId = event.sessionId) }
                repository.upsertSession(HermesSession(
                    sessionId    = event.sessionId,
                    title        = event.title,
                    createdAt    = System.currentTimeMillis(),
                    updatedAt    = System.currentTimeMillis(),
                ))
            }

            is StreamEvent.TextDelta -> {
                _state.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(content = msg.content + event.delta) else msg
                    })
                }
            }

            is StreamEvent.TextDone -> {
                val finalMsg = _state.value.messages.find { it.id == assistantMsgId }
                if (finalMsg != null) {
                    repository.saveMessage(finalMsg.copy(
                        content    = event.content.ifEmpty { finalMsg.content },
                        isStreaming = false,
                    ))
                }
                _state.update { it.copy(
                    isLoading          = false,
                    streamingMessageId = null,
                    messages           = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(
                            content    = event.content.ifEmpty { msg.content },
                            isStreaming = false,
                        ) else msg
                    },
                )}
            }

            is StreamEvent.ToolStart -> {
                val toolMsgId = UUID.randomUUID().toString()
                activeToolCallIds[event.callId] = toolMsgId
                _state.update { it.copy(
                    messages = it.messages + ChatMessage(
                        id        = toolMsgId,
                        sessionId = sessionId,
                        role      = MessageRole.TOOL_CALL,
                        content   = "",
                        timestamp = System.currentTimeMillis(),
                        toolName  = event.name,
                        toolStatus = ToolStatus.RUNNING,
                    )
                )}
            }

            is StreamEvent.ToolDone -> {
                val msgId = activeToolCallIds.remove(event.callId) ?: return
                _state.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == msgId) msg.copy(
                            toolStatus = if (event.success) ToolStatus.SUCCESS else ToolStatus.ERROR,
                        ) else msg
                    })
                }
            }

            is StreamEvent.ApprovalRequired -> {
                notificationHelper.approvalNeeded(event.request.command)
                _state.update { state ->
                    state.copy(
                        pendingApproval = event.request,
                        messages        = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(
                                approvalRequest = event.request,
                                toolStatus      = ToolStatus.WAITING_APPROVAL,
                            ) else msg
                        }
                    )
                }
            }

            is StreamEvent.Error -> {
                _state.update { it.copy(
                    isLoading          = false,
                    streamingMessageId = null,
                    error              = event.message,
                    messages           = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    },
                )}
            }

            is StreamEvent.Done -> {
                notificationHelper.taskCompleted("Response complete")
                _state.update { it.copy(
                    isLoading          = false,
                    streamingMessageId = null,
                    messages           = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    },
                )}
            }
        }
    }

    // ── Approval / stop ───────────────────────────────────────────────────────

    fun submitApproval(config: ConnectionConfig, approvalId: String, approved: Boolean) {
        _state.update { it.copy(pendingApproval = null) }
        viewModelScope.launch { repository.submitApproval(config, approvalId, approved) }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        val sid = _state.value.streamingMessageId
        _state.update { it.copy(
            isLoading          = false,
            streamingMessageId = null,
            messages           = it.messages.map { msg ->
                if (msg.id == sid) msg.copy(isStreaming = false) else msg
            },
        )}
    }

    fun clearError() = _state.update { it.copy(error = null) }
}
