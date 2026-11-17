package com.beichenxing.aurora.domain.service

import com.beichenxing.aurora.domain.model.LlmConfig
import com.beichenxing.aurora.domain.model.LlmProvider
import com.beichenxing.aurora.domain.model.LlmStreamChunk
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.MessageRole
import com.beichenxing.aurora.domain.model.PromptBlock
import com.beichenxing.aurora.domain.model.PromptMessage
import com.beichenxing.aurora.domain.model.PromptPlan
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine

@Singleton
class LlmService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ─── Public API ───────────────────────────────────────────────────────────

    /**
     * Primary API: accepts a [PromptPlan] produced by [PromptAssembler].
     *
     * Phase 1: flattens systemBlocks, interleaves depthBlocks into chat history.
     * Phase 2 will refactor provider builders to consume PromptPlan directly,
     * enabling multi-role system blocks and outlet/AN handling.
     */
    fun streamChat(
        config: LlmConfig,
        plan: PromptPlan
    ): Flow<LlmStreamChunk> {
        val systemPrompt = plan.flatSystemPrompt()

        // Convert PromptMessage → Message
        val baseMessages = plan.chatMessages.map { pm ->
            Message(
                id        = pm.id.ifBlank { java.util.UUID.randomUUID().toString() },
                chatId    = "",
                role      = pm.role,
                content   = pm.content,
                isHidden  = pm.isHidden,
                createdAt = 0L
            )
        }

        // Interleave depthBlocks into chat history
        val messagesWithDepth = if (plan.depthBlocks.isEmpty()) {
            baseMessages
        } else {
            interleaveDepthBlocks(baseMessages, plan.depthBlocks)
        }

        return streamChat(config, messagesWithDepth, systemPrompt)
    }

    /**
     * Send messages to the configured LLM and return a streaming flow of chunks.
     * Each chunk contains either a content delta or a reasoning delta.
     */
    fun streamChat(
        config: LlmConfig,
        messages: List<Message>,
        systemPrompt: String = ""
    ): Flow<LlmStreamChunk> = when (config.provider) {
        LlmProvider.CLAUDE -> streamClaude(config, messages, systemPrompt)
        LlmProvider.GEMINI -> streamGemini(config, messages, systemPrompt)
        else -> streamOpenAiCompatible(config, messages, systemPrompt)
    }

    // ─── OpenAI-compatible (OpenAI, DeepSeek, Qwen, OpenRouter, etc.) ─────────

    private fun streamOpenAiCompatible(
        config: LlmConfig,
        messages: List<Message>,
        systemPrompt: String
    ): Flow<LlmStreamChunk> = callbackFlow {
        val builtMessages = buildOpenAiMessages(messages, systemPrompt)

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.model))
            put("messages", builtMessages)
            put("max_tokens", JsonPrimitive(config.maxTokens))
            put("temperature", JsonPrimitive(config.temperature))
            put("top_p", JsonPrimitive(config.topP))
            put("stream", JsonPrimitive(true))
            if (config.frequencyPenalty != 0.0) {
                put("frequency_penalty", JsonPrimitive(config.frequencyPenalty))
            }
            if (config.presencePenalty != 0.0) {
                put("presence_penalty", JsonPrimitive(config.presencePenalty))
            }
            if (config.stopSequences.isNotEmpty()) {
                put("stop", buildJsonArray {
                    config.stopSequences.forEach { add(JsonPrimitive(it)) }
                })
            }
        }

        val endpoint = config.endpoint.trimEnd('/') + "/chat/completions"

        val request = Request.Builder()
            .url(endpoint)
            .post(json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                requestBody
            ).toRequestBody("application/json".toMediaType()))
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Content-Type", "application/json")
            .build()

        val factory = EventSources.createFactory(okHttpClient)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                if (data == "[DONE]") {
                    trySend(LlmStreamChunk(isDone = true))
                    close()
                    return
                }
                try {
                    val parsed = json.parseToJsonElement(data).jsonObject
                    val choices = parsed["choices"]?.jsonArray ?: return
                    val delta = choices[0].jsonObject["delta"]?.jsonObject ?: return

                    // Extract content delta
                    val content = delta["content"]?.jsonPrimitive?.content
                    // Extract reasoning_content (DeepSeek R1 and similar)
                    val reasoning = delta["reasoning_content"]?.jsonPrimitive?.content

                    if (!content.isNullOrEmpty()) {
                        trySend(LlmStreamChunk(content = content, isReasoningChunk = false))
                    }
                    if (!reasoning.isNullOrEmpty()) {
                        trySend(LlmStreamChunk(reasoning = reasoning, isReasoningChunk = true))
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse SSE chunk: $data")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                val error = t ?: IOException(
                    "SSE failed: HTTP ${response?.code} ${response?.message}"
                )
                close(error)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)

        awaitClose { eventSource.cancel() }
    }

    // ─── Anthropic Claude ─────────────────────────────────────────────────────

    private fun streamClaude(
        config: LlmConfig,
        messages: List<Message>,
        systemPrompt: String
    ): Flow<LlmStreamChunk> = callbackFlow {
        val builtMessages = buildClaudeMessages(messages)

        val requestBody = buildJsonObject {
            put("model", JsonPrimitive(config.model))
            put("messages", builtMessages)
            put("max_tokens", JsonPrimitive(config.maxTokens))
            put("temperature", JsonPrimitive(config.temperature))
            put("stream", JsonPrimitive(true))
            if (systemPrompt.isNotBlank()) {
                put("system", JsonPrimitive(systemPrompt))
            }
        }

        val endpoint = config.endpoint.trimEnd('/') + "/v1/messages"

        val request = Request.Builder()
            .url(endpoint)
            .post(json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                requestBody
            ).toRequestBody("application/json".toMediaType()))
            .header("x-api-key", config.apiKey)
            .header("anthropic-version", "2023-06-01")
            .header("Content-Type", "application/json")
            .build()

        val factory = EventSources.createFactory(okHttpClient)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    val parsed = json.parseToJsonElement(data).jsonObject
                    when (parsed["type"]?.jsonPrimitive?.content) {
                        "content_block_delta" -> {
                            val delta = parsed["delta"]?.jsonObject ?: return
                            when (delta["type"]?.jsonPrimitive?.content) {
                                "text_delta" -> {
                                    val text = delta["text"]?.jsonPrimitive?.content
                                    if (!text.isNullOrEmpty()) {
                                        trySend(LlmStreamChunk(
                                            content = text,
                                            isReasoningChunk = false
                                        ))
                                    }
                                }
                                "thinking_delta" -> {
                                    // Claude extended thinking
                                    val thinking = delta["thinking"]?.jsonPrimitive?.content
                                    if (!thinking.isNullOrEmpty()) {
                                        trySend(LlmStreamChunk(
                                            reasoning = thinking,
                                            isReasoningChunk = true
                                        ))
                                    }
                                }
                            }
                        }
                        "message_stop" -> {
                            trySend(LlmStreamChunk(isDone = true))
                            close()
                        }
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse Claude SSE chunk: $data")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                val error = t ?: IOException(
                    "Claude SSE failed: HTTP ${response?.code} ${response?.message}"
                )
                close(error)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    // ─── Google Gemini ────────────────────────────────────────────────────────

    private fun streamGemini(
        config: LlmConfig,
        messages: List<Message>,
        systemPrompt: String
    ): Flow<LlmStreamChunk> = callbackFlow {
        val contents = buildGeminiContents(messages)

        val requestBody = buildJsonObject {
            put("contents", contents)
            if (systemPrompt.isNotBlank()) {
                put("system_instruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject {
                            put("text", JsonPrimitive(systemPrompt))
                        })
                    })
                })
            }
            put("generationConfig", buildJsonObject {
                put("maxOutputTokens", JsonPrimitive(config.maxTokens))
                put("temperature", JsonPrimitive(config.temperature))
                put("topP", JsonPrimitive(config.topP))
                put("topK", JsonPrimitive(config.topK))
            })
        }

        val endpoint = "${config.endpoint.trimEnd('/')}/v1beta/models/${config.model}" +
            ":streamGenerateContent?alt=sse&key=${config.apiKey}"

        val request = Request.Builder()
            .url(endpoint)
            .post(json.encodeToString(
                kotlinx.serialization.json.JsonObject.serializer(),
                requestBody
            ).toRequestBody("application/json".toMediaType()))
            .header("Content-Type", "application/json")
            .build()

        val factory = EventSources.createFactory(okHttpClient)

        val listener = object : EventSourceListener() {
            override fun onEvent(
                eventSource: EventSource,
                id: String?,
                type: String?,
                data: String
            ) {
                try {
                    val parsed = json.parseToJsonElement(data).jsonObject
                    val candidates = parsed["candidates"]?.jsonArray ?: return
                    val content = candidates[0].jsonObject["content"]?.jsonObject ?: return
                    val parts = content["parts"]?.jsonArray ?: return

                    parts.forEach { part ->
                        val partObj = part.jsonObject
                        // Regular text
                        val text = partObj["text"]?.jsonPrimitive?.content
                        if (!text.isNullOrEmpty()) {
                            trySend(LlmStreamChunk(content = text, isReasoningChunk = false))
                        }
                        // Gemini thinking (thought field)
                        val thought = partObj["thought"]?.jsonPrimitive?.content
                        if (!thought.isNullOrEmpty()) {
                            trySend(LlmStreamChunk(reasoning = thought, isReasoningChunk = true))
                        }
                    }

                    // Check if generation is complete
                    val finishReason = candidates[0].jsonObject["finishReason"]
                        ?.jsonPrimitive?.content
                    if (finishReason == "STOP" || finishReason == "MAX_TOKENS") {
                        trySend(LlmStreamChunk(isDone = true))
                        close()
                    }
                } catch (e: Exception) {
                    Timber.w(e, "Failed to parse Gemini SSE chunk: $data")
                }
            }

            override fun onFailure(
                eventSource: EventSource,
                t: Throwable?,
                response: okhttp3.Response?
            ) {
                val error = t ?: IOException(
                    "Gemini SSE failed: HTTP ${response?.code} ${response?.message}"
                )
                close(error)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }
        }

        val eventSource = factory.newEventSource(request, listener)
        awaitClose { eventSource.cancel() }
    }

    // ─── Message builders ─────────────────────────────────────────────────────

    private fun buildOpenAiMessages(
        messages: List<Message>,
        systemPrompt: String
    ): JsonArray = buildJsonArray {
        // System prompt as first message
        if (systemPrompt.isNotBlank()) {
            add(buildJsonObject {
                put("role", JsonPrimitive("system"))
                put("content", JsonPrimitive(systemPrompt))
            })
        }
        // Chat history (skip hidden messages)
        messages.filter { !it.isHidden }.forEach { msg ->
            add(buildJsonObject {
                put("role", JsonPrimitive(msg.role.value))
                put("content", JsonPrimitive(msg.currentContent))
            })
        }
    }

    private fun buildClaudeMessages(messages: List<Message>): JsonArray = buildJsonArray {
        // Claude only accepts user/assistant roles (no system in messages array)
        messages.filter { !it.isHidden && it.role != MessageRole.SYSTEM }.forEach { msg ->
            add(buildJsonObject {
                put("role", JsonPrimitive(msg.role.value))
                put("content", JsonPrimitive(msg.currentContent))
            })
        }
    }

    private fun buildGeminiContents(messages: List<Message>): JsonArray = buildJsonArray {
        // Gemini uses "user" and "model" roles
        messages.filter { !it.isHidden }.forEach { msg ->
            val geminiRole = when (msg.role) {
                MessageRole.USER -> "user"
                MessageRole.ASSISTANT -> "model"
                MessageRole.SYSTEM -> "user" // Gemini doesn't support system role in contents
            }
            add(buildJsonObject {
                put("role", JsonPrimitive(geminiRole))
                put("parts", buildJsonArray {
                    add(buildJsonObject {
                        put("text", JsonPrimitive(msg.currentContent))
                    })
                })
            })
        }
    }

    // ─── Depth block interleaving ─────────────────────────────────────────────

    /**
     * Insert world info depth blocks into chat history.
     *
     * Depth semantics (ST convention):
     *   depth=0 → after last message
     *   depth=1 → before last message
     *   depth=2 → before 2nd-to-last message
     *   depth=N → before Nth-from-end message
     *
     * If depth >= message count, blocks are prepended to history.
     *
     * @param baseMessages Original chat history in chronological order.
     * @param depthBlocks  Map of depth → blocks to insert at that depth.
     * @return New message list with depth blocks interleaved as system messages.
     */
    private fun interleaveDepthBlocks(
        baseMessages: List<Message>,
        depthBlocks: Map<Int, List<PromptBlock>>
    ): List<Message> {
        if (depthBlocks.isEmpty()) return baseMessages

        val result = mutableListOf<Message>()
        val messageCount = baseMessages.size

        // Process messages in reverse order to handle depth correctly
        // Build a map: insert position → blocks to insert before that position
        val insertMap = mutableMapOf<Int, MutableList<PromptBlock>>()

        depthBlocks.forEach { (depth, blocks) ->
            // depth=0 means after last message (insert position = messageCount)
            // depth=1 means before last message (insert position = messageCount - 1)
            // depth=N means before Nth-from-end (insert position = messageCount - N)
            val insertPos = (messageCount - depth).coerceIn(0, messageCount)
            insertMap.getOrPut(insertPos) { mutableListOf() }.addAll(blocks)
        }

        // Build final list
        for (i in 0..messageCount) {
            // Insert blocks scheduled for this position
            insertMap[i]?.forEach { block ->
                result.add(Message(
                    id        = java.util.UUID.randomUUID().toString(),
                    chatId    = "",
                    role      = block.role,
                    content   = block.content,
                    isHidden  = false,
                    createdAt = 0L
                ))
            }
            // Add original message if not past end
            if (i < messageCount) {
                result.add(baseMessages[i])
            }
        }

        return result
    }
}