package com.nousresearch.hermes.data.api

import com.nousresearch.hermes.data.model.ApprovalResponse
import com.nousresearch.hermes.data.model.PairRedeemRequest
import com.nousresearch.hermes.data.model.PairRedeemResponse
import com.nousresearch.hermes.data.model.PairingRequest
import com.nousresearch.hermes.data.model.PairingResponse
import com.nousresearch.hermes.data.model.SendMessageRequest
import com.nousresearch.hermes.data.model.SessionListResponse
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

    /** Health check — used to test connectivity and auth */
    @GET
    suspend fun health(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    /** Start streaming a message response */
    @Streaming
    @POST("api/v1/chat/stream")
    suspend fun streamMessage(
        @Body request: SendMessageRequest,
    ): Response<ResponseBody>

    /** List sessions */
    @GET
    suspend fun listSessions(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Query("limit") limit: Int = 50,
        @Query("profile") profile: String? = null,
    ): Response<SessionListResponse>

    /** Delete/reset a session */
    @POST
    suspend fun resetSession(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    /** Submit an approval decision */
    @POST
    suspend fun submitApproval(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body response: ApprovalResponse,
    ): Response<Unit>

    /** Pair a new device — gets a persistent token */
    @POST
    suspend fun pair(
        @Url url: String,
        @Body request: PairingRequest,
    ): Response<PairingResponse>

    /** Redeem a short-lived pairing code for a session token */
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
    ): Response<List<String>>

    @GET
    suspend fun listCronJobs(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<com.nousresearch.hermes.data.model.CronJobListResponse>

    @POST
    suspend fun createCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
        @Body request: com.nousresearch.hermes.data.model.CreateCronJobRequest,
    ): Response<com.nousresearch.hermes.data.model.CronJob>

    @POST
    suspend fun runCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    @DELETE
    suspend fun deleteCronJob(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<Unit>

    @GET
    suspend fun listChannels(
        @Url url: String,
        @Header("Authorization") authorization: String,
    ): Response<com.nousresearch.hermes.data.model.ChannelListResponse>
}
