package com.beichenxing.aurora.domain.model

data class Chat(
    val id: String,
    val characterId: String,
    val title: String = "New Chat",
    val authorNote: String = "",
    val authorNoteDepth: Int = 4,
    val authorNoteEnabled: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)