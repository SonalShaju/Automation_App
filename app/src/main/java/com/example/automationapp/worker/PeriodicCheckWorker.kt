package com.example.automationapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.domain.usecase.ExecuteRuleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

@HiltWorker
class PeriodicCheckWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AutomationRepository,
    private val executeRuleUseCase: ExecuteRuleUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic rule evaluation")

            // Get all enabled rules
            val enabledRules = repository.getEnabledRules().first()

            if (enabledRules.isEmpty()) {
                Log.d(TAG, "No enabled rules to evaluate")
                return Result.success(
                    workDataOf(RULES_EVALUATED to 0)
                )
            }

            var successCount = 0
            var failureCount = 0

            // Evaluate each rule
            enabledRules.forEach { rule ->
                try {
                    val result = executeRuleUseCase(
                        ruleId = rule.id,
                        triggeredBy = "PeriodicCheck"
                    )

                    result.fold(
                        onSuccess = { executionResult ->
                            if (executionResult.executed) {
                                successCount++
                                Log.d(TAG, "Rule ${rule.name} executed successfully")
                            }
                        },
                        onFailure = { error ->
                            failureCount++
                            Log.e(TAG, "Failed to execute rule ${rule.name}: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    failureCount++
                    Log.e(TAG, "Error evaluating rule ${rule.name}", e)
                }
            }

            Log.d(TAG, "Periodic check completed: $successCount successful, $failureCount failed")

            Result.success(
                workDataOf(
                    RULES_EVALUATED to enabledRules.size,
                    RULES_EXECUTED to successCount,
                    RULES_FAILED to failureCount
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Periodic check failed", e)

            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(ERROR_MESSAGE to (e.message ?: "Unknown error"))
                )
            }
        }
    }

    companion object {
        private const val TAG = "PeriodicCheckWorker"
        const val WORK_NAME = "periodic_rule_check"
        const val RULES_EVALUATED = "RULES_EVALUATED"
        const val RULES_EXECUTED = "RULES_EXECUTED"
        const val RULES_FAILED = "RULES_FAILED"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        private const val MAX_RETRY_ATTEMPTS = 2

        fun buildPeriodicWorkRequest(
            intervalMinutes: Long = 15
        ): PeriodicWorkRequest {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .setRequiresDeviceIdle(false)
                .build()

            return PeriodicWorkRequestBuilder<PeriodicCheckWorker>(
                repeatInterval = intervalMinutes,
                repeatIntervalTimeUnit = TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setInitialDelay(1, TimeUnit.MINUTES) // Wait 1 minute before first run
                .addTag(WORK_NAME)
                .build()
        }
    }
}
