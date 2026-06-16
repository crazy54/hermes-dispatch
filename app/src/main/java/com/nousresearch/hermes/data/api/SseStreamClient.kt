package com.nousresearch.hermes.data.api

import android.util.Log
import com.nousresearch.hermes.data.model.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "SseClient"

/**
 * SSE streaming client for the Hermes gateway /api/sessions/{id}/chat/stream endpoint.
 *
 * Real gateway event format:
 *   event: assistant.delta
 *   data: {"delta":"Hi","session_id":"...","message_id":"...","seq":3,"ts":...}
 *
 *   event: assistant.completed
 *   data: {"content":"Hi there!","session_id":"...","message_id":"...","completed":true,...}
 *
 *   event: run.started
 *   data: {"session_id":"...","run_id":"...","seq":1,...}
 *
 *   event: run.completed
 *   data: {"session_id":"...","completed":true,...}
 *
 *   event: tool.progress
 *   data: {"tool_name":"...","delta":"...","session_id":"...",...}
 *
 *   event: run.error
 *   data: {"error":"...","session_id":"...",...}
 */
@Singleton
class SseStreamClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun stream(url: String, token: String, body: String): Flow<StreamEvent> = callbackFlow {
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(body.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE opened — HTTP ${response.code}")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,       // This is the SSE event: field e.g. "assistant.delta"
                data: String,
            ) {
                if (data.isBlank() || data == "[DONE]") return
                Log.v(TAG, "SSE event=$type data=$data")

                val parsed = runCatching {
                    json.parseToJsonElement(data).jsonObject
                }.getOrNull() ?: return

                val event = mapGatewayEvent(type, parsed) ?: return
                trySend(event)

                // Close after run.completed or run.error
                if (type == "run.completed" || type == "run.error") {
                    channel.close()
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE closed")
                channel.close()
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: Response?,
            ) {
                val code = response?.code
                val msg  = t?.message ?: "unknown error"
                Log.e(TAG, "SSE failure HTTP=$code err=$msg")
                if (t != null) channel.close(t)
                else channel.close(Exception("Stream failed (HTTP $code)"))
            }
        }

        val source = EventSources.createFactory(okHttpClient).newEventSource(request, listener)
        awaitClose { source.cancel() }
    }

    // ── Gateway event → StreamEvent mapper ───────────────────────────────────

    private fun mapGatewayEvent(type: String?, data: JsonObject): StreamEvent? {
        val sessionId = data.str("session_id") ?: ""

        return when (type) {
            "run.started" -> StreamEvent.SessionInfo(
                sessionId = sessionId,
                title     = null,
            )

            "assistant.delta" -> {
                val delta = data.str("delta") ?: return null
                StreamEvent.TextDelta(delta = delta, sessionId = sessionId)
            }

            "assistant.completed" -> {
                val content = data.str("content") ?: ""
                StreamEvent.TextDone(sessionId = sessionId, content = content)
            }

            "tool.started", "tool.progress" -> {
                val name   = data.str("tool_name") ?: data.str("name") ?: "tool"
                val callId = data.str("call_id") ?: data.str("tool_call_id") ?: name
                StreamEvent.ToolStart(name = name, callId = callId, sessionId = sessionId)
            }

            "tool.completed", "tool.done" -> {
                val callId  = data.str("call_id") ?: data.str("tool_call_id") ?: ""
                val success = data["success"]?.jsonPrimitive?.contentOrNull != "false"
                StreamEvent.ToolDone(callId = callId, sessionId = sessionId, success = success)
            }

            "approval.required" -> {
                // Best-effort parse — approval requests may not always come via streaming
                null
            }

            "run.completed" -> StreamEvent.Done(sessionId = sessionId)

            "run.error" -> {
                val msg = data.str("error") ?: data.str("message") ?: "Unknown stream error"
                StreamEvent.Error(message = msg)
            }

            // Ignore heartbeats, unknown events
            else -> {
                Log.d(TAG, "Ignoring SSE event type=$type")
                null
            }
        }
    }

    private fun JsonObject.str(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
