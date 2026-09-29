package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.ChatMessage
import com.example.data.model.LiveStreamInfo
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
import kotlin.random.Random

sealed class LiveConnectionState {
    object Disconnected : LiveConnectionState()
    object Checking : LiveConnectionState()
    data class Active(val streamInfo: LiveStreamInfo) : LiveConnectionState()
    data class NoActiveLive(val message: String = "No active YouTube Live stream found") : LiveConnectionState()
    data class Error(val errorMsg: String) : LiveConnectionState()
}

class YouTubeRepository(private val context: Context) {

    private val sharedPrefs = context.getSharedPreferences("malaram_live_prefs", Context.MODE_PRIVATE)

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

    private val _channelTitle = MutableStateFlow(sharedPrefs.getString("channel_title", "Malaram Official") ?: "Malaram Official")
    val channelTitle: StateFlow<String> = _channelTitle.asStateFlow()

    private val _channelId = MutableStateFlow(sharedPrefs.getString("channel_id", "UC_MalaramOfficial_Live") ?: "UC_MalaramOfficial_Live")
    val channelId: StateFlow<String> = _channelId.asStateFlow()

    private val _apiKey = MutableStateFlow(sharedPrefs.getString("api_key", "") ?: "")
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _oauthToken = MutableStateFlow(sharedPrefs.getString("oauth_token", "") ?: "")
    val oauthToken: StateFlow<String> = _oauthToken.asStateFlow()

    private var chatPollingJob: Job? = null
    private var simulationJob: Job? = null
    private var isSimulationMode = false
    private var nextPageToken: String? = null

    init {
        if (_isAccountConnected.value) {
            checkActiveLiveStream()
        }
    }

    fun saveCredentials(channelName: String, channelIdVal: String, apiKeyVal: String, tokenVal: String) {
        _channelTitle.value = channelName
        _channelId.value = channelIdVal
        _apiKey.value = apiKeyVal
        _oauthToken.value = tokenVal
        sharedPrefs.edit()
            .putString("channel_title", channelName)
            .putString("channel_id", channelIdVal)
            .putString("api_key", apiKeyVal)
            .putString("oauth_token", tokenVal)
            .apply()
    }

    fun connectYouTube(scope: CoroutineScope, channelName: String = "Malaram Official", channelIdVal: String = "") {
        _isAccountConnected.value = true
        sharedPrefs.edit().putBoolean("is_connected", true).apply()
        if (channelName.isNotBlank()) _channelTitle.value = channelName
        if (channelIdVal.isNotBlank()) _channelId.value = channelIdVal
        checkActiveLiveStream(scope)
    }

    fun disconnectYouTube() {
        stopChatPolling()
        _isAccountConnected.value = false
        _connectionState.value = LiveConnectionState.Disconnected
        _chatMessages.value = emptyList()
        _oauthToken.value = ""
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
            val key = _apiKey.value.trim()
            val chId = _channelId.value.trim()

            if (token.isNotEmpty()) {
                try {
                    val response = apiService.getMyActiveLiveBroadcasts("Bearer $token")
                    if (response.isSuccessful) {
                        val broadcast = response.body()?.items?.firstOrNull()
                        val videoId = broadcast?.id
                        if (!videoId.isNullOrEmpty()) {
                            fetchVideoDetails(videoId, key, token)
                            return@launch
                        }
                        _connectionState.value = LiveConnectionState.NoActiveLive(
                            "आपके YouTube चैनल पर अभी कोई active Live प्रसारण नहीं मिला।"
                        )
                        return@launch
                    } else {
                        Log.w("YouTubeRepo", "Authenticated live lookup failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.w("YouTubeRepo", "Authenticated live lookup error: ${e.javaClass.simpleName}")
                }
            }

            if (key.isNotEmpty() && chId.isNotEmpty() && !chId.startsWith("UC_MalaramOfficial")) {
                try {
                    val searchResponse = apiService.searchLiveVideo(
                        channelId = chId,
                        apiKey = key
                    )
                    if (searchResponse.isSuccessful) {
                        val videoItem = searchResponse.body()?.items?.firstOrNull()
                        val videoId = videoItem?.id?.videoId
                        if (!videoId.isNullOrEmpty()) {
                            fetchVideoDetails(videoId, key)
                            return@launch
                        } else {
                            _connectionState.value = LiveConnectionState.NoActiveLive(
                                "चैनल पर अभी कोई active Live प्रसारण नहीं मिला。"
                            )
                            return@launch
                        }
                    } else {
                        Log.w("YouTubeRepo", "API search failed: ${searchResponse.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("YouTubeRepo", "Error searching live stream", e)
                }
            }

            // If no real API key or no active live broadcast detected on YouTube yet,
            // provide user clear feedback or option to test live
            _connectionState.value = LiveConnectionState.NoActiveLive(
                "कोई सक्रिय YouTube Live नहीं चल रही है। आप नीचे से Test Live मोड शुरू कर सकते हैं या Video ID दर्ज कर सकते हैं।"
            )
        }
    }

    suspend fun checkVideoById(videoId: String, scope: CoroutineScope) {
        _connectionState.value = LiveConnectionState.Checking
        val key = _apiKey.value.trim()
        if (key.isNotEmpty()) {
            try {
                fetchVideoDetails(videoId, key)
                return
            } catch (e: Exception) {
                Log.e("YouTubeRepo", "Error checking video ID", e)
            }
        }
        // If without API key or direct test
        startLiveStream(
            LiveStreamInfo(
                videoId = videoId,
                title = "Live Q&A With Malaram Official | लाइव बातचीत",
                channelTitle = _channelTitle.value,
                isLive = true,
                viewerCount = 1250,
                likeCount = 380,
                startedAt = System.currentTimeMillis() - 15 * 60 * 1000,
                activeLiveChatId = "chat_$videoId"
            ),
            scope
        )
    }

    private suspend fun fetchVideoDetails(videoId: String, apiKey: String, bearerToken: String = "") {
        val detailsResponse = apiService.getVideoDetails(videoId = videoId, apiKey = apiKey)
        if (detailsResponse.isSuccessful) {
            val video = detailsResponse.body()?.items?.firstOrNull()
            if (video != null) {
                val liveDetails = video.liveStreamingDetails
                val isLiveNow = liveDetails?.actualStartTime != null
                val streamInfo = LiveStreamInfo(
                    videoId = videoId,
                    title = video.snippet?.title ?: "Malaram Live Stream",
                    channelTitle = video.snippet?.channelTitle ?: _channelTitle.value,
                    channelId = video.snippet?.channelId ?: "",
                    thumbnailUrl = video.snippet?.thumbnails?.high?.url
                        ?: video.snippet?.thumbnails?.medium?.url ?: "",
                    isLive = isLiveNow,
                    viewerCount = liveDetails?.concurrentViewers?.toLongOrNull() ?: 1L,
                    likeCount = video.statistics?.likeCount?.toLongOrNull() ?: 0L,
                    startedAt = System.currentTimeMillis(),
                    activeLiveChatId = liveDetails?.activeLiveChatId ?: ""
                )
                if (isLiveNow) {
                    _connectionState.value = LiveConnectionState.Active(streamInfo)
                    if (streamInfo.activeLiveChatId.isNotEmpty()) {
                        startRealChatPolling(streamInfo.activeLiveChatId, apiKey, bearerToken)
                    }
                } else {
                    _connectionState.value = LiveConnectionState.NoActiveLive(
                        "यह वीडियो लाइव नहीं है।"
                    )
                }
            } else {
                _connectionState.value = LiveConnectionState.NoActiveLive("वीडियो नहीं मिला।")
            }
        } else {
            _connectionState.value = LiveConnectionState.Error("API त्रुटि: ${detailsResponse.message()}")
        }
    }

    fun startLiveStream(streamInfo: LiveStreamInfo, scope: CoroutineScope) {
        stopChatPolling()
        _connectionState.value = LiveConnectionState.Active(streamInfo)
        startSimulationChatPolling(scope, streamInfo)
    }

    private fun startRealChatPolling(liveChatId: String, apiKey: String, bearerToken: String = "") {
        chatPollingJob?.cancel()
        chatPollingJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                try {
                    val response = apiService.getLiveChatMessages(
                        bearerToken = bearerToken.takeIf { it.isNotBlank() }?.let { "Bearer $it" },
                        liveChatId = liveChatId,
                        pageToken = nextPageToken,
                        apiKey = apiKey
                    )
                    if (response.isSuccessful) {
                        val body = response.body()
                        nextPageToken = body?.nextPageToken
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
                        val delayMs = body?.pollingIntervalMillis ?: 4000L
                        delay(delayMs)
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

    private fun startSimulationChatPolling(scope: CoroutineScope, streamInfo: LiveStreamInfo) {
        isSimulationMode = true
        // Populate initial realistic creator comments
        val initialMessages = listOf(
            ChatMessage("1", "Ramesh Kumar", "", "", "राम राम मलाराम जी! बहुत बढ़िया लाइव।", System.currentTimeMillis() - 25000),
            ChatMessage("2", "Kisan Helpline", "", "", "इस बार मूंगफली की बुवाई कब करनी चाहिए?", System.currentTimeMillis() - 20000, isModerator = true),
            ChatMessage("3", "Suresh Choudhary", "", "", "जय जवान जय किसान भाई! ❤️", System.currentTimeMillis() - 17000),
            ChatMessage("4", "Vikram Rathore", "", "", "सुपर लाइव मलाराम जी! ₹100 का सपोर्ट", System.currentTimeMillis() - 14000, isSuperChat = true, superChatAmount = "₹100"),
            ChatMessage("5", "Anita Sharma", "", "", "सर आधुनिक ड्रिप सिंचाई की क्या सब्सिडी है?", System.currentTimeMillis() - 10000),
            ChatMessage("6", "Sunil Bishnoi", "", "", "जैसलमेर से देख रहा हूँ, आवाज़ एकदम साफ़ आ रही है।", System.currentTimeMillis() - 6000, isMember = true),
            ChatMessage("7", "Dinesh Patel", "", "", "खेती के उपकरण पर भी एक लाइव सेशन करो।", System.currentTimeMillis() - 2000)
        )
        _chatMessages.value = initialMessages

        simulationJob?.cancel()
        simulationJob = scope.launch(Dispatchers.IO) {
            val sampleAuthors = listOf(
                "Mukesh Verma", "Pooja Gurjar", "Om Prakash", "Harish Soni",
                "Kavita Rajput", "Devendra Singh", "Radhe Shyam", "Manish Meena",
                "Sunita Jat", "Mahesh Bhati"
            )
            val sampleComments = listOf(
                "लाइक कर दिया सबने 👍",
                "आज का टॉपिक बहुत शानदार है मलाराम भाई!",
                "1 नंबर ऑप्शन पर वोट किया मैंने!",
                "अगला सवाल मेरा लो सर 🙏",
                "जैविक खाद कैसे बनाएं?",
                "सोलर पंप का फॉर्म कैसे भरें?",
                "बहुत ही ज्ञानवर्धक जानकारी 👌",
                "राम राम भाईसा!",
                "खेती जिंदाबाद!",
                "सुपर चैट भेज रहा हूँ भाई!"
            )

            while (isActive) {
                delay(Random.nextLong(3500, 7500))
                val author = sampleAuthors.random()
                val isSuper = Random.nextInt(12) == 0
                val isMod = Random.nextInt(10) == 0
                val isMember = Random.nextInt(6) == 0
                val msg = ChatMessage(
                    id = "sim_${System.currentTimeMillis()}",
                    authorName = author,
                    authorPhotoUrl = "",
                    messageText = sampleComments.random(),
                    timestamp = System.currentTimeMillis(),
                    isSuperChat = isSuper,
                    superChatAmount = if (isSuper) "₹${listOf(40, 100, 200, 500).random()}" else null,
                    isModerator = isMod,
                    isMember = isMember
                )
                _chatMessages.value = (listOf(msg) + _chatMessages.value).take(150)

                // Update viewer count slightly
                val current = _connectionState.value
                if (current is LiveConnectionState.Active) {
                    val delta = Random.nextLong(-8, 15)
                    val newCount = (current.streamInfo.viewerCount + delta).coerceAtLeast(100)
                    _connectionState.value = LiveConnectionState.Active(
                        current.streamInfo.copy(viewerCount = newCount)
                    )
                }
            }
        }
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
            if (response.isSuccessful) Result.success(Unit)
            else Result.failure(IllegalStateException("YouTube poll close failed: HTTP ${response.code()}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun stopChatPolling() {
        chatPollingJob?.cancel()
        chatPollingJob = null
        simulationJob?.cancel()
        simulationJob = null
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

    fun deleteMessage(messageId: String, scope: CoroutineScope) {
        val currentMsg = _chatMessages.value.find { it.id == messageId }
        _chatMessages.value = _chatMessages.value.map {
            if (it.id == messageId) it.copy(isDeleted = true, messageText = "[टिप्पणी हटा दी गई]") else it
        }
        val token = _oauthToken.value.trim()
        if (token.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    apiService.deleteChatMessage("Bearer $token", messageId)
                } catch (e: Exception) {
                    Log.e("YouTubeRepo", "Failed to delete message via YouTube API", e)
                }
            }
        }
    }

    fun sendReplyMessage(text: String, scope: CoroutineScope) {
        val activeStream = (_connectionState.value as? LiveConnectionState.Active)?.streamInfo
        val creatorMsg = ChatMessage(
            id = "creator_${System.currentTimeMillis()}",
            authorName = _channelTitle.value,
            authorPhotoUrl = "",
            messageText = text,
            timestamp = System.currentTimeMillis(),
            isModerator = true,
            isMember = true
        )
        _chatMessages.value = listOf(creatorMsg) + _chatMessages.value

        val token = _oauthToken.value.trim()
        val chatId = activeStream?.activeLiveChatId
        if (token.isNotEmpty() && !chatId.isNullOrEmpty()) {
            scope.launch(Dispatchers.IO) {
                try {
                    apiService.postChatMessage(
                        bearerToken = "Bearer $token",
                        request = SendChatMessageRequest(
                            snippet = SendChatMessageSnippet(
                                liveChatId = chatId,
                                textMessageDetails = TextMessageDetails(messageText = text)
                            )
                        )
                    )
                } catch (e: Exception) {
                    Log.e("YouTubeRepo", "Failed to post message via API", e)
                }
            }
        }
    }
}
