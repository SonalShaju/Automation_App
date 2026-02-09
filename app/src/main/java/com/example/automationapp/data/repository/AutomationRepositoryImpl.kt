package com.example.automationapp.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.automationapp.data.local.dao.AutomationDao
import com.example.automationapp.data.local.entity.*
import com.example.automationapp.domain.model.AppInfo
import com.example.automationapp.domain.model.RuleDetails
import com.example.automationapp.domain.repository.AutomationRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutomationRepositoryImpl @Inject constructor(
    private val dao: AutomationDao,
    private val packageManager: PackageManager
) : AutomationRepository {

    // ==================== Rule Operations ====================

    override suspend fun createRule(rule: AutomationRule): Long {
        return dao.insertRule(rule)
    }

    override suspend fun createRuleWithTriggersAndActions(
        rule: AutomationRule,
        triggers: List<Trigger>,
        actions: List<Action>
    ): Long {
        return dao.insertRuleWithTriggersAndActions(rule, triggers, actions)
    }

    override suspend fun updateRule(rule: AutomationRule) {
        dao.updateRule(rule)
    }

    override suspend fun deleteRule(rule: AutomationRule) {
        dao.deleteRule(rule)
    }

    override suspend fun deleteRuleById(ruleId: Long) {
        dao.deleteRuleWithRelations(ruleId)
    }

    override suspend fun getRuleById(ruleId: Long): AutomationRule? {
        return dao.getRuleById(ruleId)
    }

    override fun getAllRules(): Flow<List<AutomationRule>> {
        return dao.getAllRules()
    }

    override fun getEnabledRules(): Flow<List<AutomationRule>> {
        return dao.getEnabledRules()
    }

    override suspend fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        dao.toggleRuleEnabled(ruleId, enabled)
    }

    override fun searchRules(query: String): Flow<List<AutomationRule>> {
        return dao.searchRules(query)
    }

    override fun getRulesByTriggerType(triggerType: TriggerType): Flow<List<AutomationRule>> {
        return dao.getRulesByTriggerType(triggerType)
    }

    // ==================== Trigger Operations ====================

    override suspend fun insertTrigger(trigger: Trigger): Long {
        return dao.insertTrigger(trigger)
    }

    override suspend fun insertTriggers(triggers: List<Trigger>) {
        dao.insertTriggers(triggers)
    }

    override suspend fun deleteTrigger(trigger: Trigger) {
        dao.deleteTrigger(trigger)
    }

    override fun getTriggersForRule(ruleId: Long): Flow<List<Trigger>> {
        return dao.getTriggersForRule(ruleId)
    }

    // ==================== Action Operations ====================

    override suspend fun insertAction(action: Action): Long {
        return dao.insertAction(action)
    }

    override suspend fun insertActions(actions: List<Action>) {
        dao.insertActions(actions)
    }

    override suspend fun deleteAction(action: Action) {
        dao.deleteAction(action)
    }

    override fun getActionsForRule(ruleId: Long): Flow<List<Action>> {
        return dao.getActionsForRule(ruleId)
    }

    // ==================== Condition Operations ====================

    override suspend fun insertCondition(condition: Condition): Long {
        return dao.insertCondition(condition)
    }

    override suspend fun insertConditions(conditions: List<Condition>) {
        dao.insertConditions(conditions)
    }

    override suspend fun deleteCondition(condition: Condition) {
        dao.deleteCondition(condition)
    }

    override fun getConditionsForRule(ruleId: Long): Flow<List<Condition>> {
        return dao.getConditionsForRule(ruleId)
    }

    // ==================== Execution Log Operations ====================

    override suspend fun insertExecutionLog(log: ExecutionLog): Long {
        return dao.insertExecutionLog(log)
    }

    override fun getExecutionLogsForRule(ruleId: Long, limit: Int): Flow<List<ExecutionLog>> {
        return dao.getExecutionLogsForRule(ruleId, limit)
    }

    override fun getRecentExecutionLogs(limit: Int): Flow<List<ExecutionLog>> {
        return dao.getRecentExecutionLogs(limit)
    }

    override suspend fun clearAllExecutionLogs() {
        dao.clearAllExecutionLogs()
    }

    override suspend fun clearExecutionLogsForRule(ruleId: Long) {
        dao.clearExecutionLogsForRule(ruleId)
    }

    override suspend fun clearOldExecutionLogs(beforeTimestamp: Long) {
        dao.clearOldExecutionLogs(beforeTimestamp)
    }

    // ==================== Complex Queries ====================

    override suspend fun getRuleDetails(ruleId: Long): RuleDetails? {
        val ruleWithDetails = dao.getRuleWithDetails(ruleId) ?: return null

        return RuleDetails(
            rule = ruleWithDetails.rule,
            triggers = ruleWithDetails.triggers,
            actions = ruleWithDetails.actions,
            conditions = ruleWithDetails.conditions
        )
    }

    override suspend fun incrementExecutionCount(ruleId: Long) {
        dao.incrementExecutionCount(ruleId)
    }

    override suspend fun getInstalledUserApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        val apps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        apps.asSequence()
            .filter { appInfo ->
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdatedSystem
            }
            .map { appInfo ->
                val label = packageManager.getApplicationLabel(appInfo).toString().ifBlank { appInfo.packageName }
                AppInfo(name = label, packageName = appInfo.packageName)
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
