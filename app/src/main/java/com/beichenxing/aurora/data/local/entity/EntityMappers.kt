package com.beichenxing.aurora.data.local.entity

import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.model.Chat
import com.beichenxing.aurora.domain.model.LlmConfig
import com.beichenxing.aurora.domain.model.LlmProvider
import com.beichenxing.aurora.domain.model.Message
import com.beichenxing.aurora.domain.model.MessageAttachment
import com.beichenxing.aurora.domain.model.MessageRole
import com.beichenxing.aurora.domain.model.Persona
import kotlinx.serialization.json.Json
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer

private val json = Json { ignoreUnknownKeys = true; isLenient = true }

// ─── Character ───────────────────────────────────────────────────────────────

fun CharacterEntity.toDomain(): Character = Character(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    firstMessage = firstMessage,
    alternateGreetings = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), alternateGreetings)
    }.getOrDefault(emptyList()),
    exampleDialogue = exampleDialogue,
    systemPrompt = systemPrompt,
    postHistoryInstructions = postHistoryInstructions,
    creatorNotes = creatorNotes,
    creator = creator,
    characterVersion = characterVersion,
    tags = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), tags)
    }.getOrDefault(emptyList()),
    avatarPath = avatarPath,
    extensionsJson = extensionsJson,
    characterBookJson = characterBookJson,
    isFavorite = isFavorite,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Character.toEntity(): CharacterEntity = CharacterEntity(
    id = id,
    name = name,
    description = description,
    personality = personality,
    scenario = scenario,
    firstMessage = firstMessage,
    alternateGreetings = runCatching {
        json.encodeToString(ListSerializer(String.serializer()), alternateGreetings)
    }.getOrDefault("[]"),
    exampleDialogue = exampleDialogue,
    systemPrompt = systemPrompt,
    postHistoryInstructions = postHistoryInstructions,
    creatorNotes = creatorNotes,
    creator = creator,
    characterVersion = characterVersion,
    tags = runCatching {
        json.encodeToString(ListSerializer(String.serializer()), tags)
    }.getOrDefault("[]"),
    avatarPath = avatarPath,
    extensionsJson = extensionsJson,
    characterBookJson = characterBookJson,
    isFavorite = isFavorite,
    isPinned = isPinned,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ─── Chat ─────────────────────────────────────────────────────────────────────

fun ChatEntity.toDomain(): Chat = Chat(
    id = id,
    characterId = characterId,
    title = title,
    authorNote = authorNote,
    authorNoteDepth = authorNoteDepth,
    authorNoteEnabled = authorNoteEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Chat.toEntity(): ChatEntity = ChatEntity(
    id = id,
    characterId = characterId,
    title = title,
    authorNote = authorNote,
    authorNoteDepth = authorNoteDepth,
    authorNoteEnabled = authorNoteEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ─── Message ──────────────────────────────────────────────────────────────────

fun MessageEntity.toDomain(): Message {
    val swipes = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), swipesJson)
    }.getOrDefault(emptyList())

    val swipesReasoning = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), swipesReasoningJson)
    }.getOrDefault(emptyList())

    val attachments = runCatching {
        json.decodeFromString(
            ListSerializer(MessageAttachment.serializer()),
            attachmentsJson
        )
    }.getOrDefault(emptyList())

    return Message(
        id = id,
        chatId = chatId,
        role = MessageRole.fromString(role),
        content = content,
        reasoning = reasoning,
        swipes = swipes,
        swipesReasoning = swipesReasoning,
        currentSwipeIndex = currentSwipeIndex,
        isHidden = isHidden,
        attachments = attachments,
        characterId = characterId,
        characterName = characterName,
        createdAt = createdAt
    )
}

fun Message.toEntity(): MessageEntity {
    val swipesJson = runCatching {
        json.encodeToString(ListSerializer(String.serializer()), swipes)
    }.getOrDefault("[]")

    val swipesReasoningJson = runCatching {
        json.encodeToString(ListSerializer(String.serializer()), swipesReasoning)
    }.getOrDefault("[]")

    val attachmentsJson = runCatching {
        json.encodeToString(ListSerializer(MessageAttachment.serializer()), attachments)
    }.getOrDefault("[]")

    return MessageEntity(
        id = id,
        chatId = chatId,
        role = role.value,
        content = content,
        reasoning = reasoning,
        swipesJson = swipesJson,
        swipesReasoningJson = swipesReasoningJson,
        currentSwipeIndex = currentSwipeIndex,
        isHidden = isHidden,
        attachmentsJson = attachmentsJson,
        characterId = characterId,
        characterName = characterName,
        createdAt = createdAt
    )
}

// ─── LlmConfig ────────────────────────────────────────────────────────────────

fun LlmConfigEntity.toDomain(): LlmConfig {
    val stopSequences = runCatching {
        json.decodeFromString(ListSerializer(String.serializer()), stopSequencesJson)
    }.getOrDefault(emptyList())

    return LlmConfig(
        id = id,
        name = name,
        provider = LlmProvider.fromId(provider),
        endpoint = endpoint,
        apiKey = apiKey,
        model = model,
        isActive = isActive,
        maxTokens = maxTokens,
        contextLength = contextLength,
        temperature = temperature,
        topP = topP,
        topK = topK,
        minP = minP,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        repetitionPenalty = repetitionPenalty,
        stopSequences = stopSequences,
        streamEnabled = streamEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

fun LlmConfig.toEntity(): LlmConfigEntity {
    val stopSequencesJson = runCatching {
        json.encodeToString(ListSerializer(String.serializer()), stopSequences)
    }.getOrDefault("[]")

    return LlmConfigEntity(
        id = id,
        name = name,
        provider = provider.id,
        endpoint = endpoint,
        apiKey = apiKey,
        model = model,
        isActive = isActive,
        maxTokens = maxTokens,
        contextLength = contextLength,
        temperature = temperature,
        topP = topP,
        topK = topK,
        minP = minP,
        frequencyPenalty = frequencyPenalty,
        presencePenalty = presencePenalty,
        repetitionPenalty = repetitionPenalty,
        stopSequencesJson = stopSequencesJson,
        streamEnabled = streamEnabled,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
}

// ─── Persona ──────────────────────────────────────────────────────────────────

fun PersonaEntity.toDomain(): Persona = Persona(
    id = id,
    name = name,
    description = description,
    avatarPath = avatarPath,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Persona.toEntity(): PersonaEntity = PersonaEntity(
    id = id,
    name = name,
    description = description,
    avatarPath = avatarPath,
    isDefault = isDefault,
    createdAt = createdAt,
    updatedAt = updatedAt
)