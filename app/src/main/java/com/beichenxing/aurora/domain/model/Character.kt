package com.beichenxing.aurora.domain.model

data class Character(
    val id: String,
    val name: String,
    val description: String = "",
    val personality: String = "",
    val scenario: String = "",
    val firstMessage: String = "",
    val alternateGreetings: List<String> = emptyList(),
    val exampleDialogue: String = "",
    val systemPrompt: String = "",
    val postHistoryInstructions: String = "",
    val creatorNotes: String = "",
    val creator: String = "",
    val characterVersion: String = "",
    val tags: List<String> = emptyList(),
    val avatarPath: String? = null,
    val extensionsJson: String = "{}",
    val characterBookJson: String = "",
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)