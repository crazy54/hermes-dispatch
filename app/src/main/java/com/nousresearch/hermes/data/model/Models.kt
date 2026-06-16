package com.nousresearch.hermes.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ─── Connection / Auth ───────────────────────────────────────────────────────

@Serializable
data class ConnectionConfig(
    val gatewayUrl: String,
    val token: String,
    val profileName: String = "default",
)

@Serializable
data class PairRedeemRequest(val code: String)

@Serializable
data class PairRedeemResponse(
    val token:   String,
    val profile: String? = "default",
)

// ─── Gateway Capabilities ────────────────────────────────────────────────────

@Serializable
data class GatewayCapabilities(
    val `object`: String = "",
    val platform: String = "",
    val model: String = "",
    val auth: Map<String, String> = emptyMap(),
    val features: Map<String, Boolean> = emptyMap(),
    // profiles exposed via capabilities
    val profiles: List<String>? = null,
)

// ─── Messages ────────────────────────────────────────────────────────────────

@Serializable
enum class MessageRole { USER, ASSISTANT, TOOL_CALL, TOOL_RESULT, SYSTEM }

@Serializable
data class ChatMessage(
    val id: String,
    val sessionId: String,
    val role: MessageRole,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val toolName: String? = null,
    val toolStatus: ToolStatus? = null,
    val approvalRequest: ApprovalRequest? = null,
)

@Serializable
enum class ToolStatus { RUNNING, SUCCESS, ERROR, WAITING_APPROVAL }

@Serializable
data class ApprovalRequest(
    val approvalId: String,
    val command: String,
    val cwd: String,
    val riskLevel: RiskLevel,
    val description: String,
)

@Serializable
enum class RiskLevel { LOW, MEDIUM, HIGH, CRITICAL }

// ─── Session ─────────────────────────────────────────────────────────────────

@Serializable
data class HermesSession(
    val sessionId: String,
    val title: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val messageCount: Int = 0,
    val preview: String? = null,
    val source: String? = null,
)

// ─── Gateway session shape: { object, data: [...], limit, offset, has_more }
// Each session in data has: id, title, started_at, message_count, preview, etc.

@Serializable
data class GatewaySession(
    val id: String,
    val title: String? = null,
    @SerialName("started_at")  val startedAt:  Double? = null,
    @SerialName("ended_at")    val endedAt:    Double? = null,
    @SerialName("last_active") val lastActive: Double? = null,
    @SerialName("message_count") val messageCount: Int = 0,
    val preview: String? = null,
    val source: String? = null,
    val model: String? = null,
) {
    fun toHermesSession() = HermesSession(
        sessionId    = id,
        title        = title,
        createdAt    = ((startedAt ?: lastActive ?: 0.0) * 1000).toLong(),
        updatedAt    = ((lastActive ?: endedAt ?: startedAt ?: 0.0) * 1000).toLong(),
        messageCount = messageCount,
        preview      = preview,
        source       = source,
    )
}

@Serializable
data class SessionListResponse(
    val `object`: String = "list",
    val data: List<GatewaySession> = emptyList(),
    val limit: Int = 50,
    val offset: Int = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
) {
    /** Convenience for the repository */
    val sessions: List<HermesSession> get() = data.map { it.toHermesSession() }
}

@Serializable
data class CreateSessionResponse(
    val `object`: String = "",
    val session: GatewaySession,
)

// ─── Gateway message shape: { object, session_id, data: [...] }
// Each message has: id, session_id, role, content, timestamp, tool_name, ...

@Serializable
data class GatewayMessage(
    val id: Int = 0,
    @SerialName("session_id") val sessionId: String,
    val role: String,
    val content: String? = null,
    val timestamp: Double? = null,
    @SerialName("tool_name") val toolName: String? = null,
    @SerialName("tool_call_id") val toolCallId: String? = null,
) {
    fun toChatMessage(): ChatMessage {
        val msgRole = when (role.lowercase()) {
            "user"      -> MessageRole.USER
            "assistant" -> MessageRole.ASSISTANT
            "tool"      -> MessageRole.TOOL_RESULT
            "system"    -> MessageRole.SYSTEM
            else        -> MessageRole.ASSISTANT
        }
        return ChatMessage(
            id        = id.toString(),
            sessionId = sessionId,
            role      = msgRole,
            content   = content ?: "",
            timestamp = ((timestamp ?: 0.0) * 1000).toLong(),
            toolName  = toolName,
        )
    }
}

@Serializable
data class MessageListResponse(
    val `object`: String = "list",
    @SerialName("session_id") val sessionId: String = "",
    val data: List<GatewayMessage> = emptyList(),
)

// ─── API Request/Response ────────────────────────────────────────────────────

@Serializable
data class SendMessageRequest(
    val message: String,
    val stream: Boolean = true,
)

@Serializable
data class ApprovalResponse(
    @SerialName("approval_id") val approvalId: String,
    val approved: Boolean,
)

@Serializable
data class PairingRequest(
    val deviceName: String,
    val platform: String = "android",
)

@Serializable
data class PairingResponse(
    val token: String,
    val deviceId: String,
)

// ─── Streaming SSE Events ────────────────────────────────────────────────────

@Serializable
sealed class StreamEvent {
    @Serializable
    @SerialName("text_delta")
    data class TextDelta(val delta: String, @SerialName("session_id") val sessionId: String) : StreamEvent()

    @Serializable
    @SerialName("text_done")
    data class TextDone(@SerialName("session_id") val sessionId: String, val content: String) : StreamEvent()

    @Serializable
    @SerialName("tool_start")
    data class ToolStart(
        val name: String,
        @SerialName("call_id") val callId: String,
        @SerialName("session_id") val sessionId: String,
    ) : StreamEvent()

    @Serializable
    @SerialName("tool_done")
    data class ToolDone(
        @SerialName("call_id") val callId: String,
        @SerialName("session_id") val sessionId: String,
        val success: Boolean,
    ) : StreamEvent()

    @Serializable
    @SerialName("approval_required")
    data class ApprovalRequired(
        val request: ApprovalRequest,
        @SerialName("session_id") val sessionId: String,
    ) : StreamEvent()

    @Serializable
    @SerialName("session_info")
    data class SessionInfo(
        @SerialName("session_id") val sessionId: String,
        val title: String?,
    ) : StreamEvent()

    @Serializable
    @SerialName("error")
    data class Error(val message: String) : StreamEvent()

    @Serializable
    @SerialName("done")
    data class Done(@SerialName("session_id") val sessionId: String) : StreamEvent()
}

// ─── UI state models ─────────────────────────────────────────────────────────

data class ConnectionState(
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val config: ConnectionConfig? = null,
    val profiles: List<String> = listOf("default"),
    val isLoadingProfiles: Boolean = false,
    val error: String? = null,
)

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val currentSessionId: String? = null,
    val isLoading: Boolean = false,
    val streamingMessageId: String? = null,
    val error: String? = null,
    val pendingApproval: ApprovalRequest? = null,
)

data class SessionListUiState(
    val sessions: List<HermesSession> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

// ─── Cron / Task Scheduler ──────────────────────────────────────────────────

@Serializable
data class CronJob(
    val id: String,
    val name: String? = null,
    val schedule: String,
    val prompt: String,
    val enabled: Boolean = true,
    @SerialName("next_run_at") val nextRunAt: String? = null,
    @SerialName("last_run_at") val lastRunAt: String? = null,
    @SerialName("last_status") val lastStatus: String? = null,
    val deliver: String? = null,
)

@Serializable
data class CronJobListResponse(
    val jobs: List<CronJob> = emptyList(),
)

@Serializable
data class CreateCronJobRequest(
    val schedule: String,
    val prompt: String,
    val name: String? = null,
    val deliver: String? = null,
    val repeat: Int? = null,
)

data class CronTaskUiState(
    val jobs: List<CronJob> = emptyList(),
    val isLoading: Boolean = false,
    val isCreating: Boolean = false,
    val error: String? = null,
)

// ─── Channel Manager ────────────────────────────────────────────────────────

@Serializable
data class ChannelInfo(
    val id: String,
    val platform: String,
    val name: String? = null,
    @SerialName("thread_id") val threadId: String? = null,
    @SerialName("is_home") val isHome: Boolean = false,
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

@Serializable
data class ChannelListResponse(
    val channels: List<ChannelInfo> = emptyList(),
)

data class ChannelManagerUiState(
    val channels: List<ChannelInfo> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
