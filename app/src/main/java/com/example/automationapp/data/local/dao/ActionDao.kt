package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.ActionType
import kotlinx.coroutines.flow.Flow

@Dao
interface ActionDao {

    @Query("SELECT * FROM actions WHERE rule_id = :ruleId ORDER BY sequence ASC")
    fun getActionsByRuleId(ruleId: Long): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE rule_id = :ruleId AND is_enabled = 1 ORDER BY sequence ASC")
    fun getEnabledActionsByRuleId(ruleId: Long): Flow<List<Action>>

    @Query("SELECT * FROM actions WHERE id = :actionId")
    suspend fun getActionById(actionId: Long): Action?

    @Query("SELECT * FROM actions WHERE type = :type AND is_enabled = 1")
    fun getActionsByType(type: ActionType): Flow<List<Action>>

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
    suspend fun deleteActionsByRuleId(ruleId: Long)

    @Query("UPDATE actions SET is_enabled = :isEnabled WHERE id = :actionId")
    suspend fun toggleActionEnabled(actionId: Long, isEnabled: Boolean)

    @Query("UPDATE actions SET sequence = :sequence WHERE id = :actionId")
    suspend fun updateActionSequence(actionId: Long, sequence: Int)

    @Transaction
    suspend fun reorderActions(ruleId: Long, actionIds: List<Long>) {
        actionIds.forEachIndexed { index, actionId ->
            updateActionSequence(actionId, index)
        }
    }
}