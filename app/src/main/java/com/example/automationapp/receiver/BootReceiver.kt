package com.example.automationapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automationapp.service.RuleSchedulingManager
import com.example.automationapp.service.TriggerMonitorService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Receives boot completed broadcast and starts the foreground service
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit var ruleSchedulingManager: RuleSchedulingManager

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context != null) {
            Log.d("BootReceiver", "Device boot completed, starting TriggerMonitorService")

            // Start the foreground service to monitor triggers
            TriggerMonitorService.start(context)

            Log.d("BootReceiver", "TriggerMonitorService start requested")
        }
    }
}


