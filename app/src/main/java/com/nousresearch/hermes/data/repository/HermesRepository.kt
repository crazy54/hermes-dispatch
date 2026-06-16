package com.nousresearch.hermes.data.repository

import android.util.Log
import com.nousresearch.hermes.data.api.HermesApiService
import com.nousresearch.hermes.data.api.SseStreamClient
import com.nousresearch.hermes.data.local.MessageDao
import com.nousresearch.hermes.data.local.MessageEntity
import com.nousresearch.hermes.data.local.PreferencesManager
import com.nousresearch.hermes.data.local.SessionDao
import com.nousresearch.hermes.data.local.SessionEntity
import com.nousresearch.hermes.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "HermesRepository"

@Singleton
class HermesRepository @Inject constructor(
    private val apiService: HermesApiService,
    private val sseClient: SseStreamClient,
    private val messageDao: MessageDao,
    private val sessionDao: SessionDao,
    private val prefsManager: PreferencesManager,
    private val json: Json,
) {
    val connectionConfig: Flow<ConnectionConfig?> = prefsManager.connectionConfigFlow

    suspend fun saveConnection(config: ConnectionConfig) = prefsManager.saveConnectionConfig(config)
    suspend fun clearConnection()                        = prefsManager.clearConnectionConfig()

    // ── Connectivity ──────────────────────────────────────────────────────────

    suspend fun testConnection(config: ConnectionConfig): Result<Unit> = runCatching {
        val response = apiService.health(url(config, "health"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}: ${response.message()}")
    }

    // ── Profiles ──────────────────────────────────────────────────────────────

    // ── Sessions ──────────────────────────────────────────────────────────────

    fun observeSessions(): Flow<List<HermesSession>> =
        sessionDao.observeSessions().map { it.map { e -> e.toHermesSession() } }

    suspend fun refreshSessions(config: ConnectionConfig): Result<List<HermesSession>> = runCatching {
        val response = apiService.listSessions(
            url(config, "sessions"),
            auth(config),
            limit = 50,
            profile = config.profileName.takeIf { it != "default" },
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}: ${response.message()}")
        val sessions = response.body()?.sessions ?: emptyList()
        sessions.forEach { sessionDao.upsert(it.toEntity()) }
        sessions
    }

    suspend fun createSession(config: ConnectionConfig): Result<HermesSession> = runCatching {
        val body = mapOf("profile" to config.profileName)
        val response = apiService.createSession(url(config, "sessions"), auth(config), body)
        if (!response.isSuccessful) error("HTTP ${response.code()}: ${response.message()}")
        val session = response.body()?.session?.toHermesSession()
            ?: error("Empty response from gateway")
        sessionDao.upsert(session.toEntity())
        session
    }

    // ── Messages ──────────────────────────────────────────────────────────────

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.observeMessages(sessionId).map { it.map { e -> e.toChatMessage() } }

    suspend fun loadSessionMessages(
        config: ConnectionConfig,
        sessionId: String,
    ): Result<List<ChatMessage>> = runCatching {
        val response = apiService.getSessionMessages(
            url(config, "sessions/$sessionId/messages"),
            auth(config),
            limit = 100,
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}: ${response.message()}")
        val messages = response.body()?.data?.map { it.toChatMessage() } ?: emptyList()
        messages.forEach { messageDao.upsert(it.toEntity()) }
        messages
    }

    suspend fun saveMessage(message: ChatMessage) {
        messageDao.upsert(message.toEntity())
        sessionDao.upsert(
            SessionEntity(
                sessionId    = message.sessionId,
                title        = null,
                createdAt    = message.timestamp,
                updatedAt    = message.timestamp,
                messageCount = 0,
            )
        )
    }

    suspend fun updateMessage(message: ChatMessage) = messageDao.upsert(message.toEntity())

    suspend fun upsertSession(session: HermesSession) = sessionDao.upsert(session.toEntity())

    // ── Streaming chat ────────────────────────────────────────────────────────

    /**
     * Stream a message to [sessionId] via SSE.
     * Uses POST /api/sessions/{id}/chat/stream
     */
    fun streamMessage(
        config: ConnectionConfig,
        sessionId: String,
        message: String,
    ): Flow<StreamEvent> {
        val streamUrl = url(config, "sessions/$sessionId/chat/stream")
        val request   = SendMessageRequest(message = message, stream = true)
        val bodyJson  = json.encodeToString(SendMessageRequest.serializer(), request)
        return sseClient.stream(streamUrl, config.token, bodyJson)
    }

    // ── Approvals ─────────────────────────────────────────────────────────────

    suspend fun submitApproval(
        config: ConnectionConfig,
        runId: String,
        approved: Boolean,
    ): Result<Unit> = runCatching {
        // Real endpoint: POST /v1/runs/{run_id}/approval
        val response = apiService.submitApproval(
            url(config, "../v1/runs/$runId/approval"),
            auth(config),
            ApprovalResponse(runId, approved),
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    // ── Cron jobs ─────────────────────────────────────────────────────────────

    suspend fun listCronJobs(config: ConnectionConfig): Result<List<CronJob>> = runCatching {
        val response = apiService.listCronJobs(url(config, "jobs"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body()?.jobs ?: emptyList()
    }

    suspend fun createCronJob(
        config: ConnectionConfig,
        schedule: String,
        prompt: String,
        name: String?,
        deliver: String?,
    ): Result<CronJob> = runCatching {
        val response = apiService.createCronJob(
            url(config, "jobs"),
            auth(config),
            CreateCronJobRequest(schedule = schedule, prompt = prompt,
                name = name?.takeIf { it.isNotBlank() },
                deliver = deliver?.takeIf { it.isNotBlank() }),
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: error("Empty response")
    }

    suspend fun runCronJob(config: ConnectionConfig, id: String): Result<Unit> = runCatching {
        val response = apiService.runCronJob(url(config, "jobs/$id/run"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    suspend fun deleteCronJob(config: ConnectionConfig, id: String): Result<Unit> = runCatching {
        val response = apiService.deleteCronJob(url(config, "jobs/$id"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    suspend fun listProfiles(config: ConnectionConfig): Result<List<String>> = runCatching {
        val response = apiService.listProfiles(url(config, "profiles"), auth(config))
        if (!response.isSuccessful) return@runCatching listOf("default")
        response.body()?.data?.takeIf { it.isNotEmpty() } ?: listOf("default")
    }

    /** Channels not yet exposed by gateway — returns empty list gracefully. */
    suspend fun listChannels(config: ConnectionConfig): Result<List<ChannelInfo>> =
        Result.success(emptyList())

    // ── Pairing ───────────────────────────────────────────────────────────────

    suspend fun redeemPairingCode(gatewayUrl: String, code: String): PairRedeemResponse {
        val url      = gatewayUrl.trimEnd('/') + "/api/pair/redeem"
        val response = apiService.redeemPairingCode(url, PairRedeemRequest(code))
        if (!response.isSuccessful) error("Pairing failed: HTTP ${response.code()}")
        return response.body() ?: error("Empty response from gateway")
    }

    // ── URL helpers ───────────────────────────────────────────────────────────

    /**
     * Build an absolute URL for [path] under /api/.
     * Use "../v1/foo" to escape to /v1/foo (e.g. capabilities, runs).
     */
    private fun url(config: ConnectionConfig, path: String): String {
        val base = config.gatewayUrl.trimEnd('/')
        return if (path.startsWith("../")) {
            "$base/${path.removePrefix("../")}"
        } else {
            "$base/api/${path.trimStart('/')}"
        }
    }

    private fun auth(config: ConnectionConfig) = "Bearer ${config.token}"
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun MessageEntity.toChatMessage() = ChatMessage(
    id        = id,
    sessionId = sessionId,
    role      = MessageRole.valueOf(role),
    content   = content,
    timestamp = timestamp,
    toolName  = toolName,
    toolStatus = toolStatus?.let { ToolStatus.valueOf(it) },
)

private fun ChatMessage.toEntity() = MessageEntity(
    id        = id,
    sessionId = sessionId,
    role      = role.name,
    content   = content,
    timestamp = timestamp,
    toolName  = toolName,
    toolStatus = toolStatus?.name,
)

private fun SessionEntity.toHermesSession() = HermesSession(
    sessionId    = sessionId,
    title        = title,
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    messageCount = messageCount,
)

private fun HermesSession.toEntity() = SessionEntity(
    sessionId    = sessionId,
    title        = title,
    createdAt    = createdAt,
    updatedAt    = updatedAt,
    messageCount = messageCount,
)
