package com.beichenxing.aurora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beichenxing.aurora.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_at ASC")
    fun observeByChat(chatId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_at ASC")
    suspend fun getByChat(chatId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :id")
    suspend fun getById(id: String): MessageEntity?

    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY created_at DESC LIMIT 1")
    suspend fun getLastByChat(chatId: String): MessageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity)

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM messages WHERE chat_id = :chatId")
    suspend fun deleteByChat(chatId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE chat_id = :chatId")
    suspend fun countByChat(chatId: String): Int

    @Query("""
        UPDATE messages 
        SET content = :content, 
            reasoning = :reasoning,
            swipes_json = :swipesJson,
            swipes_reasoning_json = :swipesReasoningJson,
            current_swipe_index = :currentSwipeIndex
        WHERE id = :id
    """)
    suspend fun updateContent(
        id: String,
        content: String,
        reasoning: String?,
        swipesJson: String,
        swipesReasoningJson: String,
        currentSwipeIndex: Int
    )
}