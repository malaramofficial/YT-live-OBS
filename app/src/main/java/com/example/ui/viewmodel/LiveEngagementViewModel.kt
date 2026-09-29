package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.OverlayWebServer
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
import org.json.JSONArray
import org.json.JSONObject

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
            id = "",
            question = "",
            options = emptyList(),
            isActive = false,
            showOnOverlay = true
        )
    )
    val currentPoll: StateFlow<LivePoll> = _currentPoll.asStateFlow()

    // Announcements
    private val _currentAnnouncement = MutableStateFlow<Announcement?>(null)
    val currentAnnouncement: StateFlow<Announcement?> = _currentAnnouncement.asStateFlow()

    private val _announcementsHistory = MutableStateFlow<List<Announcement>>(emptyList())
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

    private var overlayServer: OverlayWebServer? = null
    private val _overlayServerUrl = MutableStateFlow<String?>(null)
    val overlayServerUrl: StateFlow<String?> = _overlayServerUrl.asStateFlow()

    fun startOverlayServer(): Boolean {
        if (overlayServer?.isRunning() == true) return true
        val server = OverlayWebServer(port = 8080) { buildOverlayStateJson() }
        if (!server.start()) return false
        overlayServer = server
        _overlayServerUrl.value = server.localUrl()
        return _overlayServerUrl.value != null
    }

    fun stopOverlayServer() {
        overlayServer?.stop()
        overlayServer = null
        _overlayServerUrl.value = null
    }

    private fun buildOverlayStateJson(): String {
        val stream = (connectionState.value as? LiveConnectionState.Active)?.streamInfo
        val poll = currentPoll.value
        val root = JSONObject()
            .put("channel", channelTitle.value)
            .put("viewers", stream?.viewerCount ?: 0L)
            .put("live", stream?.isLive == true)
            .put("question", spotlightQuestion.value?.questionText ?: "")
            .put("announcement", currentAnnouncement.value?.text ?: "")
        val pollArray = JSONArray()
        poll?.options?.forEach {
            pollArray.put(JSONObject().put("text", it.text).put("votes", it.votes))
        }
        root.put("poll", pollArray)
        return root.toString()
    }


    init {
        viewModelScope.launch {
            repository.activePoll.collect { remotePoll ->
                if (remotePoll != null) {
                    _currentPoll.value = remotePoll
                }
            }
        }

        // Real audience vote totals must come from YouTube; never fabricate them locally.
    }

    fun setTab(tab: ScreenTab) {
        _currentTab.value = tab
    }

    fun connectYouTube(channelName: String = "", channelId: String = "") {
        repository.connectYouTube(viewModelScope, channelName, channelId)
    }

    fun disconnectYouTube() {
        repository.disconnectYouTube()
    }

    fun setConnectionError(message: String) {
        repository.setConnectionError(message)
    }

    fun checkActiveLive() {
        repository.checkActiveLiveStream(viewModelScope)
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
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun sendChatReply(text: String) {
        if (text.isNotBlank()) {
            viewModelScope.launch {
                repository.sendReplyMessage(text)
            }
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
        viewModelScope.launch {
            repository.sendReplyMessage("$emoji Announcement: $text")
                .onSuccess {
                    val item = Announcement(
                        id = System.currentTimeMillis().toString(),
                        emoji = emoji,
                        text = text,
                        timestamp = System.currentTimeMillis(),
                        isActiveOnOverlay = true
                    )
                    _currentAnnouncement.value = item
                    _announcementsHistory.value = listOf(item) + _announcementsHistory.value.take(20)
                }
        }
    }

    fun clearAnnouncement() {
        _currentAnnouncement.value = null
    }

    // Overlay Actions
    fun updateOverlayTheme(theme: OverlayTheme) {
        _overlayConfig.value = _overlayConfig.value.copy(theme = theme)
    }

    fun togglePollOverlay(show: Boolean) {
        _currentPoll.value = _currentPoll.value.copy(showOnOverlay = show)
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

    fun saveChannelBranding(channelTitle: String) {
        repository.saveCredentials(
            channelName = channelTitle,
            channelIdVal = repository.channelId.value,
            apiKeyVal = repository.apiKey.value,
            tokenVal = repository.oauthToken.value
        )
        _overlayConfig.value = _overlayConfig.value.copy(channelName = channelTitle)
    }
}
