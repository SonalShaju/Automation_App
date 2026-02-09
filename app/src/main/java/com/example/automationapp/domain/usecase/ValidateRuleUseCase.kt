package com.example.automationapp.domain.usecase

import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.LogicalOperator
import com.example.automationapp.data.local.entity.Trigger
import javax.inject.Inject

/**
 * Use case for validating automation rule data before creation or update.
 * Ensures rules meet all business logic requirements.
 */
class ValidateRuleUseCase @Inject constructor() {

    /**
     * Validates a complete automation rule.
     *
     * @param name Rule name
     * @param description Rule description
     * @param triggers List of trigger conditions
     * @param actions List of actions to perform
     * @return ValidationResult containing success status and error messages
     */
    operator fun invoke(
        name: String,
        description: String,
        triggers: List<Trigger>,
        actions: List<Action>
    ): ValidationResult {
        val errors = mutableListOf<String>()

        // Validate rule name
        when {
            name.isBlank() ->
                errors.add("Rule name cannot be empty")
            name.length < 3 ->
                errors.add("Rule name must be at least 3 characters")
            name.length > 50 ->
                errors.add("Rule name must not exceed 50 characters")
            !name.matches(Regex("^[a-zA-Z0-9\\s\\-_]+$")) ->
                errors.add("Rule name can only contain letters, numbers, spaces, hyphens, and underscores")
        }

        // Validate description (optional - only validate if provided)
        when {
            description.isNotBlank() && description.length < 3 ->
                errors.add("Rule description must be at least 3 characters if provided")
            description.length > 200 ->
                errors.add("Rule description must not exceed 200 characters")
        }

        // Validate triggers
        when {
            triggers.isEmpty() ->
                errors.add("At least one trigger is required")
            triggers.size > 5 ->
                errors.add("Maximum 5 triggers allowed per rule")
            else -> {
                triggers.forEachIndexed { index, trigger ->
                    val triggerErrors = validateTrigger(trigger, index)
                    errors.addAll(triggerErrors)
                }
            }
        }

        // Validate actions
        when {
            actions.isEmpty() ->
                errors.add("At least one action is required")
            actions.size > 10 ->
                errors.add("Maximum 10 actions allowed per rule")
            else -> {
                actions.forEachIndexed { index, action ->
                    val actionErrors = validateAction(action, index)
                    errors.addAll(actionErrors)
                }
            }
        }

        // Check for conflicting actions
        errors.addAll(checkForConflictingActions(actions))

        return if (errors.isEmpty()) {
            ValidationResult.Success
        } else {
            ValidationResult.Failure(errors)
        }
    }

    /**
     * Validates a single trigger condition.
     */
    private fun validateTrigger(trigger: Trigger, index: Int): List<String> {
        val errors = mutableListOf<String>()

        // Parameters not empty
        if (trigger.parameters.isBlank()) {
            errors.add("Trigger ${index + 1}: Parameters cannot be empty")
            return errors
        }

        // Basic JSON format check
        try {
            if (!isValidJson(trigger.parameters)) {
                errors.add("Trigger ${index + 1}: Invalid parameters format (must be JSON object or array)")
            }
        } catch (e: Exception) {
            errors.add("Trigger ${index + 1}: Failed to parse parameters - ${e.message}")
        }

        // Logical operator (enum)
        if (trigger.logicalOperator !in listOf(LogicalOperator.AND, LogicalOperator.OR)) {
            errors.add("Trigger ${index + 1}: Logical operator must be AND or OR")
        }

        return errors
    }

    /**
     * Validates a single action.
     */
    private fun validateAction(action: Action, index: Int): List<String> {
        val errors = mutableListOf<String>()

        if (action.parameters.isBlank()) {
            errors.add("Action ${index + 1}: Parameters cannot be empty")
            return errors
        }

        try {
            if (!isValidJson(action.parameters)) {
                errors.add("Action ${index + 1}: Invalid parameters format (must be JSON object or array)")
            }
        } catch (e: Exception) {
            errors.add("Action ${index + 1}: Failed to parse parameters - ${e.message}")
        }

        return errors
    }

    /**
     * Checks for conflicting actions in the same rule.
     */
    private fun checkForConflictingActions(actions: List<Action>): List<String> {
        val errors = mutableListOf<String>()

        // WiFi enable/disable conflicts
        val hasEnableWifi = actions.any { it.type.name == "ENABLE_WIFI" }
        val hasDisableWifi = actions.any { it.type.name == "DISABLE_WIFI" }
        if (hasEnableWifi && hasDisableWifi) {
            errors.add("Conflicting actions: Cannot both enable and disable Wi‑Fi in the same rule")
        }

        // Bluetooth enable/disable conflicts
        val hasEnableBluetooth = actions.any { it.type.name == "ENABLE_BLUETOOTH" }
        val hasDisableBluetooth = actions.any { it.type.name == "DISABLE_BLUETOOTH" }
        if (hasEnableBluetooth && hasDisableBluetooth) {
            errors.add("Conflicting actions: Cannot both enable and disable Bluetooth in the same rule")
        }

        // Duplicate single‑instance actions
        val nonRepeatableTypes = setOf(
            "TOGGLE_SILENT_MODE",
            "ENABLE_DND",
            "DISABLE_DND"
        )

        val typeCounts = actions.groupingBy { it.type }.eachCount()
        typeCounts.forEach { (type, count) ->
            if (count > 1 && type.name in nonRepeatableTypes) {
                errors.add("Action type '${type.name}' cannot be used multiple times in the same rule")
            }
        }

        return errors
    }

    /**
     * Very basic JSON validation (shape only).
     */
    private fun isValidJson(json: String): Boolean {
        val trimmed = json.trim()
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                (trimmed.startsWith("[") && trimmed.endsWith("]"))
    }
}

/**
 * Result of rule validation.
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Failure(val errors: List<String>) : ValidationResult()

    fun isSuccess(): Boolean = this is Success
    fun isFailure(): Boolean = this is Failure

    fun getErrorMessage(): String = when (this) {
        is Success -> ""
        is Failure -> errors.joinToString("\n")
    }
}
