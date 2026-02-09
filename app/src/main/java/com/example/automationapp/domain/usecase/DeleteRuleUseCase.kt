package com.example.automationapp.domain.usecase

import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.service.RuleSchedulingManager
import javax.inject.Inject

/**
 * Use case for deleting an automation rule
 * This will also delete all associated triggers, actions, and execution logs via CASCADE
 */
class DeleteRuleUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val ruleSchedulingManager: RuleSchedulingManager
) {
    suspend operator fun invoke(ruleId: Long): Result<Unit> {
        return try {
            // Verify rule exists before attempting deletion
            repository.getRuleById(ruleId)
                ?: return Result.failure(RuleNotFoundException("Rule with ID $ruleId not found"))

            // Cancel all triggers for this rule first
            ruleSchedulingManager.cancelRuleTriggers(ruleId)

            // Delete the rule (triggers, actions, and logs deleted via CASCADE)
            repository.deleteRuleById(ruleId)

            Result.success(Unit)
        } catch (e: RuleNotFoundException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete rule: ${e.message}", e))
        }
    }

    /**
     * Delete rule without checking if it exists
     * Use this when you're certain the rule exists to skip the extra database query
     */
    suspend fun forceDelete(ruleId: Long): Result<Unit> {
        return try {
            ruleSchedulingManager.cancelRuleTriggers(ruleId)
            repository.deleteRuleById(ruleId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to delete rule: ${e.message}", e))
        }
    }
}

/**
 * Custom exception for rule not found scenarios
 */
class RuleNotFoundException(message: String) : Exception(message)