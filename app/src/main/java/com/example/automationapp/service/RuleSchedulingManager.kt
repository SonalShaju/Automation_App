package com.example.automationapp.service

import android.util.Log
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.worker.WorkManagerScheduler
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager responsible for scheduling all rule triggers (alarms, geofences, etc.)
 * This ensures triggers are properly registered when rules are created or enabled.
 */
@Singleton
class RuleSchedulingManager @Inject constructor(
    private val repository: AutomationRepository,
    private val alarmScheduler: AlarmScheduler,
    private val geofenceManager: GeofenceManager,
    private val workManagerScheduler: WorkManagerScheduler,
    private val json: Json
) {
    companion object {
        private const val TAG = "RuleSchedulingManager"
    }

    /**
     * Schedule all triggers for a newly created or enabled rule
     */
    suspend fun scheduleRuleTriggers(ruleId: Long) {
        try {
            val triggers = repository.getTriggersForRule(ruleId).first()
            triggers.forEach { trigger ->
                scheduleTrigger(ruleId, trigger)
            }
            Log.d(TAG, "Scheduled ${triggers.size} triggers for rule $ruleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling triggers for rule $ruleId", e)
        }
    }

    /**
     * Cancel all triggers for a disabled or deleted rule
     */
    suspend fun cancelRuleTriggers(ruleId: Long) {
        try {
            val triggers = repository.getTriggersForRule(ruleId).first()
            triggers.forEach { trigger ->
                cancelTrigger(ruleId, trigger)
            }
            workManagerScheduler.cancelRuleEvaluation(ruleId)
            Log.d(TAG, "Cancelled all triggers for rule $ruleId")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling triggers for rule $ruleId", e)
        }
    }

    /**
     * Schedule a single trigger
     */
    private suspend fun scheduleTrigger(ruleId: Long, trigger: Trigger) {
        when (trigger.type) {
            TriggerType.TIME_BASED -> scheduleTimeTrigger(ruleId, trigger)
            TriggerType.LOCATION_BASED -> scheduleLocationTrigger(ruleId, trigger)
            else -> {
                // Other triggers are handled by broadcast receivers or periodic checks
                Log.d(TAG, "Trigger type ${trigger.type} handled by broadcast/periodic checks")
            }
        }
    }

    /**
     * Cancel a single trigger
     */
    private suspend fun cancelTrigger(ruleId: Long, trigger: Trigger) {
        when (trigger.type) {
            TriggerType.TIME_BASED -> {
                alarmScheduler.cancelAllAlarmsForRule(ruleId)
            }
            TriggerType.LOCATION_BASED -> {
                geofenceManager.removeGeofence("rule_${ruleId}_trigger_${trigger.id}")
            }
            else -> {
                // Other triggers don't need explicit cancellation
            }
        }
    }

    /**
     * Schedule a time-based trigger using AlarmManager
     */
    private fun scheduleTimeTrigger(ruleId: Long, trigger: Trigger) {
        try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val time = params["time"]?.jsonPrimitive?.content ?: return
            val daysString = params["days"]?.jsonPrimitive?.content ?: ""
            val days = daysString.split(",").filter { it.isNotBlank() }

            val timeParts = time.split(":")
            if (timeParts.size != 2) return

            val hour = timeParts[0].toIntOrNull() ?: return
            val minute = timeParts[1].toIntOrNull() ?: return

            alarmScheduler.scheduleTimeAlarm(
                ruleId = ruleId,
                triggerId = trigger.id,
                hour = hour,
                minute = minute,
                days = days
            )
            Log.d(TAG, "Scheduled time trigger for rule $ruleId at $hour:$minute on days: $days")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling time trigger for rule $ruleId", e)
        }
    }

    /**
     * Schedule a location-based trigger using geofences
     */
    private suspend fun scheduleLocationTrigger(ruleId: Long, trigger: Trigger) {
        try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val latitude = params["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return
            val longitude = params["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return
            val radius = params["radius"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 100f

            val geofenceId = "rule_${ruleId}_trigger_${trigger.id}"
            geofenceManager.addGeofence(
                id = geofenceId,
                latitude = latitude,
                longitude = longitude,
                radius = radius
            )
            Log.d(TAG, "Scheduled location trigger for rule $ruleId at ($latitude, $longitude) radius=$radius")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling location trigger for rule $ruleId", e)
        }
    }

    /**
     * Reschedule all enabled rules' triggers
     * Call this on app start or after boot
     */
    suspend fun rescheduleAllEnabledRules() {
        try {
            val enabledRules = repository.getEnabledRules().first()
            enabledRules.forEach { rule ->
                scheduleRuleTriggers(rule.id)
            }
            Log.d(TAG, "Rescheduled triggers for ${enabledRules.size} enabled rules")
        } catch (e: Exception) {
            Log.e(TAG, "Error rescheduling all enabled rules", e)
        }
    }

    /**
     * Initialize periodic check worker and schedule all triggers
     * Should be called when the app starts
     */
    suspend fun initializeScheduling() {
        // Start periodic check worker
        workManagerScheduler.schedulePeriodicChecks()

        // Schedule all enabled rules' triggers
        rescheduleAllEnabledRules()

        Log.d(TAG, "Scheduling initialized")
    }
}

