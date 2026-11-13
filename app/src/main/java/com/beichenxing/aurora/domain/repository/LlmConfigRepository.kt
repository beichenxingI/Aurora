package com.beichenxing.aurora.domain.repository

import com.beichenxing.aurora.domain.model.LlmConfig
import kotlinx.coroutines.flow.Flow

interface LlmConfigRepository {
    fun observeAll(): Flow<List<LlmConfig>>
    fun observeActive(): Flow<LlmConfig?>
    suspend fun getAll(): List<LlmConfig>
    suspend fun getById(id: String): LlmConfig?
    suspend fun getActive(): LlmConfig?
    suspend fun insert(config: LlmConfig)
    suspend fun update(config: LlmConfig)
    suspend fun deleteById(id: String)
    suspend fun setActive(id: String)
}