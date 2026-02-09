package com.example.automationapp.data.local.dao

import androidx.room.*
import com.example.automationapp.data.local.entity.ExecutionLog
import com.example.automationapp.data.local.entity.ExecutionStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface ExecutionLogDao {

    @Query("SELECT * FROM execution_logs ORDER BY executed_at DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 100): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE rule_id = :ruleId ORDER BY executed_at DESC")
    fun getLogsByRuleId(ruleId: Long): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE rule_id = :ruleId ORDER BY executed_at DESC LIMIT :limit")
    fun getRecentLogsByRuleId(ruleId: Long, limit: Int = 50): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE id = :logId")
    suspend fun getLogById(logId: Long): ExecutionLog?

    @Query("SELECT * FROM execution_logs WHERE success = 0 ORDER BY executed_at DESC")
    fun getFailedLogs(): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE status = :status ORDER BY executed_at DESC")
    fun getLogsByStatus(status: ExecutionStatus): Flow<List<ExecutionLog>>

    @Query("SELECT * FROM execution_logs WHERE executed_at BETWEEN :startTime AND :endTime ORDER BY executed_at DESC")
    fun getLogsByDateRange(startTime: Long, endTime: Long): Flow<List<ExecutionLog>>

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId")
    suspend fun getExecutionCount(ruleId: Long): Int

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId AND success = 1")
    suspend fun getSuccessCount(ruleId: Long): Int

    @Query("SELECT COUNT(*) FROM execution_logs WHERE rule_id = :ruleId AND success = 0")
    suspend fun getFailureCount(ruleId: Long): Int

    @Query("SELECT AVG(execution_duration_ms) FROM execution_logs WHERE rule_id = :ruleId AND success = 1")
    suspend fun getAverageExecutionTime(ruleId: Long): Double?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: ExecutionLog): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<ExecutionLog>): List<Long>

    @Delete
    suspend fun deleteLog(log: ExecutionLog)

    @Query("DELETE FROM execution_logs WHERE id = :logId")
    suspend fun deleteLogById(logId: Long)

    @Query("DELETE FROM execution_logs WHERE rule_id = :ruleId")
    suspend fun deleteLogsByRuleId(ruleId: Long)

    @Query("DELETE FROM execution_logs WHERE executed_at < :timestamp")
    suspend fun deleteLogsOlderThan(timestamp: Long)

    @Query("DELETE FROM execution_logs")
    suspend fun deleteAllLogs()

    @Query("""
        DELETE FROM execution_logs 
        WHERE id NOT IN (
            SELECT id FROM execution_logs 
            ORDER BY executed_at DESC 
            LIMIT :keepCount
        )
    """)
    suspend fun trimLogs(keepCount: Int = 1000)

    // Statistics queries
    @Query("""
        SELECT rule_id, COUNT(*) as count, AVG(execution_duration_ms) as avg_duration
        FROM execution_logs 
        WHERE success = 1 
        GROUP BY rule_id
    """)
    fun getExecutionStatistics(): Flow<List<ExecutionStatistics>>
}

data class ExecutionStatistics(
    @ColumnInfo(name = "rule_id") val ruleId: Long,
    @ColumnInfo(name = "count") val count: Int,
    @ColumnInfo(name = "avg_duration") val avgDuration: Double
)