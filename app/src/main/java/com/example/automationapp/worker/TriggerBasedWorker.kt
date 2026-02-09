package com.example.automationapp.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.domain.usecase.ExecuteRuleUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class TriggerBasedWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: AutomationRepository,
    private val executeRuleUseCase: ExecuteRuleUseCase
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val triggerTypeName = inputData.getString(KEY_TRIGGER_TYPE)
            if (triggerTypeName == null) {
                Log.e(TAG, "No trigger type provided in input data")
                return Result.failure()
            }

            val triggerType = TriggerType.valueOf(triggerTypeName)
            Log.d(TAG, "Processing trigger type: $triggerType")

            // Get rules with this trigger type
            val rules = repository.getRulesByTriggerType(triggerType).first()
            Log.d(TAG, "Found ${rules.size} rules with trigger type $triggerType")

            var executedCount = 0
            rules.forEach { rule ->
                Log.d(TAG, "Checking rule: ${rule.name} (id=${rule.id}, enabled=${rule.isEnabled})")
                if (rule.isEnabled) {
                    val result = executeRuleUseCase(
                        ruleId = rule.id,
                        triggeredBy = "TriggerType:${triggerType.name}"
                    )

                    result.onSuccess { executionResult ->
                        Log.d(TAG, "Rule ${rule.name} execution result: executed=${executionResult.executed}, reason=${executionResult.reason}")
                        if (executionResult.executed) {
                            executedCount++
                        }
                    }
                    result.onFailure { error ->
                        Log.e(TAG, "Rule ${rule.name} execution failed: ${error.message}")
                    }
                } else {
                    Log.d(TAG, "Rule ${rule.name} is disabled, skipping")
                }
            }

            Log.d(TAG, "Trigger-based evaluation complete: $executedCount rules executed")
            Result.success(
                workDataOf(RULES_EXECUTED to executedCount)
            )
        } catch (e: Exception) {
            Log.e(TAG, "TriggerBasedWorker failed", e)
            Result.failure()
        }
    }

    companion object {
        private const val TAG = "TriggerBasedWorker"
        const val KEY_TRIGGER_TYPE = "TRIGGER_TYPE"
        const val RULES_EXECUTED = "RULES_EXECUTED"

        fun buildWorkRequest(triggerType: TriggerType): OneTimeWorkRequest {
            return OneTimeWorkRequestBuilder<TriggerBasedWorker>()
                .setInputData(
                    workDataOf(KEY_TRIGGER_TYPE to triggerType.name)
                )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .addTag("trigger_${triggerType.name}")
                .build()
        }
    }
}
