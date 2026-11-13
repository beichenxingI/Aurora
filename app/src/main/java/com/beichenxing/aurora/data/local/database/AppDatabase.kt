package com.beichenxing.aurora.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.beichenxing.aurora.data.local.dao.CharacterDao
import com.beichenxing.aurora.data.local.dao.ChatDao
import com.beichenxing.aurora.data.local.dao.LlmConfigDao
import com.beichenxing.aurora.data.local.dao.MessageDao
import com.beichenxing.aurora.data.local.dao.PersonaDao
import com.beichenxing.aurora.data.local.entity.CharacterEntity
import com.beichenxing.aurora.data.local.entity.ChatEntity
import com.beichenxing.aurora.data.local.entity.LlmConfigEntity
import com.beichenxing.aurora.data.local.entity.MessageEntity
import com.beichenxing.aurora.data.local.entity.PersonaEntity

@Database(
    entities = [
        CharacterEntity::class,
        ChatEntity::class,
        MessageEntity::class,
        LlmConfigEntity::class,
        PersonaEntity::class,
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun characterDao(): CharacterDao
    abstract fun chatDao(): ChatDao
    abstract fun messageDao(): MessageDao
    abstract fun llmConfigDao(): LlmConfigDao
    abstract fun personaDao(): PersonaDao
}