package com.example.automationapp.receiver

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * BroadcastReceiver for Do Not Disturb (DND) state changes.
 *
 * Listens for NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED to detect
 * when DND mode is enabled or disabled.
 *
 * This receiver requires the ACCESS_NOTIFICATION_POLICY permission to be granted
 * via the system settings (Notification Policy Access).
 */
@AndroidEntryPoint
class DndReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

            val currentFilter = notificationManager.currentInterruptionFilter
            val isDndEnabled = currentFilter != NotificationManager.INTERRUPTION_FILTER_ALL

            Log.d(TAG, "DND state changed: isDndEnabled=$isDndEnabled, filter=$currentFilter")

            // Schedule evaluation for all rules with DND trigger
            scheduleTriggerEvaluation()
        }
    }

    private fun scheduleTriggerEvaluation() {
        try {
            workManagerScheduler.scheduleTriggerBasedEvaluation(TriggerType.DO_NOT_DISTURB)
            Log.d(TAG, "Scheduled DND trigger evaluation")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule DND trigger evaluation", e)
        }
    }

    companion object {
        private const val TAG = "DndReceiver"

        /**
         * Check if notification policy access is granted.
         * Required for DND trigger to work properly.
         */
        fun hasNotificationPolicyAccess(context: Context): Boolean {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager
            return notificationManager.isNotificationPolicyAccessGranted
        }

        /**
         * Get intent to open notification policy access settings.
         */
        fun getNotificationPolicyAccessIntent(): Intent {
            return Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        }
    }
}

