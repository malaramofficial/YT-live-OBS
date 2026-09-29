package com.example.data.model

data class ChatMessage(
    val id: String,
    val authorName: String,
    val authorPhotoUrl: String = "",
    val authorChannelId: String = "",
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isSuperChat: Boolean = false,
    val superChatAmount: String? = null,
    val isModerator: Boolean = false,
    val isMember: Boolean = false,
    val isHighlighted: Boolean = false,
    val isAddedToQna: Boolean = false,
    val isDeleted: Boolean = false
)
