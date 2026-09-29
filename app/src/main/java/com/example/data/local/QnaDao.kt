package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QnaDao {
    @Query("SELECT * FROM qna_questions ORDER BY isAnswered ASC, orderIndex ASC, timestamp ASC")
    fun getAllQuestions(): Flow<List<QnaEntity>>

    @Query("SELECT * FROM qna_questions WHERE isSpotlight = 1 LIMIT 1")
    fun getSpotlightQuestion(): Flow<QnaEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuestion(question: QnaEntity): Long

    @Update
    suspend fun updateQuestion(question: QnaEntity)

    @Query("UPDATE qna_questions SET isSpotlight = 0")
    suspend fun clearAllSpotlights()

    @Query("UPDATE qna_questions SET isSpotlight = 1 WHERE id = :id")
    suspend fun setSpotlight(id: Long)

    @Query("UPDATE qna_questions SET isAnswered = 1, isSpotlight = 0 WHERE id = :id")
    suspend fun markAnswered(id: Long)

    @Query("DELETE FROM qna_questions WHERE id = :id")
    suspend fun deleteQuestionById(id: Long)

    @Delete
    suspend fun deleteQuestion(question: QnaEntity)

    @Query("DELETE FROM qna_questions")
    suspend fun clearAll()
}
