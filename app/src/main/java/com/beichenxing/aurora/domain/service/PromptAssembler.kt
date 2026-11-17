package com.beichenxing.aurora.domain.service

import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.PromptBlock
import com.beichenxing.aurora.domain.model.PromptMessage
import com.beichenxing.aurora.domain.model.PromptPlan
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles a [PromptPlan] from Aurora domain objects.
 *
 * Phase 1: reproduces the existing buildSystemPrompt() behaviour exactly —
 * systemPrompt / description / personality / scenario joined by blank lines.
 *
 * Future phases add to this class only; ChatViewModel and LlmService stay unchanged:
 *   - character_book / world info activation  → more blocks in systemBlocks / depthBlocks
 *   - Persona                                 → prepend to systemBlocks
 *   - Author's Note                           → authorNoteBefore / authorNoteAfter
 *   - post_history_instructions               → append to systemBlocks or separate slot
 *   - token budget and history trimming       → trim chatMessages here
 */
@Singleton
class PromptAssembler @Inject constructor() {

    /**
     * Build a [PromptPlan] for a generation request.
     *
     * @param character The active character, or null if none is selected.
     * @param messages  Chat history in chronological order,
     *                  not yet including the assistant placeholder.
     */
    fun assemble(
        character: Character?,
        messages: List<Message>
    ): PromptPlan {
        val systemBlocks = buildSystemBlocks(character)
        val chatMessages = messages.map { msg ->
            PromptMessage(
                role     = msg.role,
                content  = msg.currentContent,
                id       = msg.id,
                isHidden = msg.isHidden
            )
        }
        return PromptPlan(
            systemBlocks = systemBlocks,
            chatMessages = chatMessages
        )
    }

    // -------------------------------------------------------------------------

    private fun buildSystemBlocks(character: Character?): List<PromptBlock> {
        if (character == null) return emptyList()
        val blocks = mutableListOf<PromptBlock>()

        if (character.systemPrompt.isNotBlank())
            blocks.add(PromptBlock(
                content = character.systemPrompt,
                source  = "character.systemPrompt"
            ))

        if (character.description.isNotBlank())
            blocks.add(PromptBlock(
                content = "描述：${character.description}",
                source  = "character.description"
            ))

        if (character.personality.isNotBlank())
            blocks.add(PromptBlock(
                content = "性格：${character.personality}",
                source  = "character.personality"
            ))

        if (character.scenario.isNotBlank())
            blocks.add(PromptBlock(
                content = "场景：${character.scenario}",
                source  = "character.scenario"
            ))

        return blocks
    }
}