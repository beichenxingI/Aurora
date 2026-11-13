package com.beichenxing.aurora.core.di

import android.content.Context
import androidx.room.Room
import com.beichenxing.aurora.data.local.dao.CharacterDao
import com.beichenxing.aurora.data.local.dao.ChatDao
import com.beichenxing.aurora.data.local.dao.LlmConfigDao
import com.beichenxing.aurora.data.local.dao.MessageDao
import com.beichenxing.aurora.data.local.dao.PersonaDao
import com.beichenxing.aurora.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "aurora.db"
        )
            .fallbackToDestructiveMigration() // Phase 2: destructive migration OK (no user data yet)
            .build()
    }

    @Provides
    fun provideCharacterDao(db: AppDatabase): CharacterDao = db.characterDao()

    @Provides
    fun provideChatDao(db: AppDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideMessageDao(db: AppDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideLlmConfigDao(db: AppDatabase): LlmConfigDao = db.llmConfigDao()

    @Provides
    fun providePersonaDao(db: AppDatabase): PersonaDao = db.personaDao()
}