package com.beichenxing.aurora.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.model.Chat
import com.beichenxing.aurora.domain.model.LlmStreamChunk
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

    // Injected via navigation argument
    private val characterId: String = checkNotNull(savedStateHandle["characterId"])

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Events emitted to the Compose layer (e.g., tell WebView to do something)
    private val _events = MutableSharedFlow<ChatEvent>()
    val events: SharedFlow<ChatEvent> = _events.asSharedFlow()

    private var currentChatId: String? = null
    private var generationJob: Job? = null
    private var streamingMessageId: String? = null

    init {
        loadCharacterAndChat()
    }

    // ─── Init ─────────────────────────────────────────────────────────────────

    private fun loadCharacterAndChat() {
        viewModelScope.launch {
            try {
                val character = characterRepository.getById(characterId) ?: return@launch
                _uiState.update { it.copy(character = character) }

                // Get or create a chat for this character
                val existingChats = chatRepository.getByCharacter(characterId)
                val chat = if (existingChats.isNotEmpty()) {
                    existingChats.first() // most recent
                } else {
                    createNewChat(character)
                }

                currentChatId = chat.id

                // Load messages
                val messages = chatRepository.getMessages(chat.id)
                _uiState.update { it.copy(messages = messages) }

                // If WebView is already ready, push data
                if (_uiState.value.isWebViewReady) {
                    pushToWebView()
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

        // Add first message from character if defined
        if (character.firstMessage.isNotBlank()) {
            val firstMsg = Message(
                id = UUID.randomUUID().toString(),
                chatId = chat.id,
                role = MessageRole.ASSISTANT,
                content = character.firstMessage,
                swipes = listOf(character.firstMessage),
                currentSwipeIndex = 0,
                createdAt = now
            )
            chatRepository.insertMessage(firstMsg)
            _uiState.update { it.copy(messages = listOf(firstMsg)) }
        }

        return chat
    }

    // ─── WebView Ready ────────────────────────────────────────────────────────

    /**
     * Called by ChatScreen when the WebView fires the "ready" bridge event.
     */
    fun onWebViewReady() {
        Timber.i("[Chat] WebView ready")
        _uiState.update { it.copy(isWebViewReady = true) }
        viewModelScope.launch {
            pushToWebView()
        }
    }

    /**
     * Push character info and all messages to the WebView.
     */
    private suspend fun pushToWebView() {
        val state = _uiState.value
        val character = state.character ?: return

        // Push character info
        val characterPayload = buildCharacterPayload(character)
        _events.emit(ChatEvent.SendToWebView(
            type = "setCharacter",
            payloadJson = characterPayload
        ))

        // Push messages
        val messagesPayload = buildMessagesPayload(state.messages)
        _events.emit(ChatEvent.SendToWebView(
            type = "setMessages",
            payloadJson = messagesPayload
        ))

        // Scroll to bottom
        _events.emit(ChatEvent.ScrollToBottom)
    }

    // ─── User Input ───────────────────────────────────────────────────────────

    /**
     * Called when user submits a message from the HTML input bar.
     */
    fun onUserInput(text: String) {
        if (text.isBlank()) return
        if (_uiState.value.isGenerating) return
        val chatId = currentChatId ?: return

        viewModelScope.launch {
            try {
                // 1. Save user message
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

                val updatedMessages = chatRepository.getMessages(chatId)
                _uiState.update { it.copy(messages = updatedMessages) }

                // 2. Update WebView with new user message
                val messagesPayload = buildMessagesPayload(updatedMessages)
                _events.emit(ChatEvent.SendToWebView(
                    type = "setMessages",
                    payloadJson = messagesPayload
                ))
                _events.emit(ChatEvent.ScrollToBottom)

                // 3. Start AI generation
                generateResponse(chatId, updatedMessages)

            } catch (e: Exception) {
                Timber.e(e, "Failed to send message")
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // ─── Generation ───────────────────────────────────────────────────────────

    private fun generateResponse(chatId: String, messages: List<Message>) {
        generationJob?.cancel()

        generationJob = viewModelScope.launch {
            val config = llmConfigRepository.getActive()
            if (config == null) {
                _uiState.update { it.copy(error = "请先在设置中配置 LLM") }
                return@launch
            }

            val character = _uiState.value.character
            val systemPrompt = buildSystemPrompt(character)

            // Create placeholder assistant message
            val now = System.currentTimeMillis()
            val assistantMsgId = UUID.randomUUID().toString()
            streamingMessageId = assistantMsgId

            val placeholderMsg = Message(
                id = assistantMsgId,
                chatId = chatId,
                role = MessageRole.ASSISTANT,
                content = "",
                swipes = listOf(""),
                createdAt = now
            )
            chatRepository.insertMessage(placeholderMsg)

            _uiState.update { state ->
                state.copy(
                    messages = state.messages + placeholderMsg,
                    isGenerating = true
                )
            }

            // Notify WebView generation started
            _events.emit(ChatEvent.SendToWebView(
                type = "setGenerating",
                payloadJson = """{"generating":true,"messageId":"$assistantMsgId"}"""
            ))

            // Stream tokens
            var fullContent = ""
            var fullReasoning = ""

            try {
                llmService.streamChat(config, messages, systemPrompt).collect { chunk ->
                    when {
                        chunk.isDone -> Unit // handled below

                        chunk.isReasoningChunk && chunk.reasoning != null -> {
                            fullReasoning += chunk.reasoning
                            // Send reasoning token to WebView
                            val tokenPayload = buildAppendTokenPayload(
                                messageId = assistantMsgId,
                                token = chunk.reasoning,
                                isReasoning = true
                            )
                            _events.emit(ChatEvent.SendToWebView(
                                type = "appendToken",
                                payloadJson = tokenPayload
                            ))
                        }

                        !chunk.isReasoningChunk && chunk.content != null -> {
                            fullContent += chunk.content
                            // Send content token to WebView
                            val tokenPayload = buildAppendTokenPayload(
                                messageId = assistantMsgId,
                                token = chunk.content,
                                isReasoning = false
                            )
                            _events.emit(ChatEvent.SendToWebView(
                                type = "appendToken",
                                payloadJson = tokenPayload
                            ))
                        }
                    }
                }

                // Generation complete - save to DB
                val finalMsg = placeholderMsg.copy(
                    content = fullContent,
                    reasoning = fullReasoning.ifBlank { null },
                    swipes = listOf(fullContent)
                )
                chatRepository.updateMessage(finalMsg)

                val updatedMessages = chatRepository.getMessages(chatId)
                _uiState.update { it.copy(messages = updatedMessages, isGenerating = false) }

                // Notify WebView
                _events.emit(ChatEvent.SendToWebView(
                    type = "streamEnd",
                    payloadJson = """{"messageId":"$assistantMsgId"}"""
                ))
                _events.emit(ChatEvent.SendToWebView(
                    type = "setGenerating",
                    payloadJson = """{"generating":false}"""
                ))

            } catch (e: Exception) {
                Timber.e(e, "Generation failed")

                val errorMsg = placeholderMsg.copy(content = "[生成失败: ${e.message}]")
                chatRepository.updateMessage(errorMsg)

                _uiState.update { state ->
                    val msgs = state.messages.map {
                        if (it.id == assistantMsgId) errorMsg else it
                    }
                    state.copy(messages = msgs, isGenerating = false, error = e.message)
                }

                _events.emit(ChatEvent.SendToWebView(
                    type = "streamError",
                    payloadJson = """{"error":"${e.message ?: "Unknown error"}"}"""
                ))
                _events.emit(ChatEvent.SendToWebView(
                    type = "setGenerating",
                    payloadJson = """{"generating":false}"""
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
            _events.emit(ChatEvent.SendToWebView(
                type = "setGenerating",
                payloadJson = """{"generating":false}"""
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

    // ─── Payload Builders ─────────────────────────────────────────────────────

    private fun buildSystemPrompt(character: Character?): String {
        if (character == null) return ""
        val parts = mutableListOf<String>()
        if (character.systemPrompt.isNotBlank()) parts.add(character.systemPrompt)
        if (character.description.isNotBlank()) parts.add("描述：${character.description}")
        if (character.personality.isNotBlank()) parts.add("性格：${character.personality}")
        if (character.scenario.isNotBlank()) parts.add("场景：${character.scenario}")
        return parts.joinToString("\n\n")
    }

    private fun buildCharacterPayload(character: Character): String {
        // Build minimal character JSON that chat_stage.html understands
        val name = character.name.replace("\"", "\\\"")
        val desc = character.description.replace("\"", "\\\"").take(500)
        val avatarPath = character.avatarPath?.replace("\\", "\\\\")
            ?.replace("\"", "\\\"") ?: ""
        return """
            {
                "id": "${character.id}",
                "name": "$name",
                "description": "$desc",
                "avatarPath": "$avatarPath"
            }
        """.trimIndent()
    }

    private fun buildMessagesPayload(messages: List<Message>): String {
        val sb = StringBuilder()
        sb.append("""{"messages":[""")
        messages.forEachIndexed { index, msg ->
            val content = msg.currentContent
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
            val reasoning = msg.currentReasoning
                ?.replace("\\", "\\\\")
                ?.replace("\"", "\\\"")
                ?.replace("\n", "\\n")
                ?.replace("\r", "\\r")
                ?: ""

            sb.append("""{"id":"${msg.id}","role":"${msg.role.value}","content":"$content"""")
            if (reasoning.isNotBlank()) {
                sb.append(""","reasoning":"$reasoning"""")
            }
            sb.append("}")
            if (index < messages.size - 1) sb.append(",")
        }
        sb.append("]}")
        return sb.toString()
    }

    private fun buildAppendTokenPayload(
        messageId: String,
        token: String,
        isReasoning: Boolean
    ): String {
        val escaped = token
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
        return """{"id":"$messageId","token":"$escaped","isReasoning":$isReasoning}"""
    }
}