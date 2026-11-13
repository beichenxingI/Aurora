package com.beichenxing.aurora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "characters")
data class CharacterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "personality")
    val personality: String = "",

    @ColumnInfo(name = "scenario")
    val scenario: String = "",

    @ColumnInfo(name = "first_message")
    val firstMessage: String = "",

    @ColumnInfo(name = "alternate_greetings")
    val alternateGreetings: String = "[]", // JSON array of strings

    @ColumnInfo(name = "example_dialogue")
    val exampleDialogue: String = "",

    @ColumnInfo(name = "system_prompt")
    val systemPrompt: String = "",

    @ColumnInfo(name = "post_history_instructions")
    val postHistoryInstructions: String = "",

    @ColumnInfo(name = "creator_notes")
    val creatorNotes: String = "",

    @ColumnInfo(name = "creator")
    val creator: String = "",

    @ColumnInfo(name = "character_version")
    val characterVersion: String = "",

    @ColumnInfo(name = "tags")
    val tags: String = "[]", // JSON array of strings

    @ColumnInfo(name = "avatar_path")
    val avatarPath: String? = null,

    @ColumnInfo(name = "extensions_json")
    val extensionsJson: String = "{}", // ST-compatible extensions

    @ColumnInfo(name = "character_book_json")
    val characterBookJson: String = "", // embedded lorebook snapshot

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp ms

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long // Unix timestamp ms
)