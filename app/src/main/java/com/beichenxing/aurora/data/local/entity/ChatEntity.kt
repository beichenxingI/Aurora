package com.beichenxing.aurora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "chats",
    foreignKeys = [
        ForeignKey(
            entity = CharacterEntity::class,
            parentColumns = ["id"],
            childColumns = ["character_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["character_id"])]
)
data class ChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "character_id")
    val characterId: String,

    @ColumnInfo(name = "title")
    val title: String = "New Chat",

    @ColumnInfo(name = "author_note")
    val authorNote: String = "",

    @ColumnInfo(name = "author_note_depth")
    val authorNoteDepth: Int = 4,

    @ColumnInfo(name = "author_note_enabled")
    val authorNoteEnabled: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp ms

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long // Unix timestamp ms
)