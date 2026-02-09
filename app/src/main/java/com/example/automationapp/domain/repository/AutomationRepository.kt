package com.example.automationapp.domain.repository

import com.example.automationapp.data.local.entity.*
import com.example.automationapp.domain.model.AppInfo
import com.example.automationapp.domain.model.RuleDetails
import kotlinx.coroutines.flow.Flow

interface AutomationRepository {

    // ==================== Rule Operations ====================

    suspend fun createRule(rule: AutomationRule): Long

    suspend fun createRuleWithTriggersAndActions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>
    ): Long

    suspend fun updateRule(rule: AutomationRule)

    suspend fun deleteRule(rule: AutomationRule)

    suspend fun deleteRuleById(ruleId: Long)

    suspend fun getRuleById(ruleId: Long): AutomationRule?

    fun getAllRules(): Flow<List<AutomationRule>>

    fun getEnabledRules(): Flow<List<AutomationRule>>

    suspend fun toggleRuleEnabled(ruleId: Long, enabled: Boolean)

    fun searchRules(query: String): Flow<List<AutomationRule>>

    fun getRulesByTriggerType(triggerType: TriggerType): Flow<List<AutomationRule>>

    // ==================== Trigger Operations ====================

    suspend fun insertTrigger(trigger: Trigger): Long

    suspend fun insertTriggers(triggers: List<Trigger>)

    suspend fun deleteTrigger(trigger: Trigger)

    fun getTriggersForRule(ruleId: Long): Flow<List<Trigger>>

    // ==================== Action Operations ====================

    suspend fun insertAction(action: Action): Long

    suspend fun insertActions(actions: List<Action>)

    suspend fun deleteAction(action: Action)

    fun getActionsForRule(ruleId: Long): Flow<List<Action>>

    // ==================== Condition Operations ====================

    suspend fun insertCondition(condition: Condition): Long

    suspend fun insertConditions(conditions: List<Condition>)

    suspend fun deleteCondition(condition: Condition)

    fun getConditionsForRule(ruleId: Long): Flow<List<Condition>>

    // ==================== Execution Log Operations ====================

    suspend fun insertExecutionLog(log: ExecutionLog): Long

    fun getExecutionLogsForRule(ruleId: Long, limit: Int = 50): Flow<List<ExecutionLog>>

    fun getRecentExecutionLogs(limit: Int = 100): Flow<List<ExecutionLog>>

    suspend fun clearAllExecutionLogs()

    suspend fun clearExecutionLogsForRule(ruleId: Long)

    suspend fun clearOldExecutionLogs(beforeTimestamp: Long)

    // ==================== Complex Queries ====================

    suspend fun getRuleDetails(ruleId: Long): RuleDetails?

    suspend fun incrementExecutionCount(ruleId: Long)

    // ==================== App Queries ====================

    suspend fun getInstalledUserApps(): List<AppInfo>
}
