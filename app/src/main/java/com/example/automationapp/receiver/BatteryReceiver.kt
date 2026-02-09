package com.example.automationapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.util.Log
import android.widget.Toast
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver for battery-related events
 */
@AndroidEntryPoint
class BatteryReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BATTERY_LOW -> {
                Log.d(TAG, "Battery low")
                scheduleTriggerEvaluation(TriggerType.BATTERY_LEVEL)
            }
            Intent.ACTION_BATTERY_OKAY -> {
                Log.d(TAG, "Battery okay")
                scheduleTriggerEvaluation(TriggerType.BATTERY_LEVEL)
            }
            Intent.ACTION_POWER_CONNECTED -> {
                Log.d(TAG, "Power connected")
                Toast.makeText(context, "Charging trigger activated: Power connected", Toast.LENGTH_SHORT).show()
                val batteryStatus = context.registerReceiver(
                    null,
                    android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )
                val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL

                if (isCharging) {
                    scheduleTriggerEvaluation(TriggerType.CHARGING_STATUS)
                }
            }
            Intent.ACTION_POWER_DISCONNECTED -> {
                Log.d(TAG, "Power disconnected")
                Toast.makeText(context, "Charging trigger activated: Power disconnected", Toast.LENGTH_SHORT).show()
                scheduleTriggerEvaluation(TriggerType.CHARGING_STATUS)
            }
        }
    }

    private fun scheduleTriggerEvaluation(triggerType: TriggerType) {
        try {
            workManagerScheduler.scheduleTriggerBasedEvaluation(triggerType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule trigger evaluation", e)
        }
    }

    companion object {
        private const val TAG = "BatteryReceiver"
    }
}
