package com.example.automationapp.domain.model

import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.data.local.entity.TriggerType
import org.junit.Assert.*
import org.junit.Test

class RuleDetailsTest {

    @Test
    fun `ruleDetails creation with minimum data`() {
        val rule = createTestRule()
        val triggers = listOf(createTestTrigger())
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertEquals(rule, ruleDetails.rule)
        assertEquals(triggers, ruleDetails.triggers)
        assertEquals(actions, ruleDetails.actions)
        assertEquals(1, ruleDetails.triggerCount)
        assertEquals(1, ruleDetails.actionCount)
        assertFalse(ruleDetails.hasLocationTrigger)
        assertFalse(ruleDetails.hasTimeTrigger)
    }

    @Test
    fun `ruleDetails with location trigger flag`() {
        val rule = createTestRule()
        val triggers = listOf(
            createTestTrigger(type = TriggerType.LOCATION_BASED)
        )
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions,
            hasLocationTrigger = true
        )

        assertTrue(ruleDetails.hasLocationTrigger)
        assertFalse(ruleDetails.hasTimeTrigger)
    }

    @Test
    fun `ruleDetails with time trigger flag`() {
        val rule = createTestRule()
        val triggers = listOf(
            createTestTrigger(type = TriggerType.TIME_BASED)
        )
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions,
            hasTimeTrigger = true
        )

        assertFalse(ruleDetails.hasLocationTrigger)
        assertTrue(ruleDetails.hasTimeTrigger)
    }

    @Test
    fun `isValid returns true when triggers and actions are present`() {
        val rule = createTestRule()
        val triggers = listOf(createTestTrigger())
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertTrue(ruleDetails.isValid)
    }

    @Test
    fun `isValid returns false when triggers are empty`() {
        val rule = createTestRule()
        val triggers = emptyList<Trigger>()
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertFalse(ruleDetails.isValid)
    }

    @Test
    fun `isValid returns false when actions are empty`() {
        val rule = createTestRule()
        val triggers = listOf(createTestTrigger())
        val actions = emptyList<Action>()

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertFalse(ruleDetails.isValid)
    }

    @Test
    fun `summary returns correct format for enabled rule`() {
        val rule = createTestRule(name = "My Rule", isEnabled = true)
        val triggers = listOf(createTestTrigger(), createTestTrigger(id = 2))
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        val summary = ruleDetails.summary
        assertTrue(summary.contains("My Rule"))
        assertTrue(summary.contains("2 triggers"))
        assertTrue(summary.contains("1 action"))
        assertFalse(summary.contains("Disabled"))
    }

    @Test
    fun `summary returns correct format for disabled rule`() {
        val rule = createTestRule(name = "Disabled Rule", isEnabled = false)
        val triggers = listOf(createTestTrigger())
        val actions = listOf(createTestAction(), createTestAction(id = 2))

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        val summary = ruleDetails.summary
        assertTrue(summary.contains("Disabled Rule"))
        assertTrue(summary.contains("1 trigger"))
        assertTrue(summary.contains("2 actions"))
        assertTrue(summary.contains("(Disabled)"))
    }

    @Test
    fun `enabledActions filters correctly`() {
        val rule = createTestRule()
        val triggers = listOf(createTestTrigger())
        val actions = listOf(
            createTestAction(id = 1, isEnabled = true),
            createTestAction(id = 2, isEnabled = false),
            createTestAction(id = 3, isEnabled = true)
        )

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        val enabledActions = ruleDetails.enabledActions
        assertEquals(2, enabledActions.size)
        assertTrue(enabledActions.all { it.isEnabled })
    }

    @Test
    fun `activeTriggers filters correctly`() {
        val rule = createTestRule()
        val triggers = listOf(
            createTestTrigger(id = 1, isActive = true),
            createTestTrigger(id = 2, isActive = false),
            createTestTrigger(id = 3, isActive = true)
        )
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        val activeTriggers = ruleDetails.activeTriggers
        assertEquals(2, activeTriggers.size)
        assertTrue(activeTriggers.all { it.isActive })
    }

    @Test
    fun `triggerCount equals triggers size`() {
        val rule = createTestRule()
        val triggers = listOf(
            createTestTrigger(id = 1),
            createTestTrigger(id = 2),
            createTestTrigger(id = 3)
        )
        val actions = listOf(createTestAction())

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertEquals(3, ruleDetails.triggerCount)
        assertEquals(triggers.size, ruleDetails.triggerCount)
    }

    @Test
    fun `actionCount equals actions size`() {
        val rule = createTestRule()
        val triggers = listOf(createTestTrigger())
        val actions = listOf(
            createTestAction(id = 1),
            createTestAction(id = 2)
        )

        val ruleDetails = RuleDetails(
            rule = rule,
            triggers = triggers,
            actions = actions
        )

        assertEquals(2, ruleDetails.actionCount)
        assertEquals(actions.size, ruleDetails.actionCount)
    }

    // ==================== Helper Methods ====================

    private fun createTestRule(
        id: Long = 1L,
        name: String = "Test Rule",
        isEnabled: Boolean = true
    ): AutomationRule {
        return AutomationRule(
            id = id,
            name = name,
            description = "Test Description",
            isEnabled = isEnabled,
            createdAt = 1000L,
            updatedAt = 1000L
        )
    }

    private fun createTestTrigger(
        id: Long = 1L,
        type: TriggerType = TriggerType.TIME_BASED,
        isActive: Boolean = true
    ): Trigger {
        return Trigger(
            id = id,
            ruleId = 1L,
            type = type,
            parameters = "{}",
            isActive = isActive,
            createdAt = 1000L
        )
    }

    private fun createTestAction(
        id: Long = 1L,
        isEnabled: Boolean = true
    ): Action {
        return Action(
            id = id,
            ruleId = 1L,
            type = ActionType.TOGGLE_SILENT_MODE,
            parameters = "{}",
            isEnabled = isEnabled,
            createdAt = 1000L
        )
    }
}

