package com.example.automationapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "triggers",
    foreignKeys = [
        ForeignKey(
            entity = AutomationRule::class,
            parentColumns = ["id"],
            childColumns = ["rule_id"],
            onDelete = ForeignKey.CASCADE,
            onUpdate = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["rule_id"]),
        Index(value = ["type"])
    ]
)
data class Trigger(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "rule_id")
    val ruleId: Long,

    @ColumnInfo(name = "type")
    val type: TriggerType,

    @ColumnInfo(name = "parameters")
    val parameters: String, // JSON string

    @ColumnInfo(name = "logical_operator")
    val logicalOperator: LogicalOperator = LogicalOperator.AND,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class TriggerType {
    // ==================== TIME & LOCATION ====================
    TIME_BASED,
    TIME_RANGE,
    LOCATION_BASED,

    // ==================== BATTERY ====================
    BATTERY_LEVEL,
    CHARGING_STATUS,

    // ==================== CONNECTIVITY ====================
    WIFI_CONNECTED,
    BLUETOOTH_CONNECTED,
    AIRPLANE_MODE,

    // ==================== DEVICE STATE ====================
    HEADPHONES_CONNECTED,
    DO_NOT_DISTURB,

    // ==================== APPS ====================
    APP_OPENED
}

enum class LogicalOperator {
    AND,
    OR
}