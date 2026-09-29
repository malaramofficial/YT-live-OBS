package com.example.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Query

interface YouTubeApiService {

    @GET("liveBroadcasts")
    suspend fun getMyActiveLiveBroadcasts(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "id,snippet,contentDetails,status",
        @Query("broadcastStatus") broadcastStatus: String = "active",
        @Query("mine") mine: Boolean = true
    ): Response<YouTubeLiveBroadcastListResponse>

    @POST("liveChat/messages")
    suspend fun insertLivePoll(
        @Header("Authorization") bearerToken: String,
        @Query("part") part: String = "snippet",
        @Body request: CreateLivePollRequest
    ): Response<YouTubeLiveChatMessageItem>

    @POST("liveChat/messages/transition")
    suspend fun closeLivePoll(
        @Header("Authorization") bearerToken: String,
        @Query("id") messageId: String,
        @Query("status") status: String = "closed",
        @Query("part") part: String = "snippet"
    ): Response<YouTubeLiveChatMessageItem>

    @GET("search")
    suspend fun searchLiveVideo(
        @Query("part") part: String = "snippet",
        @Query("channelId") channelId: String,
        @Query("eventType") eventType: String = "live",
        @Query("type") type: String = "video",
        @Query("key") apiKey: String
    ): Response<YouTubeSearchResponse>

    @GET("videos")
    suspend fun getVideoDetails(
        @Query("part") part: String = "snippet,liveStreamingDetails,statistics",
        @Header("Authorization") bearerToken: String? = null,
        @Query("id") videoId: String,
        @Query("key") apiKey: String
    ): Response<YouTubeVideoListResponse>

    @GET("liveChat/messages")
    suspend fun getLiveChatMessages(
        @Header("Authorization") bearerToken: String? = null,
        @Query("liveChatId") liveChatId: String,
        @Query("part") part: String = "snippet,authorDetails",
        @Query("pageToken") pageToken: String? = null,
        @Query("key") apiKey: String
    ): Response<YouTubeLiveChatMessagesResponse>

    @POST("liveChat/messages?part=snippet")
    suspend fun postChatMessage(
        @Header("Authorization") bearerToken: String,
        @Body request: SendChatMessageRequest
    ): Response<YouTubeLiveChatMessageItem>

    @DELETE("liveChat/messages")
    suspend fun deleteChatMessage(
        @Header("Authorization") bearerToken: String,
        @Query("id") messageId: String
    ): Response<Unit>
}
