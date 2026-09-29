package com.example.data.model

data class LiveStreamInfo(
    val videoId: String = "",
    val title: String = "",
    val channelTitle: String = "",
    val channelId: String = "",
    val thumbnailUrl: String = "",
    val isLive: Boolean = false,
    val viewerCount: Long = 0,
    val likeCount: Long = 0,
    val chatMessageCount: Long = 0,
    val startedAt: Long = 0,
    val activeLiveChatId: String = "",
    val streamQuality: String = "",
    val streamStatus: String = ""
)
