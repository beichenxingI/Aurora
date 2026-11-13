package com.beichenxing.aurora.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "personas")
data class PersonaEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "avatar_path")
    val avatarPath: String? = null,

    // Only one row should have is_default = true at any time
    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "created_at")
    val createdAt: Long, // Unix timestamp ms

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long // Unix timestamp ms
)