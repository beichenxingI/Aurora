package com.beichenxing.aurora.domain.repository

import com.beichenxing.aurora.domain.model.Character
import kotlinx.coroutines.flow.Flow

interface CharacterRepository {
    fun observeAll(): Flow<List<Character>>
    suspend fun getAll(): List<Character>
    suspend fun getById(id: String): Character?
    suspend fun search(query: String): List<Character>
    suspend fun getFavorites(): List<Character>
    suspend fun insert(character: Character)
    suspend fun update(character: Character)
    suspend fun deleteById(id: String)
    suspend fun setFavorite(id: String, isFavorite: Boolean)
    suspend fun setPinned(id: String, isPinned: Boolean)
    suspend fun updateAvatarPath(id: String, path: String)
    suspend fun count(): Int
}