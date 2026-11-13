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
    val character: Character? = null,
    val messages: List<Message> = emptyList(),
    val isGenerating: Boolean = false,
    val isWebViewReady: Boolean = false,
    val error: String? = null
)

sealed class ChatEvent {
    data class SendToWebView(val type: String, val payloadJson: String) : ChatEvent()
    data object ScrollToBottom : ChatEvent()
}

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val characterRepository: CharacterRepository,
    private val chatRepository: ChatRepository,
    private val llmConfigRepository: LlmConfigRepository,
    private val llmService: LlmService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val characterId: String = checkNotNull(savedStateHandle["characterId"])
    private val chatIdArg: String = savedStateHandle["chatId"] ?: "new"

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var currentChatId: String? = null
    private var generationJob: Job? = null
    private var streamingMessageId: String? = null

    init {
        loadCharacterAndChat()
    }

    // ─── Load ─────────────────────────────────────────────────────────────────

    private fun loadCharacterAndChat() {
        viewModelScope.launch {
            try {
                val character = characterRepository.getById(characterId) ?: return@launch
                _uiState.update { it.copy(character = character) }

                val chat = when {
                    chatIdArg != "new" -> {
                        // Open specific chat by ID
                        chatRepository.getById(chatIdArg) ?: createNewChat(character)
                    }
                    else -> {
                        // "new" = always create a new chat
                        createNewChat(character)
                    }
                }
                currentChatId = chat.id

                val messages = chatRepository.getMessages(chat.id)
                _uiState.update { it.copy(messages = messages) }

                if (_uiState.value.isWebViewReady) {
                    pushMessagesToWebView(messages)
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load character/chat")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    private suspend fun createNewChat(character: Character): Chat {
        val now = System.currentTimeMillis()
        val chat = Chat(
            id = UUID.randomUUID().toString(),
            characterId = character.id,
            title = character.name,
            createdAt = now,
            updatedAt = now
        )
        chatRepository.insert(chat)

        if (character.firstMessage.isNotBlank()) {
            val firstMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chat.id,
                role = MessageRole.ASSISTANT,
                content = character.firstMessage,
                swipes = listOf(character.firstMessage),
                createdAt = now
            )
            chatRepository.insertMessage(firstMsg)
            _uiState.update { it.copy(messages = listOf(firstMsg)) }
        }

        return chat
    }

    // ─── WebView Ready ────────────────────────────────────────────────────────

    fun onWebViewReady() {
        Timber.i("[Chat] WebView ready")
        _uiState.update { it.copy(isWebViewReady = true) }
        viewModelScope.launch {
            pushMessagesToWebView(_uiState.value.messages)
        }
    }

    // ─── Push messages to WebView ─────────────────────────────────────────────

    private suspend fun pushMessagesToWebView(messages: List<Message>) {
        val payloadJson = buildSetMessagesPayload(messages)
        _events.emit(ChatEvent.SendToWebView("setMessages", payloadJson))
        _events.emit(ChatEvent.ScrollToBottom)
    }

    // ─── User sends a message ─────────────────────────────────────────────────

    fun onUserInput(text: String) {
        if (text.isBlank() || _uiState.value.isGenerating) return
        val chatId = currentChatId ?: return

        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val userMsg = Message(
                    id = UUID.randomUUID().toString(),
                    chatId = chatId,
                    role = MessageRole.USER,
                    content = text,
                    swipes = listOf(text),
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

    // ─── LLM streaming generation ─────────────────────────────────────────────

    private fun generateResponse(chatId: String, messages: List<Message>) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            val config = llmConfigRepository.getActive()
            if (config == null) {
                _uiState.update { it.copy(error = "请先在设置中添加 LLM 配置") }
                return@launch
            }

            val character = _uiState.value.character
            val systemPrompt = buildSystemPrompt(character)

            val now = System.currentTimeMillis()
            val assistantId = UUID.randomUUID().toString()
            streamingMessageId = assistantId

            // Create empty assistant message in DB
            val placeholder = Message(
                id = assistantId,
                chatId = chatId,
                role = MessageRole.ASSISTANT,
                content = "",
                swipes = listOf(""),
                createdAt = now
            )
            chatRepository.insertMessage(placeholder)

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + placeholder,
                    isGenerating = true
                )
            }

            // Notify WebView a new streaming message is starting
            _events.emit(ChatEvent.SendToWebView(
                "setGenerating",
                """{"generating":true,"id":"$assistantId"}"""
            ))

            var fullContent = ""
            var fullReasoning = ""

            try {
                llmService.streamChat(config, messages, systemPrompt).collect { chunk ->
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

                // Save final message
                val finalMsg = placeholder.copy(
                    content = fullContent,
                    reasoning = fullReasoning.ifBlank { null },
                    swipes = listOf(fullContent)
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
                    "setGenerating", """{"generating":false}"""
                ))
            } finally {
                streamingMessageId = null
            }
        }
    }

    fun stopGeneration() {
        generationJob?.cancel()
        generationJob = null
        _uiState.update { it.copy(isGenerating = false) }
        viewModelScope.launch {
            _events.emit(ChatEvent.SendToWebView("setGenerating", """{"generating":false}"""))
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        generationJob?.cancel()
    }

    // ─── Payload builders ─────────────────────────────────────────────────────

    /**
     * Build setMessages payload.
     * Format: { messages: [ { id, role, content, reasoning? } ] }
     */
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

    /**
     * Build appendToken payload.
     * Format: { id, token, isReasoning }
     */
    private fun buildTokenPayload(id: String, token: String, isReasoning: Boolean): String {
        return """{"id":"$id","token":"${escapeJson(token)}","isReasoning":$isReasoning}"""
    }

    private fun buildSystemPrompt(character: Character?): String {
        if (character == null) return ""
        val parts = mutableListOf<String>()
        if (character.systemPrompt.isNotBlank()) parts.add(character.systemPrompt)
        if (character.description.isNotBlank()) parts.add("描述：${character.description}")
        if (character.personality.isNotBlank()) parts.add("性格：${character.personality}")
        if (character.scenario.isNotBlank()) parts.add("场景：${character.scenario}")
        return parts.joinToString("\n\n")
    }

    /**
     * Escape a string for safe embedding inside a JSON string value.
     */
    private fun escapeJson(text: String): String = text
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
}