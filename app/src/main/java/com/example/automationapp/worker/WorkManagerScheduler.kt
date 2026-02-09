package com.example.automationapp.worker

import androidx.work.*
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.PeriodicCheckWorker
import com.example.automationapp.worker.RuleEvaluationWorker
import com.example.automationapp.worker.TriggerBasedWorker
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerScheduler @Inject constructor(
    private val workManager: WorkManager
) {

    /**
     * Schedule immediate rule evaluation
     */
    fun scheduleRuleEvaluation(
        ruleId: Long,
        requireNetwork: Boolean = false
    ) {
        val workRequest = RuleEvaluationWorker.buildWorkRequest(ruleId, requireNetwork)
        workManager.enqueueUniqueWork(
            "rule_eval_$ruleId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Schedule delayed rule evaluation
     */
    fun scheduleDelayedRuleEvaluation(
        ruleId: Long,
        delayMillis: Long
    ) {
        val workRequest = RuleEvaluationWorker.buildDelayedWorkRequest(ruleId, delayMillis)
        workManager.enqueueUniqueWork(
            "rule_eval_delayed_$ruleId",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel rule evaluation
     */
    fun cancelRuleEvaluation(ruleId: Long) {
        workManager.cancelUniqueWork("rule_eval_$ruleId")
        workManager.cancelUniqueWork("rule_eval_delayed_$ruleId")
        workManager.cancelAllWorkByTag("rule_$ruleId")
    }

    /**
     * Schedule periodic rule checks
     */
    fun schedulePeriodicChecks(intervalMinutes: Long = 15) {
        val workRequest = PeriodicCheckWorker.buildPeriodicWorkRequest(intervalMinutes)
        workManager.enqueueUniquePeriodicWork(
            PeriodicCheckWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    /**
     * Cancel periodic checks
     */
    fun cancelPeriodicChecks() {
        workManager.cancelUniqueWork(PeriodicCheckWorker.WORK_NAME)
    }

    /**
     * Update periodic check interval
     */
    fun updatePeriodicCheckInterval(intervalMinutes: Long) {
        cancelPeriodicChecks()
        schedulePeriodicChecks(intervalMinutes)
    }

    /**
     * Schedule evaluation based on trigger type
     */
    fun scheduleTriggerBasedEvaluation(triggerType: TriggerType) {
        val workRequest = TriggerBasedWorker.buildWorkRequest(triggerType)
        workManager.enqueueUniqueWork(
            "trigger_${triggerType.name}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    /**
     * Cancel all automation-related work
     */
    fun cancelAllWork() {
        workManager.cancelAllWorkByTag(RuleEvaluationWorker.WORK_TAG)
        workManager.cancelAllWorkByTag(PeriodicCheckWorker.WORK_NAME)
    }

    /**
     * Get work info for a specific rule
     */
    fun getRuleWorkInfo(ruleId: Long) =
        workManager.getWorkInfosForUniqueWorkLiveData("rule_eval_$ruleId")

    /**
     * Get periodic check work info
     */
    fun getPeriodicCheckWorkInfo() =
        workManager.getWorkInfosForUniqueWorkLiveData(PeriodicCheckWorker.WORK_NAME)

    /**
     * Observe work status
     */
    fun observeWorkStatus(tag: String) =
        workManager.getWorkInfosByTagLiveData(tag)
}
