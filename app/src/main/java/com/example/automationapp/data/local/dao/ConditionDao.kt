package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.Condition
import com.example.automationapp.data.local.entity.ConditionType
import kotlinx.coroutines.flow.Flow

@Dao
interface ConditionDao {

    @Query("SELECT * FROM conditions WHERE rule_id = :ruleId")
    fun getConditionsByRuleId(ruleId: Long): Flow<List<Condition>>

    @Query("SELECT * FROM conditions WHERE rule_id = :ruleId AND is_active = 1")
    fun getActiveConditionsByRuleId(ruleId: Long): Flow<List<Condition>>

    @Query("SELECT * FROM conditions WHERE id = :conditionId")
    suspend fun getConditionById(conditionId: Long): Condition?

    @Query("SELECT * FROM conditions WHERE type = :type AND is_active = 1")
    fun getActiveConditionsByType(type: ConditionType): Flow<List<Condition>>

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
    suspend fun deleteConditionsByRuleId(ruleId: Long)

    @Query("UPDATE conditions SET is_active = :isActive WHERE id = :conditionId")
    suspend fun toggleConditionActive(conditionId: Long, isActive: Boolean)

    @Query("SELECT COUNT(*) FROM conditions WHERE rule_id = :ruleId")
    suspend fun getConditionCountForRule(ruleId: Long): Int

    @Transaction
    @Query("SELECT * FROM conditions WHERE rule_id = :ruleId")
    suspend fun getConditionsWithRule(ruleId: Long): List<Condition>
}

