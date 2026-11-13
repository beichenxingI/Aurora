package com.beichenxing.aurora.domain.model

import kotlinx.serialization.Serializable

data class Message(
    val id: String,
    val chatId: String,
    val role: MessageRole,
    val content: String = "",
    val reasoning: String? = null,
    val swipes: List<String> = emptyList(),
    val swipesReasoning: List<String> = emptyList(),
    val currentSwipeIndex: Int = 0,
    val isHidden: Boolean = false,
    val attachments: List<MessageAttachment> = emptyList(),
    val characterId: String? = null,
    val characterName: String? = null,
    val createdAt: Long = 0L
) {
    val currentContent: String
        get() = if (swipes.isNotEmpty()) {
            swipes.getOrNull(currentSwipeIndex) ?: content
        } else {
            content
        }

    val currentReasoning: String?
        get() = if (swipesReasoning.isNotEmpty()) {
            swipesReasoning.getOrNull(currentSwipeIndex) ?: reasoning
        } else {
            reasoning
        }

    val hasReasoning: Boolean
        get() = !reasoning.isNullOrBlank() || swipesReasoning.any { it.isNotBlank() }
}

enum class MessageRole(val value: String) {
    USER("user"),
    ASSISTANT("assistant"),
    SYSTEM("system");

    companion object {
        fun fromString(value: String): MessageRole = entries.find {
            it.value == value
        } ?: USER
    }
}

@Serializable
data class MessageAttachment(
    val id: String,
    val path: String,
    val mimeType: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val sizeBytes: Long? = null
)