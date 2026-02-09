package com.example.automationapp.domain.model

import com.example.automationapp.data.local.entity.*

data class RuleStatistics(
    val ruleId: Long = 0L,
    val ruleName: String = "",
    val executionCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val successRate: Float = 0f,
    val averageExecutionTimeMs: Double = 0.0,
    val lastExecutedAt: Long? = null
)