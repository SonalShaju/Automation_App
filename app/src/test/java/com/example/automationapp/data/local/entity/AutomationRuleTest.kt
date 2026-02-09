package com.example.automationapp.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class AutomationRuleTest {

    @Test
    fun `rule creation with default values`() {
        val rule = AutomationRule(
            name = "Test Rule",
            description = "Test Description"
        )

        assertEquals(0L, rule.id)
        assertEquals("Test Rule", rule.name)
        assertEquals("Test Description", rule.description)
        assertTrue(rule.isEnabled)
        assertEquals(0, rule.executionCount)
        assertNull(rule.lastExecutedAt)
        assertTrue(rule.createdAt > 0)
        assertTrue(rule.updatedAt > 0)
    }

    @Test
    fun `rule creation with custom values`() {
        val customTime = 1699900000000L
        val rule = AutomationRule(
            id = 5L,
            name = "Custom Rule",
            description = "Custom Description",
            isEnabled = false,
            executionCount = 10,
            lastExecutedAt = customTime,
            createdAt = customTime,
            updatedAt = customTime
        )

        assertEquals(5L, rule.id)
        assertEquals("Custom Rule", rule.name)
        assertEquals("Custom Description", rule.description)
        assertFalse(rule.isEnabled)
        assertEquals(10, rule.executionCount)
        assertEquals(customTime, rule.lastExecutedAt)
        assertEquals(customTime, rule.createdAt)
        assertEquals(customTime, rule.updatedAt)
    }

    @Test
    fun `rule equality test`() {
        val rule1 = AutomationRule(
            id = 1L,
            name = "Test",
            description = "Desc",
            createdAt = 1000L,
            updatedAt = 1000L
        )
        val rule2 = AutomationRule(
            id = 1L,
            name = "Test",
            description = "Desc",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        assertEquals(rule1, rule2)
    }

    @Test
    fun `rule copy test`() {
        val originalRule = AutomationRule(
            id = 1L,
            name = "Original",
            description = "Original Desc"
        )

        val copiedRule = originalRule.copy(name = "Copied")

        assertEquals("Copied", copiedRule.name)
        assertEquals(originalRule.id, copiedRule.id)
        assertEquals(originalRule.description, copiedRule.description)
    }

    @Test
    fun `rule hashCode consistency`() {
        val rule = AutomationRule(
            id = 1L,
            name = "Test",
            description = "Desc",
            createdAt = 1000L,
            updatedAt = 1000L
        )

        val hashCode1 = rule.hashCode()
        val hashCode2 = rule.hashCode()

        assertEquals(hashCode1, hashCode2)
    }
}

