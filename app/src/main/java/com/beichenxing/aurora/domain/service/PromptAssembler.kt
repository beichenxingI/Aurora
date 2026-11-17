package com.beichenxing.aurora.domain.service

import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.model.CharacterBook
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.MessageRole
import com.beichenxing.aurora.domain.model.PromptBlock
import com.beichenxing.aurora.domain.model.PromptMessage
import com.beichenxing.aurora.domain.model.PromptPlan
import com.beichenxing.aurora.domain.model.SelectiveLogic
import com.beichenxing.aurora.domain.model.WorldInfoEntry
import com.beichenxing.aurora.domain.model.WorldInfoPosition
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Assembles a [PromptPlan] from Aurora domain objects.
 *
 * Phase 1: reproduces the existing buildSystemPrompt() behaviour exactly —
 * systemPrompt / description / personality / scenario joined by blank lines.
 * It also activates the character's embedded character_book using [WorldInfoScanner].
 *
 * Future phases add to this class only; ChatViewModel and LlmService stay unchanged:
 *   - Persona                                 → prepend to systemBlocks
 *   - Author's Note                           → authorNoteBefore / authorNoteAfter
 *   - post_history_instructions               → append to systemBlocks or separate slot
 *   - token budget and history trimming       → trim chatMessages here
 */
@Singleton
class PromptAssembler @Inject constructor(
    private val worldInfoScanner: WorldInfoScanner
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Build a [PromptPlan] for a generation request.
     *
     * Phase 1: adds character book activation.
     * Future phases: Persona prepend, Author's Note, token budget, history trimming.
     *
     * @param character The active character, or null if none is selected.
     * @param messages  Chat history in chronological order,
     *                  not yet including the assistant placeholder.
     */
    fun assemble(
        character: Character?,
        messages: List<Message>
    ): PromptPlan {
        val systemBlocks = mutableListOf<PromptBlock>()
        val depthBlocksMap = mutableMapOf<Int, MutableList<PromptBlock>>()

        // Character system blocks
        systemBlocks.addAll(buildSystemBlocks(character))

        // World info activation
        if (character != null && character.characterBookJson.isNotBlank()) {
            val book = parseCharacterBook(character.characterBookJson)
            if (book != null) {
                val activatedGroups = worldInfoScanner.scan(
                    book          = book,
                    messages      = messages,
                    characterId   = character.id,
                    globalScanDepth = 2,
                    globalCaseSensitive = false,
                    globalMatchWholeWords = false
                )

                // BEFORE: prepend to systemBlocks (will be before character fields)
                activatedGroups[WorldInfoPosition.BEFORE]?.forEach { entry ->
                    val content = cleanContent(entry.content)
                    if (content.isNotBlank()) {
                        systemBlocks.add(0, PromptBlock(
                            content = content,
                            role    = roleFromEntry(entry),
                            source  = "worldInfo.${entry.uid}.before"
                        ))
                    }
                }

                // AFTER: append to systemBlocks (will be after character fields)
                activatedGroups[WorldInfoPosition.AFTER]?.forEach { entry ->
                    val content = cleanContent(entry.content)
                    if (content.isNotBlank()) {
                        systemBlocks.add(PromptBlock(
                            content = content,
                            role    = roleFromEntry(entry),
                            source  = "worldInfo.${entry.uid}.after"
                        ))
                    }
                }

                // AT_DEPTH: add to depthBlocks map
                activatedGroups[WorldInfoPosition.AT_DEPTH]?.forEach { entry ->
                    val depth = entry.depth.coerceAtLeast(0)
                    val content = cleanContent(entry.content)
                    if (content.isNotBlank()) {
                        depthBlocksMap.getOrPut(depth) { mutableListOf() }.add(
                            PromptBlock(
                                content = content,
                                role    = roleFromEntry(entry),
                                source  = "worldInfo.${entry.uid}.atDepth.$depth"
                            )
                        )
                    }
                }

                // AN_TOP / AN_BOTTOM / EM_TOP / EM_BOTTOM / OUTLET: Phase 2
                // For now, fallback to AFTER if any of these positions are used
                listOf(
                    WorldInfoPosition.AN_TOP,
                    WorldInfoPosition.AN_BOTTOM,
                    WorldInfoPosition.EM_TOP,
                    WorldInfoPosition.EM_BOTTOM,
                    WorldInfoPosition.OUTLET
                ).forEach { pos ->
                    activatedGroups[pos]?.forEach { entry ->
                        val content = cleanContent(entry.content)
                        if (content.isNotBlank()) {
                            systemBlocks.add(PromptBlock(
                                content = content,
                                role    = roleFromEntry(entry),
                                source  = "worldInfo.${entry.uid}.${pos.name.lowercase()}"
                            ))
                        }
                    }
                }
            }
        }

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
            chatMessages = chatMessages,
            depthBlocks  = depthBlocksMap.mapValues { it.value.toList() }
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

    // ─── Character book parsing ───────────────────────────────────────────────

    private fun parseCharacterBook(jsonString: String): CharacterBook? {
        return try {
            parseCharacterBookFromJson(jsonString)
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse character_book_json")
            null
        }
    }

    private fun parseCharacterBookFromJson(jsonString: String): CharacterBook? {
        val root = json.parseToJsonElement(jsonString).jsonObject

        val name = root["name"]?.jsonPrimitive?.content ?: ""
        val description = root["description"]?.jsonPrimitive?.content ?: ""
        val scanDepth = root["scan_depth"]?.jsonPrimitive?.intOrNull
            ?: root["scanDepth"]?.jsonPrimitive?.intOrNull
        val tokenBudget = root["token_budget"]?.jsonPrimitive?.intOrNull
            ?: root["tokenBudget"]?.jsonPrimitive?.intOrNull
        val recursiveScanning = root["recursive_scanning"]?.jsonPrimitive?.booleanOrNull
            ?: root["recursiveScanning"]?.jsonPrimitive?.booleanOrNull
            ?: false

        val entriesElement = root["entries"] ?: return null
        val entries = when (entriesElement) {
            is JsonArray -> entriesElement.mapNotNull { parseWorldInfoEntry(it.jsonObject) }
            is JsonObject -> entriesElement.values.mapNotNull { parseWorldInfoEntry(it.jsonObject) }
            else -> emptyList()
        }

        return CharacterBook(
            name              = name,
            description       = description,
            scanDepth         = scanDepth,
            tokenBudget       = tokenBudget,
            recursiveScanning = recursiveScanning,
            entries           = entries
        )
    }

    private fun parseWorldInfoEntry(obj: JsonObject): WorldInfoEntry? {
        return try {
            val ext = obj["extensions"]?.jsonObject

            WorldInfoEntry(
                uid = obj["uid"]?.jsonPrimitive?.intOrNull
                    ?: obj["id"]?.jsonPrimitive?.intOrNull ?: 0,
                key = obj["keys"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: obj["key"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: emptyList(),
                keysecondary = obj["secondary_keys"]?.jsonArray
                    ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: obj["keysecondary"]?.jsonArray
                        ?.mapNotNull { it.jsonPrimitive.contentOrNull }
                    ?: emptyList(),
                comment = obj["comment"]?.jsonPrimitive?.content
                    ?: obj["name"]?.jsonPrimitive?.content ?: "",
                content = obj["content"]?.jsonPrimitive?.content ?: "",
                order = obj["order"]?.jsonPrimitive?.intOrNull
                    ?: obj["insertion_order"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("order")?.jsonPrimitive?.intOrNull ?: 100,
                position = WorldInfoPosition.fromValue(
                    parsePositionValue(
                        obj["position"] ?: ext?.get("position")
                    )
                ),
                disable = obj["disable"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["enabled"]?.jsonPrimitive?.booleanOrNull?.not()
                    ?: false,
                selectiveLogic = SelectiveLogic.fromValue(
                    obj["selectiveLogic"]?.jsonPrimitive?.intOrNull
                        ?: obj["selective_logic"]?.jsonPrimitive?.intOrNull
                        ?: ext?.get("selectiveLogic")?.jsonPrimitive?.intOrNull ?: 0
                ),
                constant = obj["constant"]?.jsonPrimitive?.booleanOrNull
                    ?: ext?.get("constant")?.jsonPrimitive?.booleanOrNull ?: false,
                depth = obj["depth"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("depth")?.jsonPrimitive?.intOrNull ?: 4,
                scanDepth = obj["scanDepth"]?.jsonPrimitive?.intOrNull
                    ?: obj["scan_depth"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("scan_depth")?.jsonPrimitive?.intOrNull,
                caseSensitive = obj["caseSensitive"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["case_sensitive"]?.jsonPrimitive?.booleanOrNull
                    ?: ext?.get("case_sensitive")?.jsonPrimitive?.booleanOrNull,
                matchWholeWords = obj["matchWholeWords"]?.jsonPrimitive?.booleanOrNull
                    ?: obj["match_whole_words"]?.jsonPrimitive?.booleanOrNull
                    ?: ext?.get("match_whole_words")?.jsonPrimitive?.booleanOrNull,
                useRegex = obj["use_regex"]?.jsonPrimitive?.booleanOrNull
                    ?: ext?.get("use_regex")?.jsonPrimitive?.booleanOrNull,
                automationId = obj["automationId"]?.jsonPrimitive?.content
                    ?: obj["automation_id"]?.jsonPrimitive?.content
                    ?: ext?.get("automation_id")?.jsonPrimitive?.content ?: "",
                role = obj["role"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("role")?.jsonPrimitive?.intOrNull,
                sticky = obj["sticky"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("sticky")?.jsonPrimitive?.intOrNull,
                cooldown = obj["cooldown"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("cooldown")?.jsonPrimitive?.intOrNull,
                delay = obj["delay"]?.jsonPrimitive?.intOrNull
                    ?: ext?.get("delay")?.jsonPrimitive?.intOrNull,
                vectorized = obj["vectorized"]?.jsonPrimitive?.booleanOrNull
                    ?: ext?.get("vectorized")?.jsonPrimitive?.booleanOrNull ?: false,
                extensions = ext?.mapValues { it.value.toString() } ?: emptyMap()
            )
        } catch (e: Exception) {
            Timber.w(e, "Failed to parse world info entry")
            null
        }
    }

    private fun roleFromEntry(entry: WorldInfoEntry): MessageRole {
        return when (entry.role) {
            0 -> MessageRole.SYSTEM
            1 -> MessageRole.USER
            2 -> MessageRole.ASSISTANT
            else -> MessageRole.SYSTEM
        }
    }

    private fun parsePositionValue(element: JsonElement?): Int {
        val primitive = element?.jsonPrimitive ?: return 0
        return primitive.intOrNull ?: when (primitive.contentOrNull) {
            "after_char" -> WorldInfoPosition.AFTER.value
            "before_char" -> WorldInfoPosition.BEFORE.value
            else -> WorldInfoPosition.BEFORE.value
        }
    }

    private fun cleanContent(content: String): String =
        content.lineSequence()
            .dropWhile {
                val line = it.trim()
                line.startsWith("@@activate") || line.startsWith("@@dont_activate")
            }
            .joinToString("\n")
            .trimStart()
}