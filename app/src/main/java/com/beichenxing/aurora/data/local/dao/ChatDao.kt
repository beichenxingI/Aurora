package com.beichenxing.aurora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beichenxing.aurora.data.local.entity.ChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats WHERE character_id = :characterId ORDER BY updated_at DESC")
    fun observeByCharacter(characterId: String): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE character_id = :characterId ORDER BY updated_at DESC")
    suspend fun getByCharacter(characterId: String): List<ChatEntity>

    @Query("SELECT * FROM chats ORDER BY updated_at DESC LIMIT :limit")
    suspend fun getRecent(limit: Int): List<ChatEntity>

    @Query("SELECT * FROM chats WHERE id = :id")
    suspend fun getById(id: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(chat: ChatEntity)

    @Update
    suspend fun update(chat: ChatEntity)

    @Delete
    suspend fun delete(chat: ChatEntity)

    @Query("DELETE FROM chats WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM chats WHERE character_id = :characterId")
    suspend fun deleteByCharacter(characterId: String)

    @Query("UPDATE chats SET updated_at = :now WHERE id = :id")
    suspend fun touchUpdatedAt(id: String, now: Long)

    @Query("UPDATE chats SET title = :title, updated_at = :now WHERE id = :id")
    suspend fun updateTitle(id: String, title: String, now: Long)

    @Query("SELECT COUNT(*) FROM chats WHERE character_id = :characterId")
    suspend fun countByCharacter(characterId: String): Int
}