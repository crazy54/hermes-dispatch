package com.nousresearch.hermes.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nousresearch.hermes.data.model.ApprovalRequest
import com.nousresearch.hermes.data.model.ChatMessage
import com.nousresearch.hermes.data.model.ChatUiState
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.model.HermesSession
import com.nousresearch.hermes.data.model.MessageRole
import com.nousresearch.hermes.data.model.RiskLevel
import com.nousresearch.hermes.data.model.StreamEvent
import com.nousresearch.hermes.data.model.ToolStatus
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
    private var activeToolCallIds = mutableMapOf<String, String>() // callId -> messageId

    fun loadSession(sessionId: String, config: ConnectionConfig) {
        _state.update { it.copy(currentSessionId = sessionId) }
        viewModelScope.launch {
            repository.observeMessages(sessionId).collect { messages ->
                _state.update { it.copy(messages = messages) }
            }
        }
    }

    fun startNewSession(config: ConnectionConfig) {
        _state.update { ChatUiState() }
        activeToolCallIds.clear()
    }

    fun sendMessage(text: String, config: ConnectionConfig) {
        val userMsgId = UUID.randomUUID().toString()
        val sessionId = _state.value.currentSessionId ?: UUID.randomUUID().toString()

        val userMessage = ChatMessage(
            id = userMsgId,
            sessionId = sessionId,
            role = MessageRole.USER,
            content = text,
            timestamp = System.currentTimeMillis(),
        )

        _state.update { state ->
            state.copy(
                messages = state.messages + userMessage,
                currentSessionId = sessionId,
                isLoading = true,
                error = null,
            )
        }

        viewModelScope.launch { repository.saveMessage(userMessage) }

        // Build streaming assistant message placeholder
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMsgId,
            sessionId = sessionId,
            role = MessageRole.ASSISTANT,
            content = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true,
        )
        _state.update { it.copy(
            messages = it.messages + assistantPlaceholder,
            streamingMessageId = assistantMsgId,
        )}

        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            repository.streamMessage(
                gatewayUrl = config.gatewayUrl,
                token = config.token,
                message = text,
                sessionId = _state.value.currentSessionId,
                profile = config.profileName,
            )
            .catch { err ->
                _state.update { it.copy(
                    isLoading = false,
                    streamingMessageId = null,
                    error = "Stream error: ${err.message}",
                    messages = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    },
                )}
            }
            .collect { event -> handleStreamEvent(event, assistantMsgId, sessionId) }
        }
    }

    private suspend fun handleStreamEvent(
        event: StreamEvent,
        assistantMsgId: String,
        sessionId: String,
    ) {
        when (event) {
            is StreamEvent.SessionInfo -> {
                _state.update { it.copy(currentSessionId = event.sessionId) }
                val session = HermesSession(
                    sessionId = event.sessionId,
                    title = event.title,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                )
                repository.upsertSession(session)
            }

            is StreamEvent.TextDelta -> {
                _state.update { state ->
                    state.copy(messages = state.messages.map { msg ->
                        if (msg.id == assistantMsgId) {
                            msg.copy(content = msg.content + event.delta)
                        } else msg
                    })
                }
            }

            is StreamEvent.TextDone -> {
                val finalMsg = _state.value.messages.find { it.id == assistantMsgId }
                if (finalMsg != null) {
                    val done = finalMsg.copy(
                        content = event.content.ifEmpty { finalMsg.content },
                        isStreaming = false,
                    )
                    repository.saveMessage(done)
                }
                _state.update { it.copy(
                    isLoading = false,
                    streamingMessageId = null,
                    messages = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(
                            content = event.content.ifEmpty { msg.content },
                            isStreaming = false,
                        ) else msg
                    },
                )}
            }

            is StreamEvent.ToolStart -> {
                val toolMsgId = UUID.randomUUID().toString()
                activeToolCallIds[event.callId] = toolMsgId
                val toolMsg = ChatMessage(
                    id = toolMsgId,
                    sessionId = sessionId,
                    role = MessageRole.TOOL_CALL,
                    content = "",
                    timestamp = System.currentTimeMillis(),
                    toolName = event.name,
                    toolStatus = ToolStatus.RUNNING,
                )
                _state.update { it.copy(messages = it.messages + toolMsg) }
            }

            is StreamEvent.ToolDone -> {
                val msgId = activeToolCallIds[event.callId] ?: return
                activeToolCallIds.remove(event.callId)
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
                        messages = state.messages.map { msg ->
                            if (msg.id == assistantMsgId) msg.copy(
                                approvalRequest = event.request,
                                toolStatus = ToolStatus.WAITING_APPROVAL,
                            ) else msg
                        }
                    )
                }
            }

            is StreamEvent.Error -> {
                _state.update { it.copy(
                    isLoading = false,
                    streamingMessageId = null,
                    error = event.message,
                    messages = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    },
                )}
            }

            is StreamEvent.Done -> {
                notificationHelper.taskCompleted("Session ${event.sessionId.take(8)} finished responding")
                _state.update { it.copy(
                    isLoading = false,
                    streamingMessageId = null,
                    messages = it.messages.map { msg ->
                        if (msg.id == assistantMsgId) msg.copy(isStreaming = false) else msg
                    },
                )}
            }
        }
    }

    fun submitApproval(config: ConnectionConfig, approvalId: String, approved: Boolean) {
        _state.update { it.copy(pendingApproval = null) }
        viewModelScope.launch {
            repository.submitApproval(config, approvalId, approved)
        }
    }

    fun stopStreaming() {
        streamJob?.cancel()
        val streamingId = _state.value.streamingMessageId
        _state.update { it.copy(
            isLoading = false,
            streamingMessageId = null,
            messages = it.messages.map { msg ->
                if (msg.id == streamingId) msg.copy(isStreaming = false) else msg
            },
        )}
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
