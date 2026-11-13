package com.beichenxing.aurora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beichenxing.aurora.data.local.entity.CharacterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CharacterDao {

    @Query("SELECT * FROM characters ORDER BY is_pinned DESC, updated_at DESC")
    fun observeAll(): Flow<List<CharacterEntity>>

    @Query("SELECT * FROM characters ORDER BY is_pinned DESC, updated_at DESC")
    suspend fun getAll(): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE id = :id")
    suspend fun getById(id: String): CharacterEntity?

    @Query("SELECT * FROM characters WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun search(query: String): List<CharacterEntity>

    @Query("SELECT * FROM characters WHERE is_favorite = 1 ORDER BY updated_at DESC")
    suspend fun getFavorites(): List<CharacterEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(character: CharacterEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(characters: List<CharacterEntity>)

    @Update
    suspend fun update(character: CharacterEntity)

    @Delete
    suspend fun delete(character: CharacterEntity)

    @Query("DELETE FROM characters WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE characters SET is_favorite = :isFavorite, updated_at = :now WHERE id = :id")
    suspend fun setFavorite(id: String, isFavorite: Boolean, now: Long)

    @Query("UPDATE characters SET is_pinned = :isPinned, updated_at = :now WHERE id = :id")
    suspend fun setPinned(id: String, isPinned: Boolean, now: Long)

    @Query("UPDATE characters SET avatar_path = :path, updated_at = :now WHERE id = :id")
    suspend fun updateAvatarPath(id: String, path: String, now: Long)

    @Query("SELECT COUNT(*) FROM characters")
    suspend fun count(): Int
}