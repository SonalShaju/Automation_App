package com.example.automationapp.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.worker.WorkManagerScheduler
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Broadcast receiver for network connectivity events
 */
@AndroidEntryPoint
class ConnectivityReceiver : BroadcastReceiver() {

    @Inject
    lateinit var workManagerScheduler: WorkManagerScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ConnectivityManager.CONNECTIVITY_ACTION -> {
                val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
                        as ConnectivityManager

                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                if (capabilities != null) {
                    when {
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                            Log.d(TAG, "WiFi connected")
                            scheduleTriggerEvaluation(TriggerType.WIFI_CONNECTED)
                        }
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                            Log.d(TAG, "Mobile data connected")
                            // Cellular connectivity changed - WiFi might have disconnected
                            scheduleTriggerEvaluation(TriggerType.WIFI_CONNECTED)
                        }
                    }
                } else {
                    Log.d(TAG, "Network disconnected")
                    scheduleTriggerEvaluation(TriggerType.WIFI_CONNECTED)
                }
            }
            WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                val wifiState = intent.getIntExtra(
                    WifiManager.EXTRA_WIFI_STATE,
                    WifiManager.WIFI_STATE_UNKNOWN
                )

                when (wifiState) {
                    WifiManager.WIFI_STATE_ENABLED -> {
                        Log.d(TAG, "WiFi enabled")
                        scheduleTriggerEvaluation(TriggerType.WIFI_CONNECTED)
                    }
                    WifiManager.WIFI_STATE_DISABLED -> {
                        Log.d(TAG, "WiFi disabled")
                        scheduleTriggerEvaluation(TriggerType.WIFI_CONNECTED)
                    }
                }
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
        private const val TAG = "ConnectivityReceiver"
    }
}
