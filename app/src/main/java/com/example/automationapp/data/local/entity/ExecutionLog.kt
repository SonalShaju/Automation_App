package com.example.automationapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "execution_logs",
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
        Index(value = ["executed_at"]),
        Index(value = ["success"]),
        Index(value = ["rule_id", "executed_at"])
    ]
)
data class ExecutionLog(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "rule_id")
    val ruleId: Long,

    @ColumnInfo(name = "executed_at")
    val executedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "success")
    val success: Boolean,

    @ColumnInfo(name = "status")
    val status: ExecutionStatus,

    @ColumnInfo(name = "error_message")
    val errorMessage: String? = null,

    @ColumnInfo(name = "error_code")
    val errorCode: String? = null,

    @ColumnInfo(name = "execution_duration_ms")
    val executionDurationMs: Long = 0L,

    @ColumnInfo(name = "triggered_by")
    val triggeredBy: String? = null,

    @ColumnInfo(name = "actions_executed")
    val actionsExecuted: Int = 0,

    @ColumnInfo(name = "actions_failed")
    val actionsFailed: Int = 0,

    @ColumnInfo(name = "details")
    val details: String? = null // JSON string with additional details
)

enum class ExecutionStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    FAILED,
    CANCELLED,
    TIMEOUT,
    PERMISSION_DENIED,
    INVALID_CONFIGURATION
}