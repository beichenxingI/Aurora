package com.beichenxing.aurora.presentation.character

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.core.utils.AvatarStorage
import com.beichenxing.aurora.core.utils.CharacterCardParser
import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.repository.CharacterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CharacterListUiState(
    val isLoading: Boolean = false,
    val importError: String? = null,
    val importSuccess: String? = null,
    val selectedCharacterId: String? = null,  // drives ModalBottomSheet
    val showDeleteConfirm: Boolean = false
)

@HiltViewModel
class CharacterListViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val characters: StateFlow<List<Character>> = characterRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _uiState = MutableStateFlow(CharacterListUiState())
    val uiState: StateFlow<CharacterListUiState> = _uiState.asStateFlow()

    // ─── Sheet ────────────────────────────────────────────────────────────────

    fun selectCharacter(id: String) {
        _uiState.update { it.copy(selectedCharacterId = id, showDeleteConfirm = false) }
    }

    fun dismissSheet() {
        _uiState.update { it.copy(selectedCharacterId = null, showDeleteConfirm = false) }
    }

    // ─── Pin ──────────────────────────────────────────────────────────────────

    fun togglePinned(characterId: String, currentValue: Boolean) {
        viewModelScope.launch {
            characterRepository.setPinned(characterId, !currentValue)
        }
    }

    // ─── Favorite ─────────────────────────────────────────────────────────────

    fun toggleFavorite(characterId: String, currentValue: Boolean) {
        viewModelScope.launch {
            characterRepository.setFavorite(characterId, !currentValue)
        }
    }

    // ─── Delete ───────────────────────────────────────────────────────────────

    fun requestDelete() {
        _uiState.update { it.copy(showDeleteConfirm = true) }
    }

    fun cancelDelete() {
        _uiState.update { it.copy(showDeleteConfirm = false) }
    }

    fun confirmDelete() {
        val id = _uiState.value.selectedCharacterId ?: return
        viewModelScope.launch {
            try {
                AvatarStorage.delete(context, id)
                characterRepository.deleteById(id)
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete character $id")
            } finally {
                dismissSheet()
            }
        }
    }

    // ─── Import ───────────────────────────────────────────────────────────────

    fun importFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, importError = null) }

            var successCount = 0
            var failCount = 0

            for (uri in uris) {
                try {
                    val character = CharacterCardParser.parse(context, uri)
                    if (character != null) {
                        val avatarPath = tryExtractAvatar(uri, character.id)
                        val withAvatar = if (avatarPath != null) {
                            character.copy(avatarPath = avatarPath)
                        } else {
                            character
                        }
                        characterRepository.insert(withAvatar)
                        successCount++
                    } else {
                        failCount++
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Failed to import from URI: $uri")
                    failCount++
                }
            }

            val message = when {
                successCount > 0 && failCount == 0 -> "成功导入 $successCount 个角色"
                successCount > 0 && failCount > 0  -> "导入 $successCount 个成功，$failCount 个失败"
                else -> null
            }
            val error = if (failCount > 0 && successCount == 0) "导入失败，请检查文件格式" else null

            _uiState.update {
                it.copy(isLoading = false, importSuccess = message, importError = error)
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(importError = null, importSuccess = null) }
    }

    // ─── Private ──────────────────────────────────────────────────────────────

    private fun tryExtractAvatar(uri: Uri, characterId: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            if (bytes.size > 8 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte()
            ) {
                AvatarStorage.saveFromBytes(context, bytes, characterId)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}