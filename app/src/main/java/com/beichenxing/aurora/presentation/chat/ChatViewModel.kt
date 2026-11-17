package com.beichenxing.aurora.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.model.Chat
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.MessageRole
import com.beichenxing.aurora.domain.repository.CharacterRepository
import com.beichenxing.aurora.domain.repository.ChatRepository
import com.beichenxing.aurora.domain.repository.LlmConfigRepository
import com.beichenxing.aurora.domain.service.LlmService
import com.beichenxing.aurora.domain.service.PromptAssembler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

data class ChatUiState(
    val character        : Character?    = null,
    val messages         : List<Message> = emptyList(),
    val isGenerating     : Boolean       = false,
    val isWebViewReady   : Boolean       = false,
    val error            : String?       = null,
    // Message action sheet
    val actionTarget     : Message?      = null,
    // Edit dialog
    val editingMessage   : Message?      = null,
    // Delete confirm dialog
    val deleteTarget     : Message?      = null
)

sealed class ChatEvent {
    data class SendToWebView(val type: String, val payloadJson: String) : ChatEvent()
    data object ScrollToBottom : ChatEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val characterRepository : CharacterRepository,
    private val chatRepository      : ChatRepository,
    private val llmConfigRepository : LlmConfigRepository,
    private val llmService          : LlmService,
    private val promptAssembler     : PromptAssembler,
    savedStateHandle                : SavedStateHandle
) : ViewModel() {

    private val characterId : String = checkNotNull(savedStateHandle["characterId"])
    private val chatIdArg   : String = savedStateHandle["chatId"] ?: "new"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var currentChatId      : String? = null
    private var generationJob      : Job?    = null
    private var pendingCharacter   : Character? = null

    init { loadCharacterAndChat() }

    // ─── Load ─────────────────────────────────────────────────────────────

    private fun loadCharacterAndChat() {
        viewModelScope.launch {
            try {
                val character = characterRepository.getById(characterId) ?: return@launch
                _uiState.update { it.copy(character = character) }

                when {
                    chatIdArg != "new" -> {
                        val chat = chatRepository.getById(chatIdArg)
                        if (chat != null) {
                            currentChatId = chat.id
                            val messages = chatRepository.getMessages(chat.id)
                            _uiState.update { it.copy(messages = messages) }
                            if (_uiState.value.isWebViewReady) {
                                pushMessagesToWebView(messages)
                            }
                        }
                    }
                    else -> {
                        pendingCharacter = character
                        if (character.firstMessage.isNotBlank()) {
                            val preview = Message(
                                id        = "preview_first_msg",
                                chatId    = "",
                                role      = MessageRole.ASSISTANT,
                                content   = character.firstMessage,
                                swipes    = listOf(character.firstMessage),
                                createdAt = System.currentTimeMillis()
                            )
                            _uiState.update { it.copy(messages = listOf(preview)) }
                            if (_uiState.value.isWebViewReady) {
                                pushMessagesToWebView(listOf(preview))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load character/chat")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ─── WebView Ready ────────────────────────────────────────────────────

    fun onWebViewReady() {
        _uiState.update { it.copy(isWebViewReady = true) }
        viewModelScope.launch { pushMessagesToWebView(_uiState.value.messages) }
    }

    // ─── User sends a message ─────────────────────────────────────────────

    fun onUserInput(text: String) {
        if (text.isBlank() || _uiState.value.isGenerating) return

        viewModelScope.launch {
            try {
                if (currentChatId == null) {
                    val character = pendingCharacter
                        ?: characterRepository.getById(characterId)
                        ?: return@launch
                    val now  = System.currentTimeMillis()
                    val chat = Chat(
                        id          = UUID.randomUUID().toString(),
                        characterId = character.id,
                        title       = character.name,
                        createdAt   = now,
                        updatedAt   = now
                    )
                    chatRepository.insert(chat)
                    currentChatId = chat.id

                    if (character.firstMessage.isNotBlank()) {
                        val firstMsg = Message(
                            id        = UUID.randomUUID().toString(),
                            chatId    = chat.id,
                            role      = MessageRole.ASSISTANT,
                            content   = character.firstMessage,
                            swipes    = listOf(character.firstMessage),
                            createdAt = now - 1
                        )
                        chatRepository.insertMessage(firstMsg)
                    }
                }

                val chatId = currentChatId ?: return@launch
                val now    = System.currentTimeMillis()
                val userMsg = Message(
                    id        = UUID.randomUUID().toString(),
                    chatId    = chatId,
                    role      = MessageRole.USER,
                    content   = text,
                    swipes    = listOf(text),
                    createdAt = now
                )
                chatRepository.insertMessage(userMsg)

                val updated = chatRepository.getMessages(chatId)
                _uiState.update { it.copy(messages = updated) }
                pushMessagesToWebView(updated)
                generateResponse(chatId, updated)

            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ─── Message long-press → action sheet ───────────────────────────────

    fun onMessageLongPress(id: String) {
        // Ignore preview message
        if (id == "preview_first_msg") return
        val message = _uiState.value.messages.find { it.id == id } ?: return
        _uiState.update { it.copy(actionTarget = message) }
    }

    fun dismissActionSheet() {
        _uiState.update { it.copy(actionTarget = null) }
    }

    // ─── Retry ────────────────────────────────────────────────────────────

    /**
     * Retry from a given message:
     *
     * If target is ASSISTANT:
     *   Delete target + all messages after it.
     *   Re-generate based on remaining history (which ends with the user message).
     *
     * If target is USER:
     *   Delete all messages AFTER target (target itself is kept).
     *   Re-generate based on history up to and including target.
     */
    fun retryFromMessage(messageId: String) {
        val chatId = currentChatId ?: return
        val messages = _uiState.value.messages
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0) return
        val target = messages[targetIndex]

        // Cancel any ongoing generation
        generationJob?.cancel()
        generationJob = null

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(actionTarget = null, isGenerating = false) }

                when (target.role) {
                    MessageRole.ASSISTANT -> {
                        // Delete target and everything after
                        val toDelete = messages.subList(targetIndex, messages.size)
                        toDelete.forEach { chatRepository.deleteMessage(it.id) }

                        // Notify WebView to remove all deleted bubbles
                        toDelete.forEach { msg ->
                            _events.emit(ChatEvent.SendToWebView(
                                "removeMessage",
                                """{"id":"${msg.id}"}"""
                            ))
                        }

                        // Reload messages from DB
                        val remaining = chatRepository.getMessages(chatId)
                        _uiState.update { it.copy(messages = remaining) }

                        // Generate from remaining context
                        if (remaining.isNotEmpty()) {
                            generateResponse(chatId, remaining)
                        }
                    }

                    MessageRole.USER -> {
                        // Delete everything AFTER target
                        val toDelete = messages.subList(targetIndex + 1, messages.size)
                        toDelete.forEach { chatRepository.deleteMessage(it.id) }

                        toDelete.forEach { msg ->
                            _events.emit(ChatEvent.SendToWebView(
                                "removeMessage",
                                """{"id":"${msg.id}"}"""
                            ))
                        }

                        val remaining = chatRepository.getMessages(chatId)
                        _uiState.update { it.copy(messages = remaining) }

                        // Generate using history up to and including target
                        generateResponse(chatId, remaining)
                    }

                    MessageRole.SYSTEM -> {
                        // System messages: not retryable
                        Timber.w("Retry on system message ignored")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Retry failed")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ─── Edit ─────────────────────────────────────────────────────────────

    fun beginEditMessage(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        _uiState.update { it.copy(actionTarget = null, editingMessage = message) }
    }

    fun confirmEditMessage(newContent: String) {
        val message = _uiState.value.editingMessage ?: return
        if (newContent.isBlank()) {
            _uiState.update { it.copy(error = "内容不能为空") }
            return
        }

        viewModelScope.launch {
            try {
                val updated = message.copy(
                    content            = newContent.trim(),
                    swipes             = listOf(newContent.trim()),
                    currentSwipeIndex  = 0
                )
                chatRepository.updateMessage(updated)

                val newMessages = _uiState.value.messages.map {
                    if (it.id == message.id) updated else it
                }
                _uiState.update { it.copy(messages = newMessages, editingMessage = null) }

                // Tell WebView to update this bubble in-place
                _events.emit(ChatEvent.SendToWebView(
                    "updateMessage",
                    buildUpdateMessagePayload(updated)
                ))
            } catch (e: Exception) {
                Timber.e(e, "Edit failed")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun cancelEditMessage() {
        _uiState.update { it.copy(editingMessage = null) }
    }

    // ─── Delete ───────────────────────────────────────────────────────────

    fun requestDeleteMessage(messageId: String) {
        val message = _uiState.value.messages.find { it.id == messageId } ?: return
        _uiState.update { it.copy(actionTarget = null, deleteTarget = message) }
    }

    fun confirmDeleteMessage() {
        val target = _uiState.value.deleteTarget ?: return

        viewModelScope.launch {
            try {
                chatRepository.deleteMessage(target.id)

                val newMessages = _uiState.value.messages.filter { it.id != target.id }
                _uiState.update { it.copy(messages = newMessages, deleteTarget = null) }

                _events.emit(ChatEvent.SendToWebView(
                    "removeMessage",
                    """{"id":"${target.id}"}"""
                ))
            } catch (e: Exception) {
                Timber.e(e, "Delete failed")
                _uiState.update { it.copy(error = e.message, deleteTarget = null) }
            }
        }
    }

    fun cancelDeleteMessage() {
        _uiState.update { it.copy(deleteTarget = null) }
    }

    // ─── Copy (clipboard handled in ChatScreen) ───────────────────────────

    // No ViewModel logic needed; ChatScreen calls ClipboardManager directly.

    // ─── Stop generation ──────────────────────────────────────────────────

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(isGenerating = false) }
        viewModelScope.launch {
            _events.emit(ChatEvent.SendToWebView(
                "setGenerating",
                """{"generating":false}"""
            ))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
    }

    // ─── LLM streaming generation ─────────────────────────────────────────

    private fun generateResponse(chatId: String, messages: List<Message>) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            val config = llmConfigRepository.getActive()
            if (config == null) {
                _uiState.update { it.copy(error = "请先在设置中添加 LLM 配置") }
                return@launch
            }

            val character = _uiState.value.character
            val plan      = promptAssembler.assemble(character, messages)
            val now          = System.currentTimeMillis()
            val assistantId  = UUID.randomUUID().toString()

            val placeholder = Message(
                id        = assistantId,
                chatId    = chatId,
                role      = MessageRole.ASSISTANT,
                content   = "",
                swipes    = listOf(""),
                createdAt = now
            )
            chatRepository.insertMessage(placeholder)

            _uiState.update { state ->
                state.copy(
                    messages     = state.messages + placeholder,
                    isGenerating = true
                )
            }

            _events.emit(ChatEvent.SendToWebView(
                "setGenerating",
                """{"generating":true,"id":"$assistantId"}"""
            ))

            var fullContent   = ""
            var fullReasoning = ""

            try {
                llmService.streamChat(config, plan).collect { chunk ->
                    when {
                        chunk.isDone -> Unit

                        chunk.isReasoningChunk && chunk.reasoning != null -> {
                            fullReasoning += chunk.reasoning
                            _events.emit(ChatEvent.SendToWebView(
                                "appendToken",
                                buildTokenPayload(assistantId, chunk.reasoning, true)
                            ))
                        }

                        !chunk.isReasoningChunk && chunk.content != null -> {
                            fullContent += chunk.content
                            _events.emit(ChatEvent.SendToWebView(
                                "appendToken",
                                buildTokenPayload(assistantId, chunk.content, false)
                            ))
                        }
                    }
                }

                val finalMsg = placeholder.copy(
                    content   = fullContent,
                    reasoning = fullReasoning.ifBlank { null },
                    swipes    = listOf(fullContent)
                )
                chatRepository.updateMessage(finalMsg)

                val finalMessages = chatRepository.getMessages(chatId)
                _uiState.update { it.copy(messages = finalMessages, isGenerating = false) }

                _events.emit(ChatEvent.SendToWebView(
                    "streamEnd",
                    """{"id":"$assistantId"}"""
                ))
                _events.emit(ChatEvent.SendToWebView(
                    "setGenerating",
                    """{"generating":false}"""
                ))

            } catch (e: Exception) {
                Timber.e(e, "Generation failed")
                val errorMsg = placeholder.copy(content = "[生成失败: ${e.message}]")
                chatRepository.updateMessage(errorMsg)

                _uiState.update { state ->
                    val msgs = state.messages.map {
                        if (it.id == assistantId) errorMsg else it
                    }
                    state.copy(messages = msgs, isGenerating = false, error = e.message)
                }

                _events.emit(ChatEvent.SendToWebView(
                    "streamError",
                    """{"id":"$assistantId","error":"${escapeJson(e.message ?: "Unknown")}"}"""
                ))
                _events.emit(ChatEvent.SendToWebView(
                    "setGenerating",
                    """{"generating":false}"""
                ))
            }
        }
    }

    // ─── WebView push helpers ─────────────────────────────────────────────

    private suspend fun pushMessagesToWebView(messages: List<Message>) {
        _events.emit(ChatEvent.SendToWebView("setMessages", buildSetMessagesPayload(messages)))
        _events.emit(ChatEvent.ScrollToBottom)
    }

    private fun buildSetMessagesPayload(messages: List<Message>): String {
        val sb = StringBuilder()
        sb.append("""{"messages":[""")
        messages.forEachIndexed { index, msg ->
            sb.append("""{"id":"${msg.id}",""")
            sb.append(""""role":"${msg.role.value}",""")
            sb.append(""""content":"${escapeJson(msg.currentContent)}"""")
            val reasoning = msg.currentReasoning
            if (!reasoning.isNullOrBlank()) {
                sb.append(""","reasoning":"${escapeJson(reasoning)}"""")
            }
            sb.append("}")
            if (index < messages.size - 1) sb.append(",")
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun buildUpdateMessagePayload(message: Message): String {
        val reasoning = message.currentReasoning
        return if (!reasoning.isNullOrBlank()) {
            """{"id":"${message.id}","content":"${escapeJson(message.currentContent)}","reasoning":"${escapeJson(reasoning)}"}"""
        } else {
            """{"id":"${message.id}","content":"${escapeJson(message.currentContent)}"}"""
        }
    }

    private fun buildTokenPayload(id: String, token: String, isReasoning: Boolean): String =
        """{"id":"$id","token":"${escapeJson(token)}","isReasoning":$isReasoning}"""

    private fun escapeJson(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}