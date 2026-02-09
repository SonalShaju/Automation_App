package com.example.automationapp.data.local.database


import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.automationapp.data.local.converter.Converters
import com.example.automationapp.data.local.dao.*
import com.example.automationapp.data.local.entity.*

@Database(
    entities = [
        AutomationRule::class,
        Trigger::class,
        Action::class,
        ExecutionLog::class,
        Condition::class
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = []
)
@TypeConverters(Converters::class)
abstract class AutomationDatabase : RoomDatabase() {
    abstract fun automationRuleDao(): AutomationRuleDao
    abstract fun triggerDao(): TriggerDao
    abstract fun actionDao(): ActionDao
    abstract fun executionLogDao(): ExecutionLogDao
    abstract fun automationDao(): AutomationDao
    abstract fun conditionDao(): ConditionDao

    companion object {
        const val DATABASE_NAME = "automation_database"
    }
}