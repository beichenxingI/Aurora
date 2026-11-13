package com.beichenxing.aurora.domain.model

data class LlmConfig(
    val id: String,
    val name: String,
    val provider: LlmProvider,
    val endpoint: String,
    val apiKey: String = "",
    val model: String = "",
    val isActive: Boolean = false,
    val maxTokens: Int = 8192,
    val contextLength: Int = 100000,
    val temperature: Double = 1.0,
    val topP: Double = 0.95,
    val topK: Int = 40,
    val minP: Double = 0.0,
    val frequencyPenalty: Double = 0.0,
    val presencePenalty: Double = 0.0,
    val repetitionPenalty: Double = 1.0,
    val stopSequences: List<String> = emptyList(),
    val streamEnabled: Boolean = true,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

enum class LlmProvider(val id: String, val displayName: String, val defaultEndpoint: String) {
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultEndpoint = "https://api.openai.com/v1"
    ),
    OPENAI_COMPATIBLE(
        id = "openai_compatible",
        displayName = "OpenAI Compatible",
        defaultEndpoint = ""
    ),
    CLAUDE(
        id = "claude",
        displayName = "Anthropic Claude",
        defaultEndpoint = "https://api.anthropic.com"
    ),
    GEMINI(
        id = "gemini",
        displayName = "Google Gemini",
        defaultEndpoint = "https://generativelanguage.googleapis.com"
    ),
    DEEPSEEK(
        id = "deepseek",
        displayName = "DeepSeek",
        defaultEndpoint = "https://api.deepseek.com/v1"
    ),
    QWEN(
        id = "qwen",
        displayName = "Qwen (Alibaba)",
        defaultEndpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    ),
    OPENROUTER(
        id = "openrouter",
        displayName = "OpenRouter",
        defaultEndpoint = "https://openrouter.ai/api/v1"
    );

    companion object {
        fun fromId(id: String): LlmProvider = entries.find {
            it.id == id
        } ?: OPENAI_COMPATIBLE
    }
}

// Streaming response chunk
data class LlmStreamChunk(
    val content: String? = null,
    val reasoning: String? = null,
    val isReasoningChunk: Boolean = false,
    val isDone: Boolean = false
)