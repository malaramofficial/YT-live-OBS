package com.example.data.model

data class Announcement(
    val id: String = System.currentTimeMillis().toString(),
    val emoji: String = "📢",
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isActiveOnOverlay: Boolean = true
)
