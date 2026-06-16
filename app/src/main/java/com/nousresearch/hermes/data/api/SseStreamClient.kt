package com.nousresearch.hermes.data.api

import android.util.Log
import com.nousresearch.hermes.data.model.StreamEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.serialization.json.Json
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
 * Low-level SSE streaming client backed by OkHttp EventSource.
 * Emits [StreamEvent] objects parsed from JSON server-sent events.
 */
@Singleton
class SseStreamClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val json: Json,
) {
    fun stream(url: String, token: String, body: String): Flow<StreamEvent> = callbackFlow {
        val requestBody = body.toRequestBody("application/json; charset=utf-8".toMediaType())
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Accept", "text/event-stream")
            .addHeader("Cache-Control", "no-cache")
            .post(requestBody)
            .build()

        val listener = object : EventSourceListener() {
            override fun onOpen(eventSource: EventSource, response: Response) {
                Log.d(TAG, "SSE stream opened")
            }

            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String,
            ) {
                if (data == "[DONE]" || data.isBlank()) return
                runCatching {
                    // Events come in as: {"type":"text_delta","delta":"...",...}
                    // Parse the type discriminator then decode the full event.
                    json.decodeFromString<StreamEvent>(data)
                }.onSuccess { event ->
                    trySend(event)
                }.onFailure { err ->
                    Log.w(TAG, "Failed to parse SSE event: $data", err)
                }
            }

            override fun onClosed(eventSource: EventSource) {
                Log.d(TAG, "SSE stream closed")
                channel.close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                Log.e(TAG, "SSE stream failure: ${t?.message}")
                channel.close(t)
            }
        }

        val factory = EventSources.createFactory(okHttpClient)
        val source = factory.newEventSource(request, listener)

        awaitClose {
            source.cancel()
        }
    }
}
