package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.QnaEntity
import com.example.data.model.Announcement
import com.example.data.model.ChatMessage
import com.example.data.model.LivePoll
import com.example.data.model.LiveStreamInfo
import com.example.data.model.OverlayConfig
import com.example.data.model.OverlayTheme
import com.example.data.model.PollOption
import com.example.data.repository.LiveConnectionState
import com.example.data.repository.YouTubeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ScreenTab(val title: String, val tabLabel: String) {
    LIVE("Malaram Live", "LIVE"),
    CHAT("Live Chat", "CHAT"),
    QNA("Q&A Questions", "Q&A"),
    POLL("Live Poll", "POLL"),
    OVERLAY("Overlay Preview", "OVERLAY")
}

class LiveEngagementViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val qnaDao = database.qnaDao()
    val repository = YouTubeRepository(application)

    val connectionState: StateFlow<LiveConnectionState> = repository.connectionState
    val chatMessages: StateFlow<List<ChatMessage>> = repository.chatMessages
    val isAccountConnected: StateFlow<Boolean> = repository.isAccountConnected
    val channelTitle: StateFlow<String> = repository.channelTitle
    val channelId: StateFlow<String> = repository.channelId
    val apiKey: StateFlow<String> = repository.apiKey
    val oauthToken: StateFlow<String> = repository.oauthToken

    private val _currentTab = MutableStateFlow(ScreenTab.LIVE)
    val currentTab: StateFlow<ScreenTab> = _currentTab.asStateFlow()

    // Room Q&A Questions
    val qnaList: StateFlow<List<QnaEntity>> = qnaDao.getAllQuestions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val spotlightQuestion: StateFlow<QnaEntity?> = qnaDao.getSpotlightQuestion()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Poll State
    private val _currentPoll = MutableStateFlow(
        LivePoll(
            id = "poll_default",
            question = "आज Live में क्या करें?",
            options = listOf(
                PollOption(1, "खेती (Agriculture)", 142),
                PollOption(2, "Gaming", 58),
                PollOption(3, "Comedy", 87),
                PollOption(4, "Technology", 112)
            ),
            isActive = true,
            showOnOverlay = true
        )
    )
    val currentPoll: StateFlow<LivePoll> = _currentPoll.asStateFlow()

    // Announcements
    private val _currentAnnouncement = MutableStateFlow<Announcement?>(
        Announcement(
            emoji = "❤️",
            text = "Welcome to Malaram Official Live Stream!",
            isActiveOnOverlay = true
        )
    )
    val currentAnnouncement: StateFlow<Announcement?> = _currentAnnouncement.asStateFlow()

    private val _announcementsHistory = MutableStateFlow<List<Announcement>>(
        listOf(
            Announcement(emoji = "❤️", text = "Welcome to Malaram Official Live Stream!"),
            Announcement(emoji = "👍", text = "Like कर दो सभी भाई लोग!"),
            Announcement(emoji = "🔔", text = "Subscribe करके घंटी दबा दो")
        )
    )
    val announcementsHistory: StateFlow<List<Announcement>> = _announcementsHistory.asStateFlow()

    // Overlay Configuration
    private val _overlayConfig = MutableStateFlow(
        OverlayConfig(
            showLiveBadge = true,
            showChannelTitle = true,
            showViewerCount = true,
            showCurrentQuestion = true,
            showPollResults = true,
            showAnnouncementTicker = true,
            theme = OverlayTheme.STUDIO_RED,
            channelName = repository.channelTitle.value
        )
    )
    val overlayConfig: StateFlow<OverlayConfig> = _overlayConfig.asStateFlow()

    init {
        // Seed default sample Q&A if database is empty
        viewModelScope.launch(Dispatchers.IO) {
            val count = qnaList.value.size
            if (count == 0) {
                val q1 = QnaEntity(
                    questionText = "मलाराम जी, सोलर पंप पर कितनी सब्सिडी मिल रही है?",
                    authorName = "Mukesh Bishnoi",
                    timestamp = System.currentTimeMillis() - 10 * 60 * 1000,
                    isSpotlight = true,
                    orderIndex = 1
                )
                val q2 = QnaEntity(
                    questionText = "इस मौसम में ड्रिप सिंचाई की क्या सावधानियां रखें?",
                    authorName = "Ramesh Kumar",
                    timestamp = System.currentTimeMillis() - 7 * 60 * 1000,
                    orderIndex = 2
                )
                val q3 = QnaEntity(
                    questionText = "अगला वीडियो किस विषय पर बना रहे हैं?",
                    authorName = "Sunita Meena",
                    timestamp = System.currentTimeMillis() - 4 * 60 * 1000,
                    orderIndex = 3
                )
                qnaDao.insertQuestion(q1)
                qnaDao.insertQuestion(q2)
                qnaDao.insertQuestion(q3)
            }
        }

        // Real audience vote totals must come from YouTube; never fabricate them locally.
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun connectYouTube(channelName: String = "Malaram Official", channelId: String = "") {
        repository.connectYouTube(viewModelScope, channelName, channelId)
    }

    fun disconnectYouTube() {
        repository.disconnectYouTube()
    }

    fun checkActiveLive() {
        repository.checkActiveLiveStream(viewModelScope)
    }

    fun startTestLiveMode() {
        val testStream = LiveStreamInfo(
            videoId = "demo_live_malaram",
            title = "🌾 राजस्थान में आधुनिक कृषि तकनीक और किसान चर्चा | Malaram Live",
            channelTitle = channelTitle.value,
            thumbnailUrl = "",
            isLive = true,
            viewerCount = 1420,
            likeCount = 538,
            chatMessageCount = 1240,
            startedAt = System.currentTimeMillis() - 22 * 60 * 1000,
            activeLiveChatId = "test_chat_id",
            streamQuality = "1080p60",
            streamStatus = "🔴 LIVE"
        )
        repository.startLiveStream(testStream, viewModelScope)
    }

    fun checkVideoById(videoId: String) {
        viewModelScope.launch {
            repository.checkVideoById(videoId, viewModelScope)
        }
    }

    // Chat actions
    fun highlightMessage(messageId: String) {
        repository.highlightMessage(messageId)
    }

    fun addChatToQna(message: ChatMessage) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.markMessageAddedToQna(message.id)
            val nextOrder = (qnaList.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            val entity = QnaEntity(
                questionText = message.messageText,
                authorName = message.authorName,
                authorPhotoUrl = message.authorPhotoUrl,
                timestamp = message.timestamp,
                orderIndex = nextOrder,
                isSpotlight = spotlightQuestion.value == null
            )
            qnaDao.insertQuestion(entity)
        }
    }

    fun deleteMessage(messageId: String) {
        repository.deleteMessage(messageId, viewModelScope)
    }

    fun sendChatReply(text: String) {
        if (text.isNotBlank()) {
            repository.sendReplyMessage(text, viewModelScope)
        }
    }

    // Q&A actions
    fun nextQuestion() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = spotlightQuestion.value
            if (current != null) {
                // Mark current as answered
                qnaDao.markAnswered(current.id)
            }
            // Find next unanswered question
            val unanswered = qnaList.value.filter { !it.isAnswered && it.id != current?.id }
            if (unanswered.isNotEmpty()) {
                val next = unanswered.first()
                qnaDao.clearAllSpotlights()
                qnaDao.setSpotlight(next.id)
            } else {
                qnaDao.clearAllSpotlights()
            }
        }
    }

    fun markCurrentQuestionAnswered() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = spotlightQuestion.value
            if (current != null) {
                qnaDao.markAnswered(current.id)
                // Automatically find next
                val remaining = qnaList.value.filter { !it.isAnswered && it.id != current.id }
                if (remaining.isNotEmpty()) {
                    qnaDao.setSpotlight(remaining.first().id)
                }
            }
        }
    }

    fun setQuestionSpotlight(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            qnaDao.clearAllSpotlights()
            qnaDao.setSpotlight(id)
        }
    }

    fun removeQuestion(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val wasSpotlight = spotlightQuestion.value?.id == id
            qnaDao.deleteQuestionById(id)
            if (wasSpotlight) {
                val remaining = qnaList.value.filter { !it.isAnswered && it.id != id }
                if (remaining.isNotEmpty()) {
                    qnaDao.setSpotlight(remaining.first().id)
                }
            }
        }
    }

    fun addCustomQuestion(questionText: String, authorName: String) {
        if (questionText.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val author = if (authorName.isBlank()) "Viewer" else authorName
            val nextOrder = (qnaList.value.maxOfOrNull { it.orderIndex } ?: 0) + 1
            val entity = QnaEntity(
                questionText = questionText,
                authorName = author,
                timestamp = System.currentTimeMillis(),
                orderIndex = nextOrder,
                isSpotlight = spotlightQuestion.value == null
            )
            qnaDao.insertQuestion(entity)
        }
    }

    // Poll Actions
    fun createNewPoll(question: String, optionsTexts: List<String>) {
        val validOptions = optionsTexts.map { it.trim() }.filter { it.isNotBlank() }.take(4)
        if (question.isBlank() || validOptions.size !in 2..4) return

        viewModelScope.launch {
            repository.createYouTubePoll(question.trim(), validOptions)
                .onSuccess { pollMessageId ->
                    _currentPoll.value = LivePoll(
                        id = pollMessageId.ifBlank { "poll_${System.currentTimeMillis()}" },
                        question = question.trim(),
                        options = validOptions.mapIndexed { index, text ->
                            PollOption(id = index + 1, text = text, votes = 0)
                        },
                        isActive = true,
                        showOnOverlay = true
                    )
                }
        }
    }

    fun votePollOption(optionId: Int, isAudience: Boolean = false) {
        // Local voting is only a preview interaction. Real audience votes are owned by YouTube.
        if (isAudience) return
        val poll = _currentPoll.value
        if (!poll.isActive) return
        val updatedOptions = poll.options.map {
            if (it.id == optionId) it.copy(votes = it.votes + 1) else it
        }
        _currentPoll.value = poll.copy(options = updatedOptions)
    }

    fun resetPollVotes() {
        // Do not invent or reset YouTube-owned audience totals.
        _currentPoll.value = _currentPoll.value.copy(
            options = _currentPoll.value.options.map { it.copy(votes = 0) }
        )
    }

    fun togglePollActive() {
        val poll = _currentPoll.value
        if (!poll.isActive) return
        viewModelScope.launch {
            repository.closeYouTubePoll(poll.id)
                .onSuccess { _currentPoll.value = poll.copy(isActive = false) }
        }
    }

    // Announcement Actions
    fun broadcastAnnouncement(emoji: String, text: String) {
        if (text.isBlank()) return
        val item = Announcement(
            id = System.currentTimeMillis().toString(),
            emoji = emoji,
            text = text,
            timestamp = System.currentTimeMillis(),
            isActiveOnOverlay = true
        )
        _currentAnnouncement.value = item
        _announcementsHistory.value = listOf(item) + _announcementsHistory.value.take(20)

        // Also post to chat so audience sees it
        repository.sendReplyMessage("$emoji Announcement: $text", viewModelScope)
    }

    fun clearAnnouncement() {
        _currentAnnouncement.value = null
    }

    // Overlay Actions
    fun updateOverlayTheme(theme: OverlayTheme) {
        _overlayConfig.value = _overlayConfig.value.copy(theme = theme)
    }

    fun toggleOverlayElement(element: String, enabled: Boolean) {
        val current = _overlayConfig.value
        _overlayConfig.value = when (element) {
            "liveBadge" -> current.copy(showLiveBadge = enabled)
            "channelTitle" -> current.copy(showChannelTitle = enabled)
            "viewerCount" -> current.copy(showViewerCount = enabled)
            "question" -> current.copy(showCurrentQuestion = enabled)
            "poll" -> current.copy(showPollResults = enabled)
            "announcement" -> current.copy(showAnnouncementTicker = enabled)
            else -> current
        }
    }

    fun setOAuthToken(token: String) {
        repository.saveCredentials(
            channelName = channelTitle.value,
            channelIdVal = channelId.value,
            apiKeyVal = apiKey.value,
            tokenVal = token
        )
    }

    fun saveSettings(channelTitle: String, channelId: String, apiKey: String, oauthToken: String) {
        repository.saveCredentials(channelTitle, channelId, apiKey, oauthToken)
        _overlayConfig.value = _overlayConfig.value.copy(channelName = channelTitle)
    }
}
