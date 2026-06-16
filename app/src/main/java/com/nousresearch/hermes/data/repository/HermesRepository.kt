package com.nousresearch.hermes.data.repository

import android.util.Log
import com.nousresearch.hermes.data.api.HermesApiService
import com.nousresearch.hermes.data.api.SseStreamClient
import com.nousresearch.hermes.data.local.MessageDao
import com.nousresearch.hermes.data.local.MessageEntity
import com.nousresearch.hermes.data.local.PreferencesManager
import com.nousresearch.hermes.data.local.SessionDao
import com.nousresearch.hermes.data.local.SessionEntity
import com.nousresearch.hermes.data.model.ApprovalResponse
import com.nousresearch.hermes.data.model.ChannelInfo
import com.nousresearch.hermes.data.model.ChatMessage
import com.nousresearch.hermes.data.model.ConnectionConfig
import com.nousresearch.hermes.data.model.CreateCronJobRequest
import com.nousresearch.hermes.data.model.PairRedeemRequest
import com.nousresearch.hermes.data.model.PairRedeemResponse
import com.nousresearch.hermes.data.model.CronJob
import com.nousresearch.hermes.data.model.HermesSession
import com.nousresearch.hermes.data.model.MessageRole
import com.nousresearch.hermes.data.model.SendMessageRequest
import com.nousresearch.hermes.data.model.StreamEvent
import com.nousresearch.hermes.data.model.ToolStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
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

    suspend fun saveConnection(config: ConnectionConfig) {
        prefsManager.saveConnectionConfig(config)
    }

    suspend fun clearConnection() {
        prefsManager.clearConnectionConfig()
    }

    suspend fun testConnection(config: ConnectionConfig): Result<Unit> = runCatching {
        val response = apiService.health(apiUrl(config, "health"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}: ${response.message()}")
    }

    fun observeMessages(sessionId: String): Flow<List<ChatMessage>> =
        messageDao.observeMessages(sessionId).map { entities ->
            entities.map { it.toChatMessage() }
        }

    fun observeSessions(): Flow<List<HermesSession>> =
        sessionDao.observeSessions().map { entities ->
            entities.map { it.toHermesSession() }
        }

    fun streamMessage(
        gatewayUrl: String,
        token: String,
        message: String,
        sessionId: String?,
        profile: String,
    ): Flow<StreamEvent> {
        val request = SendMessageRequest(
            message = message,
            sessionId = sessionId,
            profile = profile,
            stream = true,
        )
        val bodyJson = json.encodeToString(SendMessageRequest.serializer(), request)
        val streamUrl = gatewayUrl.trimEnd('/') + "/api/v1/chat/stream"
        return sseClient.stream(streamUrl, token, bodyJson)
    }

    suspend fun saveMessage(message: ChatMessage) {
        messageDao.upsert(message.toEntity())
        sessionDao.upsert(
            SessionEntity(
                sessionId = message.sessionId,
                title = null,
                createdAt = message.timestamp,
                updatedAt = message.timestamp,
                messageCount = 0,
            )
        )
    }

    suspend fun updateMessage(message: ChatMessage) {
        messageDao.upsert(message.toEntity())
    }

    suspend fun upsertSession(session: HermesSession) {
        sessionDao.upsert(session.toEntity())
    }

    suspend fun submitApproval(config: ConnectionConfig, approvalId: String, approved: Boolean): Result<Unit> = runCatching {
        val response = apiService.submitApproval(
            apiUrl(config, "approvals"),
            auth(config),
            ApprovalResponse(approvalId, approved),
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    suspend fun refreshSessions(config: ConnectionConfig): Result<List<HermesSession>> = runCatching {
        val response = apiService.listSessions(
            apiUrl(config, "sessions"),
            auth(config),
            profile = config.profileName,
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        val sessions = response.body()?.sessions ?: emptyList()
        sessions.forEach { sessionDao.upsert(it.toEntity()) }
        sessions
    }

    suspend fun listProfiles(config: ConnectionConfig): Result<List<String>> = runCatching {
        val response = apiService.listProfiles(apiUrl(config, "profiles"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        (response.body() ?: listOf("default")).ifEmpty { listOf("default") }
    }

    suspend fun listCronJobs(config: ConnectionConfig): Result<List<CronJob>> = runCatching {
        val response = apiService.listCronJobs(apiUrl(config, "cron/jobs"), auth(config))
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
            apiUrl(config, "cron/jobs"),
            auth(config),
            CreateCronJobRequest(
                schedule = schedule,
                prompt = prompt,
                name = name?.takeIf { it.isNotBlank() },
                deliver = deliver?.takeIf { it.isNotBlank() },
            ),
        )
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body() ?: error("Empty response")
    }

    suspend fun runCronJob(config: ConnectionConfig, id: String): Result<Unit> = runCatching {
        val response = apiService.runCronJob(apiUrl(config, "cron/jobs/$id/run"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    suspend fun deleteCronJob(config: ConnectionConfig, id: String): Result<Unit> = runCatching {
        val response = apiService.deleteCronJob(apiUrl(config, "cron/jobs/$id"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
    }

    suspend fun listChannels(config: ConnectionConfig): Result<List<ChannelInfo>> = runCatching {
        val response = apiService.listChannels(apiUrl(config, "channels"), auth(config))
        if (!response.isSuccessful) error("HTTP ${response.code()}")
        response.body()?.channels ?: emptyList()
    }

    suspend fun redeemPairingCode(gatewayUrl: String, code: String): PairRedeemResponse {
        val url = gatewayUrl.trimEnd('/') + "/api/pair/redeem"
        val response = apiService.redeemPairingCode(url, PairRedeemRequest(code))
        if (!response.isSuccessful) error("Pairing failed: HTTP ${response.code()}")
        return response.body() ?: error("Empty response from gateway")
    }

    private fun apiUrl(config: ConnectionConfig, path: String): String =
        config.gatewayUrl.trimEnd('/') + "/api/v1/" + path.trimStart('/')

    private fun auth(config: ConnectionConfig): String = "Bearer ${config.token}"
}

// ── Mappers ──────────────────────────────────────────────────────────────────

private fun MessageEntity.toChatMessage() = ChatMessage(
    id = id,
    sessionId = sessionId,
    role = MessageRole.valueOf(role),
    content = content,
    timestamp = timestamp,
    toolName = toolName,
    toolStatus = toolStatus?.let { ToolStatus.valueOf(it) },
)

private fun ChatMessage.toEntity() = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    content = content,
    timestamp = timestamp,
    toolName = toolName,
    toolStatus = toolStatus?.name,
)

private fun SessionEntity.toHermesSession() = HermesSession(
    sessionId = sessionId,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
)

private fun HermesSession.toEntity() = SessionEntity(
    sessionId = sessionId,
    title = title,
    createdAt = createdAt,
    updatedAt = updatedAt,
    messageCount = messageCount,
)
