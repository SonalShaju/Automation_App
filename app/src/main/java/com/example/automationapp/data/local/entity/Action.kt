package com.example.automationapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "actions",
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
        Index(value = ["type"]),
        Index(value = ["rule_id", "sequence"])
    ]
)
data class Action(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "rule_id")
    val ruleId: Long,

    @ColumnInfo(name = "type")
    val type: ActionType,

    @ColumnInfo(name = "parameters")
    val parameters: String, // JSON string

    @ColumnInfo(name = "sequence")
    val sequence: Int = 0,


    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class ActionType {
    // ==================== HARDWARE ====================
    TOGGLE_FLASHLIGHT,
    ENABLE_FLASHLIGHT,
    DISABLE_FLASHLIGHT,
    VIBRATE,

    // ==================== AUDIO ====================
    ADJUST_VOLUME,
    SET_RINGER_MODE,
    TOGGLE_SILENT_MODE,
    TOGGLE_VIBRATE,

    // ==================== DISPLAY (Settings.System) ====================
    ADJUST_BRIGHTNESS,
    TOGGLE_AUTO_BRIGHTNESS,
    TOGGLE_AUTO_ROTATE,

    // ==================== SYSTEM (AccessibilityService Global Actions) ====================
    GLOBAL_ACTION_LOCK_SCREEN,
    GLOBAL_ACTION_TAKE_SCREENSHOT,
    GLOBAL_ACTION_POWER_DIALOG,

    // ==================== APPS ====================
    LAUNCH_APP,
    BLOCK_APP,

    // ==================== NOTIFICATIONS ====================
    SEND_NOTIFICATION,
    CLEAR_NOTIFICATIONS,

    // ==================== DND (Native API) ====================
    ENABLE_DND,
    DISABLE_DND
}