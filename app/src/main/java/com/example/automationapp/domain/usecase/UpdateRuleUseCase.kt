package com.example.automationapp.domain.usecase

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.service.RuleSchedulingManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for updating an existing automation rule with full synchronization.
 *
 * This ensures that when a rule is updated:
 * 1. Old triggers are cancelled (alarms, geofences)
 * 2. Database is updated with new data
 * 3. New triggers are scheduled with the Android system
 * 4. Services are notified to refresh their cached data
 */
class UpdateRuleUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val ruleSchedulingManager: RuleSchedulingManager,
    @ApplicationContext private val context: Context
) {
    companion object {
        private const val TAG = "UpdateRuleUseCase"

        /**
         * Broadcast action to notify services that rules have been updated
         */
        const val ACTION_RULES_UPDATED = "com.example.automationapp.action.RULES_UPDATED"
        const val EXTRA_RULE_ID = "rule_id"
    }

    /**
     * Update an existing automation rule with full synchronization
     *
     * @param ruleId The ID of the rule to update
     * @param name Updated rule name
     * @param description Updated rule description
     * @param triggers Updated list of trigger conditions
     * @param actions Updated list of actions to perform
     * @param isEnabled Whether the rule should be enabled
     * @param exitActionType Optional exit action type for Time Range triggers
     * @param exitActionParams Optional exit action parameters for Time Range triggers
     * @return Result containing the updated rule ID
     */
    suspend operator fun invoke(
        ruleId: Long,
        name: String,
        description: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        isEnabled: Boolean = true,
        exitActionType: ActionType? = null,
        exitActionParams: String? = null
    ): Result<Long> {
        return try {
            Log.d(TAG, "Starting rule update for ruleId: $ruleId")

            // Validate inputs
            if (ruleId <= 0) {
                return Result.failure(IllegalArgumentException("Invalid rule ID"))
            }
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Rule name cannot be empty"))
            }
            if (triggers.isEmpty()) {
                return Result.failure(IllegalArgumentException("At least one trigger is required"))
            }
            if (actions.isEmpty()) {
                return Result.failure(IllegalArgumentException("At least one action is required"))
            }

            // Get the existing rule
            val existingRule = repository.getRuleById(ruleId)
                ?: return Result.failure(IllegalArgumentException("Rule not found"))

            // ========== STEP A: Cancel old triggers ==========
            Log.d(TAG, "Step A: Cancelling old triggers for rule $ruleId")
            ruleSchedulingManager.cancelRuleTriggers(ruleId)

            // ========== STEP B: Update database ==========
            Log.d(TAG, "Step B: Updating database for rule $ruleId")

            // Update the rule entity with exit action support
            val updatedRule = existingRule.copy(
                name = name,
                description = description,
                isEnabled = isEnabled,
                updatedAt = System.currentTimeMillis(),
                exitActionType = exitActionType,
                exitActionParams = exitActionParams
            )
            repository.updateRule(updatedRule)

            // Delete old triggers and actions
            repository.getTriggersForRule(ruleId).first().forEach { trigger ->
                repository.deleteTrigger(trigger)
            }
            repository.getActionsForRule(ruleId).first().forEach { action ->
                repository.deleteAction(action)
            }

            // Insert new triggers and actions with proper ruleId
            val triggersWithRuleId = triggers.map { it.copy(ruleId = ruleId) }
            val actionsWithRuleId = actions.map { it.copy(ruleId = ruleId) }

            repository.insertTriggers(triggersWithRuleId)
            repository.insertActions(actionsWithRuleId)

            // ========== STEP C: Reschedule triggers (if enabled) ==========
            if (isEnabled) {
                Log.d(TAG, "Step C: Rescheduling triggers for rule $ruleId")
                ruleSchedulingManager.scheduleRuleTriggers(ruleId)
            } else {
                Log.d(TAG, "Step C: Rule is disabled, skipping trigger scheduling")
            }

            // ========== STEP D: Notify services to refresh ==========
            Log.d(TAG, "Step D: Broadcasting rule update notification")
            broadcastRuleUpdate(ruleId)

            Log.d(TAG, "Rule update completed successfully for ruleId: $ruleId")
            Result.success(ruleId)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating rule $ruleId", e)
            Result.failure(e)
        }
    }

    /**
     * Send a broadcast to notify services that a rule has been updated.
     * This allows services like TriggerMonitorService to refresh their cached data.
     */
    private fun broadcastRuleUpdate(ruleId: Long) {
        try {
            val intent = Intent(ACTION_RULES_UPDATED).apply {
                setPackage(context.packageName)
                putExtra(EXTRA_RULE_ID, ruleId)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "Broadcast sent for rule update: $ruleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error sending broadcast for rule update", e)
        }
    }
}

