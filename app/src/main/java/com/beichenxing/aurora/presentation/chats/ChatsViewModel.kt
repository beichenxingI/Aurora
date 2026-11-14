package com.beichenxing.aurora.presentation.chats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.domain.repository.CharacterRepository
import com.beichenxing.aurora.domain.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class ChatListItem(
    val chatId: String,
    val characterId: String,
    val characterName: String,
    val characterAvatarPath: String?,
    val chatTitle: String,           // session title (editable)
    val lastMessagePreview: String,
    val updatedAt: Long
)

data class ChatsUiState(
    val menuTarget: ChatListItem? = null,     // long-pressed item (DropdownMenu anchor)
    val renameTarget: ChatListItem? = null,   // item being renamed
    val deleteTarget: ChatListItem? = null,   // item pending delete confirmation
    val renameError: String? = null
)

@HiltViewModel
class ChatsViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val characterRepository: CharacterRepository
) : ViewModel() {

    val chatItems = chatRepository
        .observeAllChats()
        .map { chats ->
            chats
                .filter { chat ->
                    // Only show chats that have at least one message
                    chatRepository.getLastMessage(chat.id) != null
                }
                .map { chat ->
                    val character = characterRepository.getById(chat.characterId)
                    val lastMsg = chatRepository.getLastMessage(chat.id)
                    ChatListItem(
                        chatId = chat.id,
                        characterId = chat.characterId,
                        characterName = character?.name ?: "未知角色",
                        characterAvatarPath = character?.avatarPath,
                        chatTitle = chat.title,
                        lastMessagePreview = lastMsg?.content?.take(60) ?: "",
                        updatedAt = chat.updatedAt
                    )
                }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(ChatsUiState())
    val uiState = _uiState.asStateFlow()

    // ─── Menu ─────────────────────────────────────────────────────────────────

    fun openMenu(item: ChatListItem) {
        _uiState.update { it.copy(menuTarget = item) }
    }

    fun closeMenu() {
        _uiState.update { it.copy(menuTarget = null) }
    }

    // ─── Rename ───────────────────────────────────────────────────────────────

    fun openRename(item: ChatListItem) {
        _uiState.update { it.copy(menuTarget = null, renameTarget = item, renameError = null) }
    }

    fun confirmRename(newTitle: String) {
        val target = _uiState.value.renameTarget ?: return
        if (newTitle.isBlank()) {
            _uiState.update { it.copy(renameError = "标题不能为空") }
            return
        }
        viewModelScope.launch {
            try {
                chatRepository.updateTitle(target.chatId, newTitle.trim())
                _uiState.update { it.copy(renameTarget = null, renameError = null) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to rename chat")
                _uiState.update { it.copy(renameError = "重命名失败") }
            }
        }
    }

    fun dismissRename() {
        _uiState.update { it.copy(renameTarget = null, renameError = null) }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    fun openDelete(item: ChatListItem) {
        _uiState.update { it.copy(menuTarget = null, deleteTarget = item) }
    }

    fun confirmDelete() {
        val target = _uiState.value.deleteTarget ?: return
        viewModelScope.launch {
            try {
                chatRepository.deleteById(target.chatId)
                // List updates automatically via Flow
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete chat")
            } finally {
                _uiState.update { it.copy(deleteTarget = null) }
            }
        }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(deleteTarget = null) }
    }
}