package com.example.automationapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.service.RuleSchedulingManager
import com.example.automationapp.service.AlarmScheduler
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * BroadcastReceiver for time-based alarms
 * Triggered by AlarmManager when a scheduled time alarm fires
 */
@AndroidEntryPoint
class TimeAlarmReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @Inject
    lateinit var ruleSchedulingManager: RuleSchedulingManager

    override fun onReceive(context: Context, intent: Intent) {
        val ruleId = intent.getLongExtra(AlarmScheduler.EXTRA_RULE_ID, -1)
        val triggerId = intent.getLongExtra(AlarmScheduler.EXTRA_TRIGGER_ID, -1)

        Log.d(TAG, "Time alarm received for rule: $ruleId, trigger: $triggerId")

        if (ruleId == -1L) {
            Log.e(TAG, "Invalid rule ID in alarm intent")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Schedule immediate evaluation for this specific rule
                workManagerScheduler.scheduleRuleEvaluation(ruleId)

                // Reschedule the alarm for the next occurrence
                ruleSchedulingManager.scheduleRuleTriggers(ruleId)

                Log.d(TAG, "Scheduled rule evaluation and rescheduled alarm for rule $ruleId")
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling rule evaluation", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "TimeAlarmReceiver"
    }
}

