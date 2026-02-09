package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.AutomationRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {

    @Query("SELECT * FROM automation_rules ORDER BY created_at DESC")
    fun getAllRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE is_enabled = 1 ORDER BY created_at DESC")
    fun getEnabledRules(): Flow<List<AutomationRule>>

    @Query("SELECT * FROM automation_rules WHERE id = :ruleId")
    suspend fun getRuleById(ruleId: Long): AutomationRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: AutomationRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<AutomationRule>): List<Long>

    @Update
    suspend fun updateRule(rule: AutomationRule)

    @Delete
    suspend fun deleteRule(rule: AutomationRule)

    @Query("DELETE FROM automation_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)

    @Query("UPDATE automation_rules SET is_enabled = :isEnabled WHERE id = :ruleId")
    suspend fun toggleRuleEnabled(ruleId: Long, isEnabled: Boolean)

    @Query("UPDATE automation_rules SET execution_count = execution_count + 1 WHERE id = :ruleId")
    suspend fun incrementExecutionCount(ruleId: Long)

    @Query("DELETE FROM automation_rules")
    suspend fun deleteAllRules()
}