package com.example.automationapp.di

import com.example.automationapp.data.repository.AutomationRepositoryImpl
import com.example.automationapp.domain.repository.AutomationRepository
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
    abstract fun bindAutomationRepository(
        impl: AutomationRepositoryImpl
    ): AutomationRepository
}