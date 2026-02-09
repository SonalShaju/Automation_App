package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.data.local.entity.TriggerType
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {

    @Query("SELECT * FROM triggers WHERE rule_id = :ruleId")
    fun getTriggersByRuleId(ruleId: Long): Flow<List<Trigger>>

    @Query("SELECT * FROM triggers WHERE id = :triggerId")
    suspend fun getTriggerById(triggerId: Long): Trigger?

    @Query("SELECT * FROM triggers WHERE type = :type AND is_active = 1")
    fun getActiveTriggersByType(type: TriggerType): Flow<List<Trigger>>

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
    suspend fun deleteTriggersByRuleId(ruleId: Long)

    @Query("UPDATE triggers SET is_active = :isActive WHERE id = :triggerId")
    suspend fun toggleTriggerActive(triggerId: Long, isActive: Boolean)

    @Transaction
    @Query("SELECT * FROM triggers WHERE rule_id = :ruleId")
    suspend fun getTriggersWithRule(ruleId: Long): List<Trigger>
}