package com.beichenxing.aurora.data.repository

import androidx.room.withTransaction
import com.beichenxing.aurora.data.local.dao.LlmConfigDao
import com.beichenxing.aurora.data.local.database.AppDatabase
import com.beichenxing.aurora.data.local.entity.toDomain
import com.beichenxing.aurora.data.local.entity.toEntity
import com.beichenxing.aurora.domain.model.LlmConfig
import com.beichenxing.aurora.domain.repository.LlmConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LlmConfigRepositoryImpl @Inject constructor(
    private val dao: LlmConfigDao,
    private val db: AppDatabase
) : LlmConfigRepository {

    override fun observeAll(): Flow<List<LlmConfig>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeActive(): Flow<LlmConfig?> =
        dao.observeActive().map { it?.toDomain() }

    override suspend fun getAll(): List<LlmConfig> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getById(id: String): LlmConfig? =
        dao.getById(id)?.toDomain()

    override suspend fun getActive(): LlmConfig? =
        dao.getActive()?.toDomain()

    override suspend fun insert(config: LlmConfig) =
        dao.insert(config.toEntity())

    override suspend fun update(config: LlmConfig) =
        dao.update(config.toEntity())

    override suspend fun deleteById(id: String) =
        dao.deleteById(id)

    override suspend fun setActive(id: String) {
        // Transaction ensures only one config is active at a time
        db.withTransaction {
            dao.clearActive()
            dao.setActive(id, System.currentTimeMillis())
        }
    }
}