package com.beichenxing.aurora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chat_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["chat_id"])]
)
data class MessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "role")
    val role: String, // "user" / "assistant" / "system"

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "reasoning")
    val reasoning: String? = null, // chain-of-thought content (persisted)

    @ColumnInfo(name = "swipes_json")
    val swipesJson: String = "[]", // JSON array of string (alternative generations)

    @ColumnInfo(name = "swipes_reasoning_json")
    val swipesReasoningJson: String = "[]", // JSON array of reasoning per swipe

    @ColumnInfo(name = "current_swipe_index")
    val currentSwipeIndex: Int = 0,

    @ColumnInfo(name = "is_hidden")
    val isHidden: Boolean = false, // hidden from prompt context

    @ColumnInfo(name = "attachments_json")
    val attachmentsJson: String = "[]", // JSON array of attachment objects

    @ColumnInfo(name = "character_id")
    val characterId: String? = null, // group chat: which character sent this

    @ColumnInfo(name = "character_name")
    val characterName: String? = null, // group chat: cached character name

    @ColumnInfo(name = "created_at")
    val createdAt: Long // Unix timestamp ms
)