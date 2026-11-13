package com.beichenxing.aurora.data.repository

import com.beichenxing.aurora.data.local.dao.CharacterDao
import com.beichenxing.aurora.data.local.entity.toDomain
import com.beichenxing.aurora.data.local.entity.toEntity
import com.beichenxing.aurora.domain.model.Character
import com.beichenxing.aurora.domain.repository.CharacterRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CharacterRepositoryImpl @Inject constructor(
    private val dao: CharacterDao
) : CharacterRepository {

    override fun observeAll(): Flow<List<Character>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Character> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): Character? =
        dao.getById(id)?.toDomain()

    override suspend fun search(query: String): List<Character> =
        dao.search(query).map { it.toDomain() }

    override suspend fun getFavorites(): List<Character> =
        dao.getFavorites().map { it.toDomain() }

    override suspend fun insert(character: Character) =
        dao.insert(character.toEntity())

    override suspend fun update(character: Character) =
        dao.update(character.toEntity())

    override suspend fun deleteById(id: String) =
        dao.deleteById(id)

    override suspend fun setFavorite(id: String, isFavorite: Boolean) =
        dao.setFavorite(id, isFavorite, System.currentTimeMillis())

    override suspend fun setPinned(id: String, isPinned: Boolean) =
        dao.setPinned(id, isPinned, System.currentTimeMillis())

    override suspend fun updateAvatarPath(id: String, path: String) =
        dao.updateAvatarPath(id, path, System.currentTimeMillis())

    override suspend fun count(): Int = dao.count()
}