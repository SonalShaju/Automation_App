package com.example.automationapp.domain.usecase

import org.junit.Assert.*
import org.junit.Test

class ExecutionResultTest {

    @Test
    fun `executionResult creation for successful execution`() {
        val result = ExecutionResult(
            executed = true,
            reason = "Executed successfully",
            actionsExecuted = 3,
            executionTimeMs = 150L
        )

        assertTrue(result.executed)
        assertEquals("Executed successfully", result.reason)
        assertEquals(3, result.actionsExecuted)
        assertEquals(150L, result.executionTimeMs)
        assertNull(result.errors)
    }

    @Test
    fun `executionResult creation with errors`() {
        val errors = listOf("Action 1 failed", "Action 3 timeout")
        val result = ExecutionResult(
            executed = true,
            reason = "Some actions failed",
            actionsExecuted = 3,
            executionTimeMs = 500L,
            errors = errors
        )

        assertTrue(result.executed)
        assertEquals("Some actions failed", result.reason)
        assertEquals(3, result.actionsExecuted)
        assertEquals(500L, result.executionTimeMs)
        assertNotNull(result.errors)
        assertEquals(2, result.errors!!.size)
        assertTrue(result.errors!!.contains("Action 1 failed"))
    }

    @Test
    fun `executionResult for non-execution due to disabled rule`() {
        val result = ExecutionResult(
            executed = false,
            reason = "Rule is disabled",
            actionsExecuted = 0,
            executionTimeMs = 0L
        )

        assertFalse(result.executed)
        assertEquals("Rule is disabled", result.reason)
        assertEquals(0, result.actionsExecuted)
        assertEquals(0L, result.executionTimeMs)
        assertNull(result.errors)
    }

    @Test
    fun `executionResult for trigger conditions not met`() {
        val result = ExecutionResult(
            executed = false,
            reason = "Trigger conditions not met",
            actionsExecuted = 0,
            executionTimeMs = 25L
        )

        assertFalse(result.executed)
        assertEquals("Trigger conditions not met", result.reason)
        assertEquals(0, result.actionsExecuted)
    }

    @Test
    fun `executionResult for no triggers configured`() {
        val result = ExecutionResult(
            executed = false,
            reason = "No triggers configured",
            actionsExecuted = 0,
            executionTimeMs = 0L
        )

        assertFalse(result.executed)
        assertTrue(result.reason.contains("triggers"))
    }

    @Test
    fun `executionResult for no actions configured`() {
        val result = ExecutionResult(
            executed = false,
            reason = "No actions configured",
            actionsExecuted = 0,
            executionTimeMs = 0L
        )

        assertFalse(result.executed)
        assertTrue(result.reason.contains("actions"))
    }

    @Test
    fun `executionResult equality test`() {
        val result1 = ExecutionResult(
            executed = true,
            reason = "Success",
            actionsExecuted = 2,
            executionTimeMs = 100L
        )
        val result2 = ExecutionResult(
            executed = true,
            reason = "Success",
            actionsExecuted = 2,
            executionTimeMs = 100L
        )

        assertEquals(result1, result2)
    }

    @Test
    fun `executionResult copy test`() {
        val originalResult = ExecutionResult(
            executed = true,
            reason = "Success",
            actionsExecuted = 2,
            executionTimeMs = 100L
        )

        val copiedResult = originalResult.copy(actionsExecuted = 5)

        assertEquals(5, copiedResult.actionsExecuted)
        assertEquals(originalResult.executed, copiedResult.executed)
        assertEquals(originalResult.reason, copiedResult.reason)
    }

    @Test
    fun `executionResult with empty error list`() {
        val result = ExecutionResult(
            executed = true,
            reason = "Partial success",
            actionsExecuted = 3,
            executionTimeMs = 200L,
            errors = emptyList()
        )

        assertNotNull(result.errors)
        assertTrue(result.errors!!.isEmpty())
    }

    @Test
    fun `executionResult hasErrors check`() {
        val resultWithErrors = ExecutionResult(
            executed = true,
            reason = "Some failures",
            actionsExecuted = 2,
            executionTimeMs = 100L,
            errors = listOf("Error 1")
        )

        val resultWithoutErrors = ExecutionResult(
            executed = true,
            reason = "Success",
            actionsExecuted = 2,
            executionTimeMs = 100L,
            errors = null
        )

        assertTrue(resultWithErrors.errors?.isNotEmpty() == true)
        assertTrue(resultWithoutErrors.errors.isNullOrEmpty())
    }
}

