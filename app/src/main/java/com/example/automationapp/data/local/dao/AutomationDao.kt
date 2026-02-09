package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationDao {

    // ==================== Rule Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule): Long

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)

    @Query("DELETE FROM automation_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)

    @Query("SELECT * FROM automation_rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: Long): AutomationRule?

    @Query("SELECT * FROM automation_rules ORDER BY created_at DESC")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE is_enabled = 1 ORDER BY created_at DESC")
    fun getEnabledRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE is_enabled = 0 ORDER BY created_at DESC")
    fun getDisabledRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE name LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchRules(query: String): Flow<List<AutomationRule>>

    @Query("UPDATE automation_rules SET is_enabled = :enabled, updated_at = :timestamp WHERE id = :ruleId")
    suspend fun toggleRuleEnabled(ruleId: Long, enabled: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE automation_rules SET execution_count = execution_count + 1, last_executed_at = :timestamp, updated_at = :timestamp WHERE id = :ruleId")
    suspend fun incrementExecutionCount(ruleId: Long, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM automation_rules")
    suspend fun getRuleCount(): Int

    @Query("SELECT COUNT(*) FROM automation_rules WHERE is_enabled = 1")
    suspend fun getEnabledRuleCount(): Int

    // ==================== Trigger Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrigger(trigger: Trigger): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggers(triggers: List<Trigger>): List<Long>

    @Update
    suspend fun updateTrigger(trigger: Trigger)

    @Delete
    suspend fun deleteTrigger(trigger: Trigger)

    @Query("DELETE FROM triggers WHERE id = :triggerId")
    suspend fun deleteTriggerById(triggerId: Long)

    @Query("DELETE FROM triggers WHERE rule_id = :ruleId")
    suspend fun deleteTriggersForRule(ruleId: Long)

    @Query("SELECT * FROM triggers WHERE id = :triggerId")
    suspend fun getTriggerById(triggerId: Long): Trigger?

    @Query("SELECT * FROM triggers WHERE rule_id = :ruleId ORDER BY created_at ASC")
    fun getTriggersForRule(ruleId: Long): Flow<List<Trigger>>

    @Query("SELECT * FROM triggers WHERE rule_id = :ruleId AND is_active = 1 ORDER BY created_at ASC")
    fun getActiveTriggersForRule(ruleId: Long): Flow<List<Trigger>>

    @Query("SELECT * FROM triggers WHERE type = :triggerType")
    fun getTriggersByType(triggerType: TriggerType): Flow<List<Trigger>>

    @Query("UPDATE triggers SET is_active = :isActive WHERE id = :triggerId")
    suspend fun toggleTriggerActive(triggerId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM triggers WHERE rule_id = :ruleId")
    suspend fun getTriggerCountForRule(ruleId: Long): Int

    // ==================== Action Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAction(action: Action): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActions(actions: List<Action>): List<Long>

    @Update
    suspend fun updateAction(action: Action)

    @Delete
    suspend fun deleteAction(action: Action)

    @Query("DELETE FROM actions WHERE id = :actionId")
    suspend fun deleteActionById(actionId: Long)

    @Query("DELETE FROM actions WHERE rule_id = :ruleId")
    suspend fun deleteActionsForRule(ruleId: Long)

    @Query("SELECT * FROM actions WHERE id = :actionId")
    suspend fun getActionById(actionId: Long): Action?

    @Query("SELECT * FROM actions WHERE rule_id = :ruleId ORDER BY sequence ASC")
    fun getActionsForRule(ruleId: Long): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE rule_id = :ruleId AND is_enabled = 1 ORDER BY sequence ASC")
    fun getEnabledActionsForRule(ruleId: Long): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE type = :actionType")
    fun getActionsByType(actionType: ActionType): Flow<List<Action>>

    @Query("UPDATE actions SET is_enabled = :isEnabled WHERE id = :actionId")
    suspend fun toggleActionEnabled(actionId: Long, isEnabled: Boolean)

    @Query("UPDATE actions SET sequence = :sequence WHERE id = :actionId")
    suspend fun updateActionSequence(actionId: Long, sequence: Int)

    @Query("SELECT COUNT(*) FROM actions WHERE rule_id = :ruleId")
    suspend fun getActionCountForRule(ruleId: Long): Int

    // ==================== Condition Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCondition(condition: Condition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConditions(conditions: List<Condition>): List<Long>

    @Update
    suspend fun updateCondition(condition: Condition)

    @Delete
    suspend fun deleteCondition(condition: Condition)

    @Query("DELETE FROM conditions WHERE id = :conditionId")
    suspend fun deleteConditionById(conditionId: Long)

    @Query("DELETE FROM conditions WHERE rule_id = :ruleId")
    suspend fun deleteConditionsForRule(ruleId: Long)

    @Query("SELECT * FROM conditions WHERE id = :conditionId")
    suspend fun getConditionById(conditionId: Long): Condition?

    @Query("SELECT * FROM conditions WHERE rule_id = :ruleId ORDER BY created_at ASC")
    fun getConditionsForRule(ruleId: Long): Flow<List<Condition>>

    @Query("SELECT * FROM conditions WHERE rule_id = :ruleId AND is_active = 1 ORDER BY created_at ASC")
    fun getActiveConditionsForRule(ruleId: Long): Flow<List<Condition>>

    @Query("SELECT * FROM conditions WHERE type = :conditionType")
    fun getConditionsByType(conditionType: ConditionType): Flow<List<Condition>>

    @Query("UPDATE conditions SET is_active = :isActive WHERE id = :conditionId")
    suspend fun toggleConditionActive(conditionId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM conditions WHERE rule_id = :ruleId")
    suspend fun getConditionCountForRule(ruleId: Long): Int

    // ==================== Execution Log Operations ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExecutionLog(log: ExecutionLog): Long

    @Query("SELECT * FROM execution_logs WHERE id = :logId")
    suspend fun getExecutionLogById(logId: Long): ExecutionLog?

    @Query("SELECT * FROM execution_logs WHERE rule_id = :ruleId ORDER BY executed_at DESC LIMIT :limit")
    fun getExecutionLogsForRule(ruleId: Long, limit: Int = 50): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs ORDER BY executed_at DESC LIMIT :limit")
    fun getRecentExecutionLogs(limit: Int = 100): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE success = 1 ORDER BY executed_at DESC LIMIT :limit")
    fun getSuccessfulExecutionLogs(limit: Int = 50): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE success = 0 ORDER BY executed_at DESC LIMIT :limit")
    fun getFailedExecutionLogs(limit: Int = 50): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE executed_at BETWEEN :startTime AND :endTime ORDER BY executed_at DESC")
    fun getExecutionLogsBetween(startTime: Long, endTime: Long): Flow<List<ExecutionLog>>

    @Query("DELETE FROM execution_logs")
    suspend fun clearAllExecutionLogs()

    @Query("DELETE FROM execution_logs WHERE rule_id = :ruleId")
    suspend fun clearExecutionLogsForRule(ruleId: Long)

    @Query("DELETE FROM execution_logs WHERE executed_at < :beforeTimestamp")
    suspend fun clearOldExecutionLogs(beforeTimestamp: Long)

    @Query("SELECT COUNT(*) FROM execution_logs")
    suspend fun getExecutionLogCount(): Int

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId")
    suspend fun getExecutionLogCountForRule(ruleId: Long): Int

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId AND success = 1")
    suspend fun getSuccessfulExecutionCountForRule(ruleId: Long): Int

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId AND success = 0")
    suspend fun getFailedExecutionCountForRule(ruleId: Long): Int

    @Query("SELECT * FROM execution_logs WHERE rule_id = :ruleId ORDER BY executed_at DESC LIMIT 1")
    suspend fun getLastExecutionForRule(ruleId: Long): ExecutionLog?

    // ==================== Complex Queries ====================

    /**
     * Get complete rule details with triggers and actions
     */
    @Transaction
    @Query("SELECT * FROM automation_rules WHERE id = :ruleId")
    suspend fun getRuleWithDetails(ruleId: Long): RuleWithDetails?

    /**
     * Get all rules with their triggers
     */
    @Transaction
    @Query("SELECT * FROM automation_rules ORDER BY created_at DESC")
    fun getAllRulesWithTriggers(): Flow<List<RuleWithTriggers>>

    /**
     * Get all rules with their actions
     */
    @Transaction
    @Query("SELECT * FROM automation_rules ORDER BY created_at DESC")
    fun getAllRulesWithActions(): Flow<List<RuleWithActions>>

    /**
     * Get rules by trigger type
     */
    @Query("""
        SELECT DISTINCT r.* FROM automation_rules r
        INNER JOIN triggers t ON r.id = t.rule_id
        WHERE t.type = :triggerType
        ORDER BY r.created_at DESC
    """)
    fun getRulesByTriggerType(triggerType: TriggerType): Flow<List<AutomationRule>>

    /**
     * Get rules by action type
     */
    @Query("""
        SELECT DISTINCT r.* FROM automation_rules r
        INNER JOIN actions a ON r.id = a.rule_id
        WHERE a.type = :actionType
        ORDER BY r.created_at DESC
    """)
    fun getRulesByActionType(actionType: ActionType): Flow<List<AutomationRule>>

    /**
     * Get execution statistics for a rule
     */
    @Query("""
        SELECT 
            COUNT(*) as total_executions,
            SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful_executions,
            SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_executions,
            AVG(execution_duration_ms) as avg_execution_time,
            MAX(executed_at) as last_execution_time
        FROM execution_logs
        WHERE rule_id = :ruleId
    """)
    suspend fun getRuleExecutionStats(ruleId: Long): ExecutionStats?

    /**
     * Get overall execution statistics
     */
    @Query("""
        SELECT 
            COUNT(*) as total_executions,
            SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) as successful_executions,
            SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) as failed_executions,
            AVG(execution_duration_ms) as avg_execution_time,
            MAX(executed_at) as last_execution_time
        FROM execution_logs
    """)
    suspend fun getOverallExecutionStats(): ExecutionStats?

    /**
     * Get most executed rules
     */
    @Query("""
        SELECT * FROM automation_rules
        WHERE execution_count > 0
        ORDER BY execution_count DESC
        LIMIT :limit
    """)
    fun getMostExecutedRules(limit: Int = 10): Flow<List<AutomationRule>>

    /**
     * Get least executed rules
     */
    @Query("""
        SELECT * FROM automation_rules
        ORDER BY execution_count ASC
        LIMIT :limit
    """)
    fun getLeastExecutedRules(limit: Int = 10): Flow<List<AutomationRule>>

    /**
     * Get rules never executed
     */
    @Query("SELECT * FROM automation_rules WHERE execution_count = 0 ORDER BY created_at DESC")
    fun getNeverExecutedRules(): Flow<List<AutomationRule>>

    // ==================== Batch Operations ====================

    @Transaction
    suspend fun insertRuleWithTriggersAndActions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>
    ): Long {
        val ruleId = insertRule(rule)
        val triggersWithRuleId = triggers.map { it.copy(ruleId = ruleId) }
        val actionsWithRuleId = actions.map { it.copy(ruleId = ruleId) }
        insertTriggers(triggersWithRuleId)
        insertActions(actionsWithRuleId)
        return ruleId
    }

    @Transaction
    suspend fun insertRuleWithTriggersActionsAndConditions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>,
        conditions: List<Condition>
    ): Long {
        val ruleId = insertRule(rule)
        val triggersWithRuleId = triggers.map { it.copy(ruleId = ruleId) }
        val actionsWithRuleId = actions.map { it.copy(ruleId = ruleId) }
        val conditionsWithRuleId = conditions.map { it.copy(ruleId = ruleId) }
        insertTriggers(triggersWithRuleId)
        insertActions(actionsWithRuleId)
        insertConditions(conditionsWithRuleId)
        return ruleId
    }

    @Transaction
    suspend fun updateRuleWithTriggersAndActions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>
    ) {
        updateRule(rule)
        deleteTriggersForRule(rule.id)
        deleteActionsForRule(rule.id)
        insertTriggers(triggers)
        insertActions(actions)
    }

    @Transaction
    suspend fun updateRuleWithTriggersActionsAndConditions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>,
        conditions: List<Condition>
    ) {
        updateRule(rule)
        deleteTriggersForRule(rule.id)
        deleteActionsForRule(rule.id)
        deleteConditionsForRule(rule.id)
        insertTriggers(triggers)
        insertActions(actions)
        insertConditions(conditions)
    }

    @Transaction
    suspend fun deleteRuleWithRelations(ruleId: Long) {
        deleteTriggersForRule(ruleId)
        deleteActionsForRule(ruleId)
        deleteConditionsForRule(ruleId)
        clearExecutionLogsForRule(ruleId)
        deleteRuleById(ruleId)
    }
}

// ==================== Data Classes for Complex Queries ====================

data class RuleWithDetails(
    @Embedded val rule: AutomationRule,
    @Relation(
        parentColumn = "id",
        entityColumn = "rule_id"
    )
    val triggers: List<Trigger>,
    @Relation(
        parentColumn = "id",
        entityColumn = "rule_id"
    )
    val actions: List<Action>,
    @Relation(
        parentColumn = "id",
        entityColumn = "rule_id"
    )
    val conditions: List<Condition>
)

data class RuleWithTriggers(
    @Embedded val rule: AutomationRule,
    @Relation(
        parentColumn = "id",
        entityColumn = "rule_id"
    )
    val triggers: List<Trigger>
)

data class RuleWithActions(
    @Embedded val rule: AutomationRule,
    @Relation(
        parentColumn = "id",
        entityColumn = "rule_id"
    )
    val actions: List<Action>
)

data class ExecutionStats(
    val total_executions: Int,
    val successful_executions: Int,
    val failed_executions: Int,
    val avg_execution_time: Double?,
    val last_execution_time: Long?
)
