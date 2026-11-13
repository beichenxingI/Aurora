package com.beichenxing.aurora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beichenxing.aurora.data.local.entity.PersonaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PersonaDao {

    @Query("SELECT * FROM personas ORDER BY is_default DESC, name ASC")
    fun observeAll(): Flow<List<PersonaEntity>>

    @Query("SELECT * FROM personas ORDER BY is_default DESC, name ASC")
    suspend fun getAll(): List<PersonaEntity>

    @Query("SELECT * FROM personas WHERE id = :id")
    suspend fun getById(id: String): PersonaEntity?

    @Query("SELECT * FROM personas WHERE is_default = 1 LIMIT 1")
    suspend fun getDefault(): PersonaEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(persona: PersonaEntity)

    @Update
    suspend fun update(persona: PersonaEntity)

    @Delete
    suspend fun delete(persona: PersonaEntity)

    @Query("DELETE FROM personas WHERE id = :id")
    suspend fun deleteById(id: String)

    // Must be called inside a transaction
    @Query("UPDATE personas SET is_default = 0")
    suspend fun clearDefault()

    @Query("UPDATE personas SET is_default = 1, updated_at = :now WHERE id = :id")
    suspend fun setDefault(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM personas")
    suspend fun count(): Int
}