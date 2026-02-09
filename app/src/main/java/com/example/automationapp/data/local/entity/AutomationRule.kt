package com.example.automationapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "automation_rules",
    indices = [Index(value = ["name"], unique = true)]
)
data class AutomationRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "description")
    val description: String,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "execution_count")
    val executionCount: Int = 0,

    @ColumnInfo(name = "last_executed_at")
    val lastExecutedAt: Long? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    // Exit Action for Time Range triggers - executes when time range ends
    @ColumnInfo(name = "exit_action_type")
    val exitActionType: ActionType? = null,

    @ColumnInfo(name = "exit_action_params")
    val exitActionParams: String? = null
)