package com.beichenxing.aurora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "llm_configs")
data class LlmConfigEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    // Provider identifier: "openai" / "openai_compatible" / "claude" / "gemini" /
    // "deepseek" / "qwen" / "openrouter"
    @ColumnInfo(name = "provider")
    val provider: String,

    @ColumnInfo(name = "endpoint")
    val endpoint: String,

    @ColumnInfo(name = "api_key")
    val apiKey: String = "",

    @ColumnInfo(name = "model")
    val model: String = "",

    // Only one row should have is_active = true at any time
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = false,

    // Sampling parameters
    @ColumnInfo(name = "max_tokens")
    val maxTokens: Int = 8192,

    @ColumnInfo(name = "context_length")
    val contextLength: Int = 100000,

    @ColumnInfo(name = "temperature")
    val temperature: Double = 1.0,

    @ColumnInfo(name = "top_p")
    val topP: Double = 0.95,

    @ColumnInfo(name = "top_k")
    val topK: Int = 40,

    @ColumnInfo(name = "min_p")
    val minP: Double = 0.0,

    @ColumnInfo(name = "frequency_penalty")
    val frequencyPenalty: Double = 0.0,

    @ColumnInfo(name = "presence_penalty")
    val presencePenalty: Double = 0.0,

    @ColumnInfo(name = "repetition_penalty")
    val repetitionPenalty: Double = 1.0,

    @ColumnInfo(name = "stop_sequences_json")
    val stopSequencesJson: String = "[]", // JSON array of strings

    @ColumnInfo(name = "stream_enabled")
    val streamEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp ms

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long // Unix timestamp ms
)