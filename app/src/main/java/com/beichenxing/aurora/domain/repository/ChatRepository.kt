package com.beichenxing.aurora.domain.repository

import com.beichenxing.aurora.domain.model.Chat
import com.beichenxing.aurora.domain.model.Message
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeByCharacter(characterId: String): Flow<List<Chat>>
    suspend fun getByCharacter(characterId: String): List<Chat>
    suspend fun getRecent(limit: Int): List<Chat>
    suspend fun getById(id: String): Chat?
    suspend fun insert(chat: Chat)
    suspend fun update(chat: Chat)
    suspend fun deleteById(id: String)
    suspend fun updateTitle(id: String, title: String)

    fun observeMessages(chatId: String): Flow<List<Message>>
    suspend fun getMessages(chatId: String): List<Message>
    suspend fun getLastMessage(chatId: String): Message?
    suspend fun insertMessage(message: Message)
    suspend fun updateMessage(message: Message)
    suspend fun deleteMessage(id: String)
    suspend fun clearMessages(chatId: String)
    suspend fun messageCount(chatId: String): Int

    // Returns all chats across all characters, ordered by updated_at DESC
    fun observeAllChats(): Flow<List<Chat>>
}