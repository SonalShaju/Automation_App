package com.example.automationapp.data.local.entity

import org.junit.Assert.*
import org.junit.Test

class ExecutionLogTest {

    @Test
    fun `executionLog creation with default values`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = true,
            status = ExecutionStatus.SUCCESS
        )

        assertEquals(0L, log.id)
        assertEquals(1L, log.ruleId)
        assertTrue(log.executedAt > 0)
        assertTrue(log.success)
        assertEquals(ExecutionStatus.SUCCESS, log.status)
        assertNull(log.errorMessage)
        assertNull(log.errorCode)
        assertEquals(0L, log.executionDurationMs)
        assertNull(log.triggeredBy)
        assertEquals(0, log.actionsExecuted)
        assertEquals(0, log.actionsFailed)
        assertNull(log.details)
    }

    @Test
    fun `executionLog creation with custom values`() {
        val customTime = 1699900000000L
        val log = ExecutionLog(
            id = 5L,
            ruleId = 10L,
            executedAt = customTime,
            success = false,
            status = ExecutionStatus.FAILED,
            errorMessage = "Connection timeout",
            errorCode = "ERR_TIMEOUT",
            executionDurationMs = 5000L,
            triggeredBy = "TIME_BASED",
            actionsExecuted = 3,
            actionsFailed = 2,
            details = """{"action1": "success", "action2": "failed"}"""
        )

        assertEquals(5L, log.id)
        assertEquals(10L, log.ruleId)
        assertEquals(customTime, log.executedAt)
        assertFalse(log.success)
        assertEquals(ExecutionStatus.FAILED, log.status)
        assertEquals("Connection timeout", log.errorMessage)
        assertEquals("ERR_TIMEOUT", log.errorCode)
        assertEquals(5000L, log.executionDurationMs)
        assertEquals("TIME_BASED", log.triggeredBy)
        assertEquals(3, log.actionsExecuted)
        assertEquals(2, log.actionsFailed)
        assertNotNull(log.details)
    }

    @Test
    fun `executionLog equality test`() {
        val log1 = ExecutionLog(
            id = 1L,
            ruleId = 1L,
            executedAt = 1000L,
            success = true,
            status = ExecutionStatus.SUCCESS
        )
        val log2 = ExecutionLog(
            id = 1L,
            ruleId = 1L,
            executedAt = 1000L,
            success = true,
            status = ExecutionStatus.SUCCESS
        )

        assertEquals(log1, log2)
    }

    @Test
    fun `executionLog copy test`() {
        val originalLog = ExecutionLog(
            id = 1L,
            ruleId = 1L,
            success = true,
            status = ExecutionStatus.SUCCESS
        )

        val copiedLog = originalLog.copy(success = false, status = ExecutionStatus.FAILED)

        assertFalse(copiedLog.success)
        assertEquals(ExecutionStatus.FAILED, copiedLog.status)
        assertEquals(originalLog.id, copiedLog.id)
        assertEquals(originalLog.ruleId, copiedLog.ruleId)
    }

    @Test
    fun `partial success log`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = true,
            status = ExecutionStatus.PARTIAL_SUCCESS,
            actionsExecuted = 5,
            actionsFailed = 2
        )

        assertEquals(ExecutionStatus.PARTIAL_SUCCESS, log.status)
        assertEquals(5, log.actionsExecuted)
        assertEquals(2, log.actionsFailed)
    }

    @Test
    fun `cancelled log`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = false,
            status = ExecutionStatus.CANCELLED,
            errorMessage = "User cancelled execution"
        )

        assertEquals(ExecutionStatus.CANCELLED, log.status)
        assertFalse(log.success)
        assertEquals("User cancelled execution", log.errorMessage)
    }

    @Test
    fun `timeout log`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = false,
            status = ExecutionStatus.TIMEOUT,
            executionDurationMs = 30000L
        )

        assertEquals(ExecutionStatus.TIMEOUT, log.status)
        assertEquals(30000L, log.executionDurationMs)
    }

    @Test
    fun `permission denied log`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = false,
            status = ExecutionStatus.PERMISSION_DENIED,
            errorMessage = "Missing WRITE_SETTINGS permission"
        )

        assertEquals(ExecutionStatus.PERMISSION_DENIED, log.status)
        assertNotNull(log.errorMessage)
    }

    @Test
    fun `invalid configuration log`() {
        val log = ExecutionLog(
            ruleId = 1L,
            success = false,
            status = ExecutionStatus.INVALID_CONFIGURATION,
            errorCode = "ERR_INVALID_PARAMS"
        )

        assertEquals(ExecutionStatus.INVALID_CONFIGURATION, log.status)
        assertEquals("ERR_INVALID_PARAMS", log.errorCode)
    }
}

class ExecutionStatusTest {

    @Test
    fun `all execution status types exist`() {
        val expectedStatuses = listOf(
            ExecutionStatus.SUCCESS,
            ExecutionStatus.PARTIAL_SUCCESS,
            ExecutionStatus.FAILED,
            ExecutionStatus.CANCELLED,
            ExecutionStatus.TIMEOUT,
            ExecutionStatus.PERMISSION_DENIED,
            ExecutionStatus.INVALID_CONFIGURATION
        )

        assertEquals(7, ExecutionStatus.entries.size)
        expectedStatuses.forEach { status ->
            assertTrue(ExecutionStatus.entries.contains(status))
        }
    }

    @Test
    fun `execution status valueOf works correctly`() {
        assertEquals(ExecutionStatus.SUCCESS, ExecutionStatus.valueOf("SUCCESS"))
        assertEquals(ExecutionStatus.FAILED, ExecutionStatus.valueOf("FAILED"))
        assertEquals(ExecutionStatus.PARTIAL_SUCCESS, ExecutionStatus.valueOf("PARTIAL_SUCCESS"))
        assertEquals(ExecutionStatus.TIMEOUT, ExecutionStatus.valueOf("TIMEOUT"))
    }

    @Test
    fun `execution status name returns correct string`() {
        assertEquals("SUCCESS", ExecutionStatus.SUCCESS.name)
        assertEquals("FAILED", ExecutionStatus.FAILED.name)
        assertEquals("PARTIAL_SUCCESS", ExecutionStatus.PARTIAL_SUCCESS.name)
        assertEquals("PERMISSION_DENIED", ExecutionStatus.PERMISSION_DENIED.name)
    }

    @Test
    fun `success status ordinal is first`() {
        assertEquals(0, ExecutionStatus.SUCCESS.ordinal)
    }
}

