package com.beichenxing.aurora.domain.model

/**
 * A single content block to be injected into the prompt.
 *
 * @param content The text content of this block.
 * @param role    The role this block occupies in the final prompt (default: SYSTEM).
 * @param source  Debug label identifying where this block came from,
 *                e.g. "character.systemPrompt", "worldInfo.entry.42".
 */
data class PromptBlock(
    val content: String,
    val role: MessageRole = MessageRole.SYSTEM,
    val source: String = ""
)

/**
 * A single turn in the chat history, stripped of Aurora-internal metadata.
 * LlmService consumes this instead of the full [Message] domain model.
 */
data class PromptMessage(
    val role: MessageRole,
    val content: String,
    val id: String = "",
    val isHidden: Boolean = false
)

/**
 * Provider-neutral, structured representation of a complete generation prompt.
 *
 * Phase 1: only [systemBlocks] and [chatMessages] are populated.
 * Future phases will fill [depthBlocks], [authorNoteBefore], [authorNoteAfter],
 * [exampleBlocks], and [outlets] as world info, AN, and persona are implemented.
 *
 * Slot semantics follow SillyTavern's insertion positions:
 *   systemBlocks    → worldInfoBefore / worldInfoAfter / character fields / persona
 *   exampleBlocks   → EMTop / EMBottom
 *   depthBlocks     → atDepth (key = distance from end of chat)
 *   authorNoteBefore → ANTop
 *   authorNoteAfter  → ANBottom
 *   outlets         → named prompt manager positions
 */
data class PromptPlan(
    val systemBlocks: List<PromptBlock> = emptyList(),
    val exampleBlocks: List<PromptBlock> = emptyList(),
    val chatMessages: List<PromptMessage> = emptyList(),
    val depthBlocks: Map<Int, List<PromptBlock>> = emptyMap(),
    val authorNoteBefore: List<PromptBlock> = emptyList(),
    val authorNoteAfter: List<PromptBlock> = emptyList(),
    val outlets: Map<String, List<PromptBlock>> = emptyMap()
) {
    /**
     * Flattens [systemBlocks] into a single string joined by blank lines.
     * Used by LlmService for providers that accept a single system string
     * (OpenAI-compatible, Claude, Gemini).
     */
    fun flatSystemPrompt(): String =
        systemBlocks
            .filter { it.content.isNotBlank() }
            .joinToString("\n\n") { it.content }
}