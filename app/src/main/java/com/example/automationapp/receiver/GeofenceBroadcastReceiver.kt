package com.example.automationapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver for geofence events
 */
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return

        if (geofencingEvent.hasError()) {
            Log.e(TAG, "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences

        when (geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_ENTER -> {
                Log.d(TAG, "Entered geofence")
                triggeringGeofences?.forEach { geofence ->
                    Log.d(TAG, "Entered geofence: ${geofence.requestId}")
                    scheduleRuleEvaluationFromGeofence(geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_EXIT -> {
                Log.d(TAG, "Exited geofence")
                triggeringGeofences?.forEach { geofence ->
                    Log.d(TAG, "Exited geofence: ${geofence.requestId}")
                    scheduleRuleEvaluationFromGeofence(geofence.requestId)
                }
            }
            Geofence.GEOFENCE_TRANSITION_DWELL -> {
                Log.d(TAG, "Dwelling in geofence")
                triggeringGeofences?.forEach { geofence ->
                    scheduleRuleEvaluationFromGeofence(geofence.requestId)
                }
            }
        }
    }

    /**
     * Extract rule ID from geofence ID (format: "rule_{ruleId}_trigger_{triggerId}")
     * and schedule evaluation for that specific rule
     */
    private fun scheduleRuleEvaluationFromGeofence(geofenceId: String) {
        try {
            // Extract rule ID from geofence ID pattern: "rule_{ruleId}_trigger_{triggerId}"
            val regex = Regex("rule_(\\d+)_trigger_(\\d+)")
            val matchResult = regex.find(geofenceId)

            if (matchResult != null) {
                val ruleId = matchResult.groupValues[1].toLongOrNull()
                if (ruleId != null) {
                    Log.d(TAG, "Scheduling evaluation for rule $ruleId from geofence $geofenceId")
                    workManagerScheduler.scheduleRuleEvaluation(ruleId)
                    return
                }
            }

            // Fallback to general location-based trigger evaluation
            Log.d(TAG, "Could not extract rule ID from geofence $geofenceId, using general evaluation")
            workManagerScheduler.scheduleTriggerBasedEvaluation(TriggerType.LOCATION_BASED)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule rule evaluation from geofence", e)
        }
    }

    companion object {
        private const val TAG = "GeofenceReceiver"
    }
}
