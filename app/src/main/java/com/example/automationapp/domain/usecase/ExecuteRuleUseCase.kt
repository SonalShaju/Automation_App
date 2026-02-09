package com.example.automationapp.domain.usecase

import android.util.Log
import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.Condition
import com.example.automationapp.data.local.entity.ExecutionLog
import com.example.automationapp.data.local.entity.ExecutionStatus
import com.example.automationapp.data.local.entity.LogicalOperator
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.domain.executor.ActionExecutor
import com.example.automationapp.domain.executor.TriggerEvaluator
import com.example.automationapp.domain.repository.AutomationRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Use case for executing an automation rule
 */
class ExecuteRuleUseCase @Inject constructor(
    private val repository: AutomationRepository,
    private val triggerEvaluator: TriggerEvaluator,
    private val actionExecutor: ActionExecutor
) {
    /**
     * Execute a rule by ID with optional trigger type for AND logic evaluation.
     *
     * @param ruleId The ID of the rule to execute
     * @param triggeredBy Source that triggered the rule execution
     * @param eventTriggerType Optional: The specific trigger type that caused this event.
     *                         When provided, uses AND logic: trigger must match AND all conditions must be true.
     * @return Result containing execution result
     */
    suspend operator fun invoke(
        ruleId: Long,
        triggeredBy: String = "Manual",
        eventTriggerType: TriggerType? = null
    ): Result<ExecutionResult> {
        return try {
            val startTime = System.currentTimeMillis()
            Log.d("ExecuteRuleUseCase", "Starting execution for rule $ruleId, triggered by: $triggeredBy")

            // Get rule
            val rule = repository.getRuleById(ruleId)
            if (rule == null) {
                Log.e("ExecuteRuleUseCase", "Rule $ruleId not found")
                return Result.failure(Exception("Rule not found"))
            }
            Log.d("ExecuteRuleUseCase", "Rule found: ${rule.name}, enabled: ${rule.isEnabled}")

            // Check if rule is enabled
            if (!rule.isEnabled) {
                Log.d("ExecuteRuleUseCase", "Rule $ruleId is disabled, skipping execution")
                return Result.success(
                    ExecutionResult(
                        executed = false,
                        reason = "Rule is disabled",
                        actionsExecuted = 0,
                        executionTimeMs = 0L
                    )
                )
            }

            // Get triggers
            val triggers = repository.getTriggersForRule(ruleId).first()
            Log.d("ExecuteRuleUseCase", "Found ${triggers.size} triggers for rule $ruleId")
            if (triggers.isEmpty()) {
                Log.w("ExecuteRuleUseCase", "No triggers configured for rule $ruleId")
                logExecution(
                    ruleId = ruleId,
                    success = false,
                    errorMessage = "No triggers configured",
                    executionTimeMs = 0L
                )
                return Result.success(
                    ExecutionResult(
                        executed = false,
                        reason = "No triggers configured",
                        actionsExecuted = 0,
                        executionTimeMs = 0L
                    )
                )
            }

            // Get actions
            val actions = repository.getActionsForRule(ruleId).first()
            Log.d("ExecuteRuleUseCase", "Found ${actions.size} actions for rule $ruleId")
            if (actions.isEmpty()) {
                Log.w("ExecuteRuleUseCase", "No actions configured for rule $ruleId")
                logExecution(
                    ruleId = ruleId,
                    success = false,
                    errorMessage = "No actions configured",
                    executionTimeMs = 0L
                )
                return Result.success(
                    ExecutionResult(
                        executed = false,
                        reason = "No actions configured",
                        actionsExecuted = 0,
                        executionTimeMs = 0L
                    )
                )
            }

            // Get conditions for AND logic evaluation
            val conditions = repository.getConditionsForRule(ruleId).first()
            Log.d("ExecuteRuleUseCase", "Found ${conditions.size} conditions for rule $ruleId")

            // Evaluate triggers and conditions
            Log.d("ExecuteRuleUseCase", "Evaluating triggers and conditions for rule $ruleId")
            val triggersSatisfied = if (eventTriggerType != null) {
                // New AND logic: check if trigger matches AND all conditions are met
                Log.d("ExecuteRuleUseCase", "Using AND logic with eventTriggerType: $eventTriggerType")
                triggerEvaluator.evaluate(eventTriggerType, triggers, conditions)
            } else {
                // Legacy evaluation: evaluate triggers only (for manual execution or backward compatibility)
                Log.d("ExecuteRuleUseCase", "Using legacy trigger evaluation (no eventTriggerType)")
                evaluateTriggers(triggers, conditions)
            }
            Log.d("ExecuteRuleUseCase", "Triggers and conditions satisfied: $triggersSatisfied")
            if (!triggersSatisfied) {
                Log.d("ExecuteRuleUseCase", "Trigger conditions not met for rule $ruleId, skipping action execution")
                return Result.success(
                    ExecutionResult(
                        executed = false,
                        reason = "Trigger conditions not met",
                        actionsExecuted = 0,
                        executionTimeMs = System.currentTimeMillis() - startTime
                    )
                )
            }

            // Execute actions
            Log.d("ExecuteRuleUseCase", "Starting action execution for rule $ruleId")
            var actionsExecuted = 0
            val actionErrors = mutableListOf<String>()

            actions.sortedBy { it.sequence }.forEach { action ->
                if (action.isEnabled) {
                    try {
                        Log.d("ExecuteRuleUseCase", "Executing action ${action.type} (sequence: ${action.sequence})")
                        executeAction(action)
                        actionsExecuted++
                        Log.d("ExecuteRuleUseCase", "Action ${action.type} executed successfully")
                    } catch (e: Exception) {
                        Log.e("ExecuteRuleUseCase", "Failed to execute action ${action.type}", e)
                        actionErrors.add("Action ${action.type.name}: ${e.message}")
                    }
                } else {
                    Log.d("ExecuteRuleUseCase", "Skipping disabled action ${action.type}")
                }
            }

            val executionTime = System.currentTimeMillis() - startTime
            Log.d("ExecuteRuleUseCase", "Execution completed: $actionsExecuted actions executed in ${executionTime}ms")

            // Log execution
            val success = actionErrors.isEmpty() && actionsExecuted > 0
            logExecution(
                ruleId = ruleId,
                success = success,
                errorMessage = actionErrors.joinToString("; ").takeIf { it.isNotEmpty() },
                executionTimeMs = executionTime
            )

            // Increment execution count
            if (success) {
                repository.incrementExecutionCount(ruleId)
            }

            Result.success(
                ExecutionResult(
                    executed = true,
                    reason = if (success) "Executed successfully" else "Some actions failed",
                    actionsExecuted = actionsExecuted,
                    executionTimeMs = executionTime,
                    errors = actionErrors.takeIf { it.isNotEmpty() }
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Evaluate if triggers are satisfied based on their logical operators,
     * then check if all conditions are met (AND logic for conditions).
     */
    private suspend fun evaluateTriggers(triggers: List<Trigger>, conditions: List<Condition>): Boolean {
        if (triggers.isEmpty()) {
            Log.d("ExecuteRuleUseCase", "No triggers to evaluate")
            return false
        }

        // Filter only active triggers
        val activeTriggers = triggers.filter { it.isActive }
        if (activeTriggers.isEmpty()) {
            Log.d("ExecuteRuleUseCase", "No active triggers to evaluate")
            return false
        }

        // Separate AND and OR triggers
        val andTriggers = activeTriggers.filter { it.logicalOperator == LogicalOperator.AND }
        val orTriggers = activeTriggers.filter { it.logicalOperator == LogicalOperator.OR }

        Log.d("ExecuteRuleUseCase", "Evaluating ${andTriggers.size} AND triggers and ${orTriggers.size} OR triggers")

        // Evaluate AND triggers - all must be true
        var andResult = true
        for (trigger in andTriggers) {
            try {
                val triggerResult = triggerEvaluator.evaluate(trigger)
                Log.d("ExecuteRuleUseCase", "AND Trigger ${trigger.type} evaluated: $triggerResult")
                andResult = andResult && triggerResult
                if (!andResult) {
                    Log.d("ExecuteRuleUseCase", "AND trigger ${trigger.type} failed, short-circuiting")
                    break // Short-circuit on first failure
                }
            } catch (e: Exception) {
                Log.e("ExecuteRuleUseCase", "Error evaluating AND trigger ${trigger.type}", e)
                andResult = false
                break
            }
        }

        // Evaluate OR triggers - at least one must be true
        var orResult = false
        for (trigger in orTriggers) {
            try {
                val triggerResult = triggerEvaluator.evaluate(trigger)
                Log.d("ExecuteRuleUseCase", "OR Trigger ${trigger.type} evaluated: $triggerResult")
                orResult = orResult || triggerResult
                if (orResult) {
                    Log.d("ExecuteRuleUseCase", "OR trigger ${trigger.type} succeeded, short-circuiting")
                    break // Short-circuit on first success
                }
            } catch (e: Exception) {
                Log.e("ExecuteRuleUseCase", "Error evaluating OR trigger ${trigger.type}", e)
                // Continue to next OR trigger
            }
        }

        // Combine trigger results based on what triggers exist
        val triggerResult = when {
            andTriggers.isEmpty() && orTriggers.isEmpty() -> false
            andTriggers.isEmpty() -> orResult  // Only OR triggers: at least one must pass
            orTriggers.isEmpty() -> andResult  // Only AND triggers: all must pass
            else -> andResult && orResult      // Both types: AND conditions AND at least one OR must pass
        }

        Log.d("ExecuteRuleUseCase", "Trigger evaluation result: $triggerResult (AND=$andResult, OR=$orResult)")

        // If triggers don't pass, return false
        if (!triggerResult) {
            return false
        }

        // Now evaluate ALL conditions (AND logic) - all must pass
        val activeConditions = conditions.filter { it.isActive }
        if (activeConditions.isEmpty()) {
            Log.d("ExecuteRuleUseCase", "No active conditions to evaluate, triggers passed - returning true")
            return true
        }

        Log.d("ExecuteRuleUseCase", "Evaluating ${activeConditions.size} conditions with AND logic")
        for (condition in activeConditions) {
            try {
                val conditionResult = triggerEvaluator.evaluateCondition(condition)
                Log.d("ExecuteRuleUseCase", "Condition ${condition.type} evaluated: $conditionResult")
                if (!conditionResult) {
                    Log.d("ExecuteRuleUseCase", "Condition ${condition.type} not met - returning false")
                    return false
                }
            } catch (e: Exception) {
                Log.e("ExecuteRuleUseCase", "Error evaluating condition ${condition.type}", e)
                return false
            }
        }

        Log.d("ExecuteRuleUseCase", "All triggers and conditions satisfied - returning true")
        return true
    }

    /**
     * Execute a single action using ActionExecutor
     */
    private suspend fun executeAction(action: Action) {
        Log.d("ExecuteRuleUseCase", "Executing action: ${action.type}")
        actionExecutor.executeAction(action)
    }

    /**
     * Log rule execution to database
     */
    private suspend fun logExecution(
        ruleId: Long,
        success: Boolean,
        errorMessage: String? = null,
        executionTimeMs: Long
    ) {
        try {
            val log = ExecutionLog(
                ruleId = ruleId,
                status = if (success) ExecutionStatus.SUCCESS else ExecutionStatus.FAILED,  // ← ADD THIS
                success = success,
                errorMessage = errorMessage,
                executionDurationMs = executionTimeMs,
                executedAt = System.currentTimeMillis()
            )
            repository.insertExecutionLog(log)
        } catch (e: Exception) {
            // Log error but don't fail the execution
            android.util.Log.e("ExecuteRuleUseCase", "Failed to log execution", e)
        }
    }
}

/**
 * Result of rule execution
 */
data class ExecutionResult(
    val executed: Boolean,
    val reason: String,
    val actionsExecuted: Int,
    val executionTimeMs: Long,
    val errors: List<String>? = null
)
