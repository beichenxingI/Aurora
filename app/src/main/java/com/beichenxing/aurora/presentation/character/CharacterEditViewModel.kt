package com.beichenxing.aurora.presentation.character

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.core.utils.AvatarStorage
import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.repository.CharacterRepository
import com.beichenxing.aurora.presentation.navigation.NavRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

data class CharacterEditUiState(
    val isLoading : Boolean    = true,
    val isSaving  : Boolean    = false,
    val draft     : Character? = null,
    val isDirty   : Boolean    = false,   // any unsaved changes
    val error     : String?    = null,
    val saved     : Boolean    = false
)

@HiltViewModel
class CharacterEditViewModel @Inject constructor(
    private val characterRepository : CharacterRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: String =
        checkNotNull(savedStateHandle[NavRoutes.CharacterEdit.ARG_CHARACTER_ID])

    private val _uiState = MutableStateFlow(CharacterEditUiState())
    val uiState: StateFlow<CharacterEditUiState> = _uiState.asStateFlow()

    init { loadCharacter() }

    // ─── Load ─────────────────────────────────────────────────────────────

    private fun loadCharacter() {
        viewModelScope.launch {
            try {
                val character = characterRepository.getById(characterId)
                _uiState.update { it.copy(isLoading = false, draft = character) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load character for edit")
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    // ─── Draft field updates ───────────────────────────────────────────────

    private fun updateDraft(block: (Character) -> Character) {
        val current = _uiState.value.draft ?: return
        _uiState.update { it.copy(draft = block(current), isDirty = true) }
    }

    fun updateName(value: String)                    = updateDraft { it.copy(name = value) }
    fun updateDescription(value: String)             = updateDraft { it.copy(description = value) }
    fun updatePersonality(value: String)             = updateDraft { it.copy(personality = value) }
    fun updateScenario(value: String)                = updateDraft { it.copy(scenario = value) }
    fun updateFirstMessage(value: String)            = updateDraft { it.copy(firstMessage = value) }
    fun updateSystemPrompt(value: String)            = updateDraft { it.copy(systemPrompt = value) }
    fun updatePostHistoryInstructions(value: String) = updateDraft { it.copy(postHistoryInstructions = value) }
    fun updateCreatorNotes(value: String)            = updateDraft { it.copy(creatorNotes = value) }
    fun updateExampleDialogue(value: String)         = updateDraft { it.copy(exampleDialogue = value) }

    // ─── Avatar ───────────────────────────────────────────────────────────

    /**
     * Save avatar immediately on selection (overwrites existing file).
     * First-version behaviour: avatar is saved even if user cancels editing.
     */
    fun updateAvatarFromUri(uri: Uri) {
        viewModelScope.launch {
            val path = AvatarStorage.saveFromUri(context, uri, characterId)
            if (path != null) {
                updateDraft { it.copy(avatarPath = path) }
            } else {
                _uiState.update { it.copy(error = "头像保存失败") }
            }
        }
    }

    // ─── Save ─────────────────────────────────────────────────────────────

    fun save() {
        val draft = _uiState.value.draft ?: return
        if (draft.name.isBlank()) {
            _uiState.update { it.copy(error = "角色名不能为空") }
            return
        }
        if (_uiState.value.isSaving) return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            try {
                val updated = draft.copy(
                    name      = draft.name.trim(),
                    updatedAt = System.currentTimeMillis()
                )
                characterRepository.update(updated)
                _uiState.update { it.copy(isSaving = false, saved = true, isDirty = false) }
            } catch (e: Exception) {
                Timber.e(e, "Failed to save character")
                _uiState.update { it.copy(isSaving = false, error = "保存失败：${e.message}") }
            }
        }
    }

    fun consumeError() {
        _uiState.update { it.copy(error = null) }
    }
}