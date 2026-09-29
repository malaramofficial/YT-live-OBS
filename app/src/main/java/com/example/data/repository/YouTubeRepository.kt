package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.LiveStreamInfo
import com.example.data.model.LivePoll
import com.example.data.model.PollOption
import com.example.data.local.SecureTokenStore
import com.example.data.remote.SendChatMessageRequest
import com.example.data.remote.SendChatMessageSnippet
import com.example.data.remote.TextMessageDetails
import com.example.data.remote.YouTubeApiService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

sealed class LiveConnectionState {
    object Disconnected : LiveConnectionState()
    object Checking : LiveConnectionState()
    data class Active(val streamInfo: LiveStreamInfo) : LiveConnectionState()
    data class NoActiveLive(val message: String = "No active YouTube Live stream found") : LiveConnectionState()
    data class Error(val errorMsg: String) : LiveConnectionState()
}

class YouTubeRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("malaram_live_prefs", Context.MODE_PRIVATE)
    private val secureTokenStore = SecureTokenStore(context)

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.NONE
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/youtube/v3/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val apiService = retrofit.create(YouTubeApiService::class.java)

    private val _connectionState = MutableStateFlow<LiveConnectionState>(LiveConnectionState.Disconnected)
    val connectionState: StateFlow<LiveConnectionState> = _connectionState.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAccountConnected = MutableStateFlow(sharedPrefs.getBoolean("is_connected", false))
    val isAccountConnected: StateFlow<Boolean> = _isAccountConnected.asStateFlow()

    private val _channelTitle = MutableStateFlow(sharedPrefs.getString("channel_title", "") ?: "")
    val channelTitle: StateFlow<String> = _channelTitle.asStateFlow()

    private val _channelId = MutableStateFlow(sharedPrefs.getString("channel_id", "") ?: "")
    val channelId: StateFlow<String> = _channelId.asStateFlow()

    private val _oauthToken = MutableStateFlow(loadOAuthToken())
    val oauthToken: StateFlow<String> = _oauthToken.asStateFlow()

    private var chatPollingJob: Job? = null
    private var nextPageToken: String? = null

    private val _activePoll = MutableStateFlow<LivePoll?>(null)
    val activePoll: StateFlow<LivePoll?> = _activePoll.asStateFlow()

    private fun loadOAuthToken(): String {
        val secure = secureTokenStore.get()
        if (secure.isNotBlank()) return secure
        val legacy = sharedPrefs.getString("oauth_token", "") ?: ""
        if (legacy.isNotBlank()) {
            secureTokenStore.save(legacy)
            sharedPrefs.edit().remove("oauth_token").apply()
        }
        return legacy
    }

    init {
        if (_isAccountConnected.value) {
            checkActiveLiveStream()
        }
    }

    fun saveCredentials(channelName: String, channelIdVal: String, tokenVal: String) {
        _channelTitle.value = channelName
        _channelId.value = channelIdVal
        _oauthToken.value = tokenVal
        secureTokenStore.save(tokenVal)
        sharedPrefs.edit()
            .putString("channel_title", channelName)
            .putString("channel_id", channelIdVal)
            .remove("oauth_token")
            .apply()
    }

    fun connectYouTube(scope: CoroutineScope, channelName: String = "Malaram Official", channelIdVal: String = "") {
        _isAccountConnected.value = true
        sharedPrefs.edit().putBoolean("is_connected", true).apply()
        if (channelName.isNotBlank()) _channelTitle.value = channelName
        if (channelIdVal.isNotBlank()) _channelId.value = channelIdVal
        checkActiveLiveStream(scope)
    }

    fun setConnectionError(message: String) {
        _isAccountConnected.value = false
        sharedPrefs.edit().putBoolean("is_connected", false).apply()
        _connectionState.value = LiveConnectionState.Error(message)
    }

    fun disconnectYouTube() {
        stopChatPolling()
        _activePoll.value = null
        _isAccountConnected.value = false
        _connectionState.value = LiveConnectionState.Disconnected
        _chatMessages.value = emptyList()
        _oauthToken.value = ""
        secureTokenStore.clear()
        sharedPrefs.edit()
            .putBoolean("is_connected", false)
            .remove("oauth_token")
            .apply()
    }

    fun checkActiveLiveStream(scope: CoroutineScope? = null) {
        val launchScope = scope ?: CoroutineScope(Dispatchers.IO)
        launchScope.launch {
            _connectionState.value = LiveConnectionState.Checking
            val token = _oauthToken.value.trim()
            if (token.isBlank()) {
                _connectionState.value = LiveConnectionState.Error("YouTube is not connected. Tap Connect YouTube and authorize your Google account.")
                return@launch
            }
            try {
                val response = apiService.getMyLiveBroadcasts("Bearer $token")
                if (response.isSuccessful) {
                    val videoId = response.body()?.items?.firstOrNull {\n                        it.status?.lifeCycleStatus?.equals("live", ignoreCase = true) == true\n                    }?.id
                    if (!videoId.isNullOrEmpty()) {
                        fetchVideoDetails(videoId, token)
                    } else {
                        _connectionState.value = LiveConnectionState.NoActiveLive("Your YouTube account is connected, but no active Live broadcast was found.")
                    }
                } else if (response.code() == 401 || response.code() == 403) {
                    setConnectionError("YouTube authorization expired or was denied (${response.code()}). Reconnect YouTube.")
                } else {
                    _connectionState.value = LiveConnectionState.Error("YouTube API error (${response.code()}). ${response.message()}")
                }
            } catch (e: Exception) {
                _connectionState.value = LiveConnectionState.Error("Unable to reach YouTube: ${e.message ?: e.javaClass.simpleName}")
            }
        }
    }

    suspend fun checkVideoById(videoId: String, scope: CoroutineScope) {
        _connectionState.value = LiveConnectionState.Checking
        val token = _oauthToken.value.trim()
        if (token.isBlank()) {
            _connectionState.value = LiveConnectionState.Error(
                "YouTube authentication required. Connect Google / YouTube first."
            )
            return
        }
        try {
            fetchVideoDetails(videoId, token)
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Error checking video ID", e)
            _connectionState.value = LiveConnectionState.Error(
                "YouTube API error: " + (e.message ?: "unknown error")
            )
        }
    }

    private suspend fun fetchVideoDetails(videoId: String, bearerToken: String) {
        val response = apiService.getVideoDetails(bearerToken = "Bearer $bearerToken", videoId = videoId)
        if (!response.isSuccessful) {
            if (response.code() == 401 || response.code() == 403) {
                setConnectionError("YouTube authorization expired or was denied (${response.code()}). Reconnect YouTube.")
            } else {
                _connectionState.value = LiveConnectionState.Error("YouTube video lookup failed (${response.code()}). ${response.message()}")
            }
            return
        }
        val video = response.body()?.items?.firstOrNull()
        if (video == null) {
            _connectionState.value = LiveConnectionState.NoActiveLive("YouTube did not return the active broadcast.")
            return
        }
        val details = video.liveStreamingDetails
        val streamInfo = LiveStreamInfo(
            videoId = videoId,
            title = video.snippet?.title ?: "",
            channelTitle = video.snippet?.channelTitle ?: "",
            channelId = video.snippet?.channelId ?: "",
            thumbnailUrl = video.snippet?.thumbnails?.high?.url ?: video.snippet?.thumbnails?.medium?.url ?: "",
            isLive = details?.actualStartTime != null,
            viewerCount = details?.concurrentViewers?.toLongOrNull() ?: 0L,
            likeCount = video.statistics?.likeCount?.toLongOrNull() ?: 0L,
            startedAt = System.currentTimeMillis(),
            activeLiveChatId = details?.activeLiveChatId ?: ""
        )
        if (!streamInfo.isLive) {
            _connectionState.value = LiveConnectionState.NoActiveLive("The selected YouTube broadcast is not live right now.")
            return
        }
        _connectionState.value = LiveConnectionState.Active(streamInfo)
        if (streamInfo.activeLiveChatId.isNotEmpty()) startRealChatPolling(streamInfo.activeLiveChatId, bearerToken)
    }

    private fun startRealChatPolling(liveChatId: String, bearerToken: String) {
        chatPollingJob?.cancel()
        nextPageToken = null
        chatPollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getLiveChatMessages(
                        bearerToken = "Bearer $bearerToken",
                        liveChatId = liveChatId,
                        pageToken = nextPageToken
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        nextPageToken = body?.nextPageToken
                        syncActivePoll(body?.activePollItem)
                        val newItems = body?.items ?: emptyList()
                        val parsed = newItems.map { item ->
                            val snippet = item.snippet
                            val author = item.authorDetails
                            val isSuperChat = snippet?.superChatDetails != null
                            val amount = snippet?.superChatDetails?.amountDisplayString
                            val text = if (isSuperChat) {
                                snippet?.superChatDetails?.userComment ?: ""
                            } else {
                                snippet?.textMessageDetails?.messageText
                                    ?: snippet?.displayMessage ?: ""
                            }
                            ChatMessage(
                                id = item.id ?: "msg_${System.currentTimeMillis()}",
                                authorName = author?.displayName ?: "Viewer",
                                authorPhotoUrl = author?.profileImageUrl ?: "",
                                authorChannelId = author?.channelId ?: "",
                                messageText = text,
                                timestamp = System.currentTimeMillis(),
                                isSuperChat = isSuperChat,
                                superChatAmount = amount,
                                isModerator = author?.isChatModerator == true,
                                isMember = author?.isChatSponsor == true
                            )
                        }
                        if (parsed.isNotEmpty()) {
                            _chatMessages.value = (parsed + _chatMessages.value).take(150)
                        }
                        val delayMs = (body?.pollingIntervalMillis ?: 4000L).coerceAtLeast(1000L)
                        delay(delayMs)
                    } else if (response.code() == 401 || response.code() == 403) {
                        setConnectionError("YouTube chat authorization expired or was denied (${response.code()}). Reconnect YouTube.")
                        break
                    } else {
                        delay(5000L)
                    }
                } catch (e: Exception) {
                    Log.e("YouTubeRepo", "Chat polling error", e)
                    delay(6000L)
                }
            }
        }
    }

    private fun syncActivePoll(item: com.example.data.remote.YouTubeLiveChatMessageItem?) {
        val details = item?.snippet?.pollDetails
        val metadata = details?.metadata
        if (item?.id.isNullOrBlank() || metadata?.questionText.isNullOrBlank()) {
            if (details == null) _activePoll.value = null
            return
        }
        val options = metadata.options.orEmpty().mapIndexed { index, option ->
            PollOption(
                id = index + 1,
                text = option.optionText.orEmpty(),
                votes = option.tally?.toIntOrNull() ?: 0
            )
        }
        _activePoll.value = LivePoll(
            id = item.id!!,
            question = metadata.questionText!!,
            options = options,
            isActive = metadata.status?.equals("active", ignoreCase = true) == true,
            showOnOverlay = true
        )
    }

    suspend fun createYouTubePoll(
        question: String,
        options: List<String>
    ): Result<String> {
        val token = _oauthToken.value.trim()
        val chatId = (_connectionState.value as? LiveConnectionState.Active)
            ?.streamInfo?.activeLiveChatId

        if (token.isBlank()) return Result.failure(IllegalStateException("YouTube authorization required"))
        if (chatId.isNullOrBlank()) return Result.failure(IllegalStateException("Active live chat not available"))

        val cleanOptions = options.map { it.trim() }.filter { it.isNotBlank() }.take(4)
        if (question.isBlank() || cleanOptions.size !in 2..4) {
            return Result.failure(IllegalArgumentException("Poll needs 2 to 4 non-empty options"))
        }

        return try {
            val response = apiService.insertLivePoll(
                bearerToken = "Bearer $token",
                request = com.example.data.remote.CreateLivePollRequest(
                    snippet = com.example.data.remote.CreateLivePollSnippet(
                        liveChatId = chatId,
                        pollDetails = com.example.data.remote.PollDetails(
                            metadata = com.example.data.remote.PollMetadata(
                                questionText = question.trim(),
                                options = cleanOptions.map { com.example.data.remote.PollOptionRequest(it) }
                            )
                        )
                    )
                )
            )
            if (response.isSuccessful) {
                Result.success(response.body()?.id ?: "")
            } else {
                Result.failure(IllegalStateException("YouTube poll failed: HTTP ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun closeYouTubePoll(messageId: String): Result<Unit> {
        val token = _oauthToken.value.trim()
        if (token.isBlank()) return Result.failure(IllegalStateException("YouTube authorization required"))
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("Poll message ID missing"))

        return try {
            val response = apiService.closeLivePoll(
                bearerToken = "Bearer $token",
                messageId = messageId
            )
            if (response.isSuccessful) {
                _activePoll.value = null
                Result.success(Unit)
            } else Result.failure(IllegalStateException("YouTube poll close failed: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopChatPolling() {
        chatPollingJob?.cancel()
        chatPollingJob = null
    }

    fun highlightMessage(messageId: String) {
        _chatMessages.value = _chatMessages.value.map {
            if (it.id == messageId) it.copy(isHighlighted = !it.isHighlighted) else it
        }
    }

    fun markMessageAddedToQna(messageId: String) {
        _chatMessages.value = _chatMessages.value.map {
            if (it.id == messageId) it.copy(isAddedToQna = true) else it
        }
    }

    suspend fun deleteMessage(messageId: String): Result<Unit> {
        val token = _oauthToken.value.trim()
        if (token.isBlank()) return Result.failure(IllegalStateException("YouTube authorization required"))
        if (messageId.isBlank()) return Result.failure(IllegalArgumentException("Message ID missing"))

        return try {
            val response = apiService.deleteChatMessage("Bearer $token", messageId)
            if (response.isSuccessful) {
                _chatMessages.value = _chatMessages.value.map {
                    if (it.id == messageId) it.copy(isDeleted = true, messageText = "[टिप्पणी हटा दी गई]") else it
                }
                Result.success(Unit)
            } else if (response.code() == 401 || response.code() == 403) {
                setConnectionError("YouTube authorization expired or was denied (HTTP " + response.code() + "). Reconnect YouTube.")
                Result.failure(IllegalStateException("YouTube authorization expired or was denied"))
            } else {
                Result.failure(IllegalStateException("YouTube delete failed: HTTP " + response.code()))
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Failed to delete message via YouTube API", e)
            Result.failure(e)
        }
    }

    suspend fun sendReplyMessage(text: String): Result<Unit> {
        val activeStream = (_connectionState.value as? LiveConnectionState.Active)?.streamInfo
            ?: return Result.failure(IllegalStateException("No active YouTube Live stream"))
        val token = _oauthToken.value.trim()
        val chatId = activeStream.activeLiveChatId
        if (token.isBlank()) return Result.failure(IllegalStateException("YouTube authorization required"))
        if (chatId.isBlank()) return Result.failure(IllegalStateException("Active live chat not available"))
        if (text.isBlank()) return Result.failure(IllegalArgumentException("Message cannot be blank"))

        return try {
            val response = apiService.postChatMessage(
                bearerToken = "Bearer " + token,
                request = SendChatMessageRequest(
                    snippet = SendChatMessageSnippet(
                        liveChatId = chatId,
                        textMessageDetails = TextMessageDetails(messageText = text)
                    )
                )
            )
            if (response.isSuccessful) Result.success(Unit)
            else if (response.code() == 401 || response.code() == 403) {
                setConnectionError("YouTube authorization expired or was denied (HTTP " + response.code() + "). Reconnect YouTube.")
                Result.failure(IllegalStateException("YouTube authorization expired or was denied"))
            } else {
                Result.failure(IllegalStateException("YouTube message failed: HTTP " + response.code()))
            }
        } catch (e: Exception) {
            Log.e("YouTubeRepo", "Failed to post message via YouTube API", e)
            Result.failure(e)
        }
    }
}
