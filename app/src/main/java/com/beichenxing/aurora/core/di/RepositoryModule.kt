package com.beichenxing.aurora.core.di

import com.beichenxing.aurora.data.repository.CharacterRepositoryImpl
import com.beichenxing.aurora.data.repository.ChatRepositoryImpl
import com.beichenxing.aurora.data.repository.LlmConfigRepositoryImpl
import com.beichenxing.aurora.domain.repository.CharacterRepository
import com.beichenxing.aurora.domain.repository.ChatRepository
import com.beichenxing.aurora.domain.repository.LlmConfigRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCharacterRepository(
        impl: CharacterRepositoryImpl
    ): CharacterRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindLlmConfigRepository(
        impl: LlmConfigRepositoryImpl
    ): LlmConfigRepository
}