package com.beichenxing.aurora.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.domain.repository.CharacterRepository
import com.beichenxing.aurora.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class ChatListItem(
    val chatId: String,
    val characterId: String,
    val characterName: String,
    val characterAvatarPath: String?,
    val lastMessagePreview: String,
    val updatedAt: Long
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val chatItems = chatRepository
        .observeAllChats()
        .map { chats ->
            val items = mutableListOf<ChatListItem>()
            for (chat in chats) {
                val character = characterRepository.getById(chat.characterId)
                val lastMsg = chatRepository.getLastMessage(chat.id)
                items.add(
                    ChatListItem(
                        chatId = chat.id,
                        characterId = chat.characterId,
                        characterName = character?.name ?: "未知角色",
                        characterAvatarPath = character?.avatarPath,
                        lastMessagePreview = lastMsg?.content?.take(60) ?: "",
                        updatedAt = chat.updatedAt
                    )
                )
            }
            items
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
}