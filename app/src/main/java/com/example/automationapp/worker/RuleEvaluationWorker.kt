package com.example.automationapp.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.automationapp.domain.usecase.ExecuteRuleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class RuleEvaluationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val executeRuleUseCase: ExecuteRuleUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val ruleId = inputData.getLong(KEY_RULE_ID, -1L)

            if (ruleId == -1L) {
                return Result.failure(
                    workDataOf(ERROR_MESSAGE to "Invalid rule ID")
                )
            }

            // Execute the rule
            val result = executeRuleUseCase(
                ruleId = ruleId,
                triggeredBy = "WorkManager"
            )

            result.fold(
                onSuccess = { executionResult ->
                    if (executionResult.executed) {
                        Result.success(
                            workDataOf(
                                RESULT_EXECUTED to true,
                                RESULT_ACTIONS_EXECUTED to executionResult.actionsExecuted,
                                RESULT_EXECUTION_TIME to executionResult.executionTimeMs
                            )
                        )
                    } else {
                        Result.success(
                            workDataOf(
                                RESULT_EXECUTED to false,
                                RESULT_REASON to executionResult.reason
                            )
                        )
                    }
                },
                onFailure = { error ->
                    Result.failure(
                        workDataOf(ERROR_MESSAGE to (error.message ?: "Unknown error"))
                    )
                }
            )
        } catch (e: Exception) {
            if (runAttemptCount < MAX_RETRY_ATTEMPTS) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(ERROR_MESSAGE to (e.message ?: "Failed after retries"))
                )
            }
        }
    }

    companion object {
        const val KEY_RULE_ID = "RULE_ID"
        const val WORK_TAG = "rule_evaluation"
        const val ERROR_MESSAGE = "ERROR_MESSAGE"
        const val RESULT_EXECUTED = "RESULT_EXECUTED"
        const val RESULT_ACTIONS_EXECUTED = "RESULT_ACTIONS_EXECUTED"
        const val RESULT_EXECUTION_TIME = "RESULT_EXECUTION_TIME"
        const val RESULT_REASON = "RESULT_REASON"
        private const val MAX_RETRY_ATTEMPTS = 3

        fun buildWorkRequest(
            ruleId: Long,
            requireNetwork: Boolean = false
        ): OneTimeWorkRequest {
            val inputData = workDataOf(KEY_RULE_ID to ruleId)

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .apply {
                    if (requireNetwork) {
                        setRequiredNetworkType(NetworkType.CONNECTED)
                    }
                }
                .build()

            return OneTimeWorkRequestBuilder<RuleEvaluationWorker>()
                .setInputData(inputData)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_TAG)
                .addTag("rule_$ruleId")
                .build()
        }

        fun buildDelayedWorkRequest(
            ruleId: Long,
            delayMillis: Long
        ): OneTimeWorkRequest {
            val inputData = workDataOf(KEY_RULE_ID to ruleId)

            return OneTimeWorkRequestBuilder<RuleEvaluationWorker>()
                .setInputData(inputData)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag(WORK_TAG)
                .addTag("rule_$ruleId")
                .build()
        }
    }
}
