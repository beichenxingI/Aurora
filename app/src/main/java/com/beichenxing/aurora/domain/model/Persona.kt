package com.beichenxing.aurora.domain.model

data class Persona(
    val id: String,
    val name: String,
    val description: String = "",
    val avatarPath: String? = null,
    val isDefault: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)