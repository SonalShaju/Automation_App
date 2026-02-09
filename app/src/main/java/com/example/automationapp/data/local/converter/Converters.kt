package com.example.automationapp.data.local.converter

import androidx.room.TypeConverter
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.data.local.entity.ConditionType
import com.example.automationapp.data.local.entity.LogicalOperator
import com.example.automationapp.data.local.entity.ExecutionStatus

class Converters {

    // TriggerType converters
    @TypeConverter
    fun fromTriggerType(value: TriggerType): String {
        return value.name
    }

    @TypeConverter
    fun toTriggerType(value: String): TriggerType {
        return try {
            TriggerType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            TriggerType.TIME_BASED // Default fallback
        }
    }

    // ActionType converters
    @TypeConverter
    fun fromActionType(value: ActionType): String {
        return value.name
    }

    @TypeConverter
    fun toActionType(value: String): ActionType {
        return try {
            ActionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ActionType.SEND_NOTIFICATION // Default fallback
        }
    }

    // LogicalOperator converters
    @TypeConverter
    fun fromLogicalOperator(value: LogicalOperator): String {
        return value.name
    }

    @TypeConverter
    fun toLogicalOperator(value: String): LogicalOperator {
        return try {
            LogicalOperator.valueOf(value)
        } catch (e: IllegalArgumentException) {
            LogicalOperator.AND // Default fallback
        }
    }

    // ExecutionStatus converters
    @TypeConverter
    fun fromExecutionStatus(value: ExecutionStatus): String {
        return value.name
    }

    @TypeConverter
    fun toExecutionStatus(value: String): ExecutionStatus {
        return try {
            ExecutionStatus.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ExecutionStatus.FAILED // Default fallback
        }
    }

    // ConditionType converters
    @TypeConverter
    fun fromConditionType(value: ConditionType): String {
        return value.name
    }

    @TypeConverter
    fun toConditionType(value: String): ConditionType {
        return try {
            ConditionType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            ConditionType.BATTERY_LEVEL // Default fallback
        }
    }

    // List<String> converters (for future use)
    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isBlank()) emptyList() else value.split(",")
    }

    // List<Long> converters (for future use)
    @TypeConverter
    fun fromLongList(value: List<Long>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String): List<Long> {
        return if (value.isBlank()) {
            emptyList()
        } else {
            value.split(",").mapNotNull { it.toLongOrNull() }
        }
    }
}