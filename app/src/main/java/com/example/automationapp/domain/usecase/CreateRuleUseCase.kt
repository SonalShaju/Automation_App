package com.example.automationapp.domain.usecase

import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.service.RuleSchedulingManager
import javax.inject.Inject

/**
 * Use case for creating a new automation rule with triggers and actions
 */
class CreateRuleUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val ruleSchedulingManager: RuleSchedulingManager
) {
    /**
     * Create a new automation rule
     *
     * @param name Rule name
     * @param description Rule description
     * @param triggers List of trigger conditions
     * @param actions List of actions to perform
     * @param exitActionType Optional exit action type for Time Range triggers
     * @param exitActionParams Optional exit action parameters for Time Range triggers
     * @return Result containing the created rule ID
     */
    suspend operator fun invoke(
        name: String,
        description: String,
        triggers: List<Trigger>,
        actions: List<Action>,
        exitActionType: ActionType? = null,
        exitActionParams: String? = null
    ): Result<Long> {
        return try {
            // Validate inputs
            if (name.isBlank()) {
                return Result.failure(IllegalArgumentException("Rule name cannot be empty"))
            }


            if (triggers.isEmpty()) {
                return Result.failure(IllegalArgumentException("At least one trigger is required"))
            }

            if (actions.isEmpty()) {
                return Result.failure(IllegalArgumentException("At least one action is required"))
            }

            // Create the rule with exit action support
            val rule = AutomationRule(
                name = name,
                description = description,
                isEnabled = true,
                executionCount = 0,
                lastExecutedAt = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                exitActionType = exitActionType,
                exitActionParams = exitActionParams
            )

            // Insert rule with triggers and actions
            val ruleId = repository.createRuleWithTriggersAndActions(
                rule = rule,
                triggers = triggers,
                actions = actions
            )

            // Schedule the triggers (alarms, geofences, etc.)
            ruleSchedulingManager.scheduleRuleTriggers(ruleId)

            Result.success(ruleId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
