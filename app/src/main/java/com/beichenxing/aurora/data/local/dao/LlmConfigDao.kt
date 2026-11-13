package com.beichenxing.aurora.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.beichenxing.aurora.data.local.entity.LlmConfigEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LlmConfigDao {

    @Query("SELECT * FROM llm_configs ORDER BY created_at DESC")
    fun observeAll(): Flow<List<LlmConfigEntity>>

    @Query("SELECT * FROM llm_configs ORDER BY created_at DESC")
    suspend fun getAll(): List<LlmConfigEntity>

    @Query("SELECT * FROM llm_configs WHERE id = :id")
    suspend fun getById(id: String): LlmConfigEntity?

    @Query("SELECT * FROM llm_configs WHERE is_active = 1 LIMIT 1")
    suspend fun getActive(): LlmConfigEntity?

    @Query("SELECT * FROM llm_configs WHERE is_active = 1 LIMIT 1")
    fun observeActive(): Flow<LlmConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: LlmConfigEntity)

    @Update
    suspend fun update(config: LlmConfigEntity)

    @Delete
    suspend fun delete(config: LlmConfigEntity)

    @Query("DELETE FROM llm_configs WHERE id = :id")
    suspend fun deleteById(id: String)

    // Set all configs to inactive, then set the specified one as active
    // Must be called inside a transaction
    @Query("UPDATE llm_configs SET is_active = 0")
    suspend fun clearActive()

    @Query("UPDATE llm_configs SET is_active = 1, updated_at = :now WHERE id = :id")
    suspend fun setActive(id: String, now: Long)

    @Query("SELECT COUNT(*) FROM llm_configs")
    suspend fun count(): Int
}