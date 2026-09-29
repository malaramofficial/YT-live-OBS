package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "qna_questions")
data class QnaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val questionText: String,
    val authorName: String,
    val authorPhotoUrl: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isAnswered: Boolean = false,
    val isSpotlight: Boolean = false,
    val orderIndex: Int = 0
)
