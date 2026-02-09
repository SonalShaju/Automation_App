package com.example.automationapp.domain.usecase

import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.service.RuleSchedulingManager
import javax.inject.Inject

/**
 * Use case for toggling the enabled state of an automation rule
 */
class ToggleRuleUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val ruleSchedulingManager: RuleSchedulingManager
) {
    suspend operator fun invoke(ruleId: Long, enabled: Boolean): Result<Unit> {
        return try {
            // Verify rule exists before toggling
            val rule = repository.getRuleById(ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with ID $ruleId not found"))

            // Check if the state is already as requested
            if (rule.isEnabled == enabled) {
                return Result.success(Unit) // Already in desired state
            }

            // Toggle the rule
            repository.toggleRuleEnabled(ruleId, enabled)

            // Schedule or cancel triggers based on new state
            if (enabled) {
                ruleSchedulingManager.scheduleRuleTriggers(ruleId)
            } else {
                ruleSchedulingManager.cancelRuleTriggers(ruleId)
            }

            Result.success(Unit)
        } catch (e: RuleNotFoundException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to toggle rule: ${e.message}", e))
        }
    }

    /**
     * Enable a rule
     */
    suspend fun enable(ruleId: Long): Result<Unit> {
        return invoke(ruleId, enabled = true)
    }

    /**
     * Disable a rule
     */
    suspend fun disable(ruleId: Long): Result<Unit> {
        return invoke(ruleId, enabled = false)
    }
}