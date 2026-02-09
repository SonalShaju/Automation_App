package com.example.automationapp.domain.usecase

import com.example.automationapp.domain.model.RuleDetails
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.domain.repository.AutomationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for retrieving all automation rules
 */
class GetAllRulesUseCase @Inject constructor(
    private val repository: AutomationRepository
) {
    operator fun invoke(): Flow<List<AutomationRule>> {
        return repository.getAllRules()
    }
}

class GetRuleDetailsUseCase @Inject constructor(
    private val repository: AutomationRepository
) {
    suspend operator fun invoke(ruleId: Long): Result<RuleDetails> {
        return try {
            // Get the rule
            val rule = repository.getRuleById(ruleId)
                ?: return Result.failure(Exception("Rule with ID $ruleId not found"))

            // Get triggers and actions
            val triggers = repository.getTriggersForRule(ruleId).first()
            val actions = repository.getActionsForRule(ruleId).first()

            // Create and return rule details
            val ruleDetails = RuleDetails(
                rule = rule,
                triggers = triggers.sortedBy { it.id },
                actions = actions.sortedBy { it.sequence },
                triggerCount = triggers.size,
                actionCount = actions.size,
                hasLocationTrigger = triggers.any { it.type == com.example.automationapp.data.local.entity.TriggerType.LOCATION_BASED },
                hasTimeTrigger = triggers.any { it.type == com.example.automationapp.data.local.entity.TriggerType.TIME_BASED }
            )

            Result.success(ruleDetails)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to get rule details: ${e.message}", e))
        }
    }
}