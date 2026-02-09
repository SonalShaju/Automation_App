package com.example.automationapp.domain.model

import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.data.local.entity.Condition
import com.example.automationapp.data.local.entity.Trigger

/**
 * Data class containing complete details of an automation rule
 * with additional computed properties
 */
data class RuleDetails(
    val rule: AutomationRule,
    val triggers: List<Trigger>,
    val actions: List<Action>,
    val conditions: List<Condition> = emptyList(),
    val triggerCount: Int = triggers.size,
    val actionCount: Int = actions.size,
    val conditionCount: Int = conditions.size,
    val hasLocationTrigger: Boolean = false,
    val hasTimeTrigger: Boolean = false
) {
    /**
     * Check if the rule is valid (has both triggers and actions)
     */
    val isValid: Boolean
        get() = triggers.isNotEmpty() && actions.isNotEmpty()

    /**
     * Get a summary description of the rule
     */
    val summary: String
        get() = buildString {
            append("${rule.name}: ")
            append("$triggerCount trigger${if (triggerCount != 1) "s" else ""}, ")
            append("$actionCount action${if (actionCount != 1) "s" else ""}")
            if (!rule.isEnabled) {
                append(" (Disabled)")
            }
        }

    /**
     * Get enabled actions only
     */
    val enabledActions: List<Action>
        get() = actions.filter { it.isEnabled }

    /**
     * Get active triggers only
     */
    val activeTriggers: List<Trigger>
        get() = triggers.filter { it.isActive }

    /**
     * Get active conditions only
     */
    val activeConditions: List<Condition>
        get() = conditions.filter { it.isActive }
}
