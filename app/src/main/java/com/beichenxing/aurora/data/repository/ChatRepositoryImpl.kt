package com.beichenxing.aurora.data.repository

import com.beichenxing.aurora.data.local.dao.ChatDao
import com.beichenxing.aurora.data.local.dao.MessageDao
import com.beichenxing.aurora.data.local.entity.toDomain
import com.beichenxing.aurora.data.local.entity.toEntity
import com.beichenxing.aurora.domain.model.Chat
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ChatRepositoryImpl @Inject constructor(
    private val chatDao: ChatDao,
    private val messageDao: MessageDao
) : ChatRepository {

    override fun observeByCharacter(characterId: String): Flow<List<Chat>> =
        chatDao.observeByCharacter(characterId).map { list -> list.map { it.toDomain() } }

    override suspend fun getByCharacter(characterId: String): List<Chat> =
        chatDao.getByCharacter(characterId).map { it.toDomain() }

    override suspend fun getRecent(limit: Int): List<Chat> =
        chatDao.getRecent(limit).map { it.toDomain() }

    override suspend fun getById(id: String): Chat? =
        chatDao.getById(id)?.toDomain()

    override suspend fun insert(chat: Chat) {
        chatDao.insert(chat.toEntity())
    }

    override suspend fun update(chat: Chat) {
        chatDao.update(chat.toEntity())
        chatDao.touchUpdatedAt(chat.id, System.currentTimeMillis())
    }

    override suspend fun deleteById(id: String) {
        chatDao.deleteById(id) // messages deleted by CASCADE
    }

    override suspend fun updateTitle(id: String, title: String) =
        chatDao.updateTitle(id, title, System.currentTimeMillis())

    override fun observeMessages(chatId: String): Flow<List<Message>> =
        messageDao.observeByChat(chatId).map { list -> list.map { it.toDomain() } }

    override suspend fun getMessages(chatId: String): List<Message> =
        messageDao.getByChat(chatId).map { it.toDomain() }

    override suspend fun getLastMessage(chatId: String): Message? =
        messageDao.getLastByChat(chatId)?.toDomain()

    override suspend fun insertMessage(message: Message) {
        messageDao.insert(message.toEntity())
        chatDao.touchUpdatedAt(message.chatId, System.currentTimeMillis())
    }

    override suspend fun updateMessage(message: Message) =
        messageDao.update(message.toEntity())

    override suspend fun deleteMessage(id: String) =
        messageDao.deleteById(id)

    override suspend fun clearMessages(chatId: String) =
        messageDao.deleteByChat(chatId)

    override suspend fun messageCount(chatId: String): Int =
        messageDao.countByChat(chatId)

    override fun observeAllChats(): Flow<List<Chat>> =
        chatDao.observeAll().map { list -> list.map { it.toDomain() } }
}