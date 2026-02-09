package com.example.automationapp.receiver

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver for headphone/headset connection events
 */
@AndroidEntryPoint
class HeadsetReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    @SuppressLint("MissingPermission")
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_HEADSET_PLUG -> {
                val state = intent.getIntExtra("state", -1)
                when (state) {
                    0 -> {
                        Log.d(TAG, "Headset unplugged")
                        scheduleTriggerEvaluation(false)
                    }
                    1 -> {
                        Log.d(TAG, "Headset plugged")
                        scheduleTriggerEvaluation(true)
                    }
                }
            }
            BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(
                    BluetoothHeadset.EXTRA_STATE,
                    BluetoothHeadset.STATE_DISCONNECTED
                )
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

                when (state) {
                    BluetoothHeadset.STATE_CONNECTED -> {
                        Log.d(TAG, "Bluetooth headset connected: ${device?.name}")
                        scheduleTriggerEvaluation(true)
                    }
                    BluetoothHeadset.STATE_DISCONNECTED -> {
                        Log.d(TAG, "Bluetooth headset disconnected: ${device?.name}")
                        scheduleTriggerEvaluation(false)
                    }
                }
            }
        }
    }

    private fun scheduleTriggerEvaluation(connected: Boolean) {
        try {
            workManagerScheduler.scheduleTriggerBasedEvaluation(
                TriggerType.HEADPHONES_CONNECTED
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule trigger evaluation", e)
        }
    }

    companion object {
        private const val TAG = "HeadsetReceiver"
    }
}
