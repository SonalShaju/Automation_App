package com.example.automationapp.di

import android.content.Context
import androidx.room.Room
import com.example.automationapp.data.local.database.AutomationDatabase
import com.example.automationapp.data.local.dao.ActionDao
import com.example.automationapp.data.local.dao.ConditionDao
import com.example.automationapp.data.local.MIGRATION_1_2
import com.example.automationapp.data.local.MIGRATION_2_3
import com.example.automationapp.data.local.dao.AutomationRuleDao
import com.example.automationapp.data.local.dao.ExecutionLogDao
import com.example.automationapp.data.local.dao.TriggerDao
import com.example.automationapp.data.local.dao.AutomationDao
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
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AutomationDatabase {
        return Room.databaseBuilder(
            context,
            AutomationDatabase::class.java,
            "automation_database"
        )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    @Provides
    @Singleton
    fun provideAutomationRuleDao(database: AutomationDatabase): AutomationRuleDao {
        return database.automationRuleDao()
    }

    @Provides
    @Singleton
    fun provideTriggerDao(database: AutomationDatabase): TriggerDao {
        return database.triggerDao()
    }

    @Provides
    @Singleton
    fun provideActionDao(database: AutomationDatabase): ActionDao {
        return database.actionDao()
    }

    @Provides
    @Singleton
    fun provideExecutionLogDao(database: AutomationDatabase): ExecutionLogDao {
        return database.executionLogDao()
    }

    @Provides
    @Singleton
    fun provideAutomationDao(database: AutomationDatabase): AutomationDao {
        return database.automationDao()
    }

    @Provides
    @Singleton
    fun provideConditionDao(database: AutomationDatabase): ConditionDao {
        return database.conditionDao()
    }
}