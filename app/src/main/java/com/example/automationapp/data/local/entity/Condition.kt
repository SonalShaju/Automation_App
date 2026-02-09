package com.example.automationapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a state check condition that must be true for a rule to execute.
 * Unlike Triggers (which are events that start the evaluation), Conditions are
 * state checks that are evaluated when a Trigger fires. ALL conditions must
 * be satisfied (AND logic) for the rule to execute.
 */
@Entity(
    tableName = "conditions",
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
data class Condition(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "rule_id")
    val ruleId: Long,

    @ColumnInfo(name = "type")
    val type: ConditionType,

    @ColumnInfo(name = "parameters")
    val parameters: String, // JSON string

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Types of conditions that can be checked.
 * These represent state checks, not events.
 */
enum class ConditionType {
    // ==================== TIME & LOCATION ====================
    TIME_RANGE,        // Check if current time is within a range
    LOCATION_BASED,    // Check if device is at a specific location

    // ==================== BATTERY ====================
    BATTERY_LEVEL,     // Check current battery level
    CHARGING_STATUS,   // Check if device is charging

    // ==================== CONNECTIVITY ====================
    WIFI_CONNECTED,    // Check if connected to specific WiFi
    BLUETOOTH_CONNECTED, // Check if Bluetooth is enabled
    NETWORK_TYPE,      // Check current network type
    AIRPLANE_MODE,     // Check if airplane mode is on/off

    // ==================== DEVICE STATE ====================
    HEADPHONES_CONNECTED, // Check if headphones are connected
    SCREEN_STATE,      // Check if screen is on/off
    DO_NOT_DISTURB     // Check if DND is enabled
}

