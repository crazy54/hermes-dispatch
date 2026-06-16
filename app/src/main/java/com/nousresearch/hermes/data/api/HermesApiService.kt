package com.nousresearch.hermes.data.api

import com.nousresearch.hermes.data.model.ApprovalResponse
import com.nousresearch.hermes.data.model.CreateCronJobRequest
import com.nousresearch.hermes.data.model.CronJob
import com.nousresearch.hermes.data.model.CronJobListResponse
import com.nousresearch.hermes.data.model.PairRedeemRequest
import com.nousresearch.hermes.data.model.PairRedeemResponse
import com.nousresearch.hermes.data.model.PairingRequest
import com.nousresearch.hermes.data.model.PairingResponse
import com.nousresearch.hermes.data.model.SendMessageRequest
import com.nousresearch.hermes.data.model.SessionListResponse
import com.nousresearch.hermes.data.model.MessageListResponse
import com.nousresearch.hermes.data.model.CreateSessionResponse
import com.nousresearch.hermes.data.model.GatewayCapabilities
import com.nousresearch.hermes.data.model.ProfileListResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming
import retrofit2.http.Url

interface HermesApiService {

    /** Health check */
    @GET
    suspend fun health(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    /** Gateway capabilities — includes profile list */
    @GET
    suspend fun getCapabilities(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<GatewayCapabilities>

    /** List sessions — returns { object, data, limit, offset, has_more } */
    @GET
    suspend fun listSessions(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 50,
        @Query("profile") profile: String? = null,
    ): Response<SessionListResponse>

    /** Create a new session — returns { object, session: {...} } */
    @POST
    suspend fun createSession(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body body: Map<String, String>,
    ): Response<CreateSessionResponse>

    /** Get messages for a session — returns { object, session_id, data: [...] } */
    @GET
    suspend fun getSessionMessages(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 100,
    ): Response<MessageListResponse>

    /** Send a message to a session (non-streaming) */
    @POST
    suspend fun sendMessage(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: SendMessageRequest,
    ): Response<ResponseBody>

    /** Send a message to a session (streaming SSE) */
    @Streaming
    @POST
    suspend fun streamMessage(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: SendMessageRequest,
    ): Response<ResponseBody>

    /** Submit an approval decision */
    @POST
    suspend fun submitApproval(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body response: ApprovalResponse,
    ): Response<Unit>

    /** Pair a new device */
    @POST
    suspend fun pair(
        @Url url: String,
        @Body request: PairingRequest,
    ): Response<PairingResponse>

    /** Redeem pairing code for session token */
    @POST
    suspend fun redeemPairingCode(
        @Url url: String,
        @Body request: PairRedeemRequest,
    ): Response<PairRedeemResponse>

    /** List available profiles */
    @GET
    suspend fun listProfiles(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<ProfileListResponse>

    /** List cron jobs — returns { jobs: [...] } */
    @GET
    suspend fun listCronJobs(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<CronJobListResponse>

    /** Create a cron job */
    @POST
    suspend fun createCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: CreateCronJobRequest,
    ): Response<CronJob>

    /** Run a cron job immediately */
    @POST
    suspend fun runCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    /** Delete a cron job */
    @DELETE
    suspend fun deleteCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>
}
