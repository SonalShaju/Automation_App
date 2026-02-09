package com.example.automationapp.service

import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.util.Log
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.domain.usecase.ExecuteRuleUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TriggerManager - Android 16 Compliant
 *
 * Centralized manager for all trigger listeners. This is a Singleton that:
 * 1. Registers/unregisters all broadcast receivers
 * 2. Manages NetworkCallback for WiFi detection
 * 3. Collects AccessibilityService events for app launch detection
 * 4. Collects NotificationListenerService events for notification triggers
 *
 * All triggers bypass WorkManager and execute rules IMMEDIATELY via ExecuteRuleUseCase.
 */
@Singleton
class TriggerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AutomationRepository,
    private val executeRuleUseCase: ExecuteRuleUseCase
) {
    companion object {
        private const val TAG = "TriggerManager"
    }

    // Coroutine scope for async operations
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Track initialization state
    private var isInitialized = false

    // System services
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Network callback for WiFi detection
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    // Track last known states to detect changes
    private var lastBatteryLevel: Int = -1
    private var lastChargingState: Boolean? = null
    private var lastWifiConnected: Boolean? = null
    private var lastForegroundApp: String? = null

    // ==================== BROADCAST RECEIVERS ====================

    /**
     * Battery broadcast receiver - handles BATTERY_CHANGED for level and charging status
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryChanged(intent)
                Intent.ACTION_POWER_CONNECTED -> handlePowerConnected()
                Intent.ACTION_POWER_DISCONNECTED -> handlePowerDisconnected()
            }
        }
    }


    /**
     * Bluetooth broadcast receiver - handles ACL_CONNECTED and ACL_DISCONNECTED
     */
    @Suppress("DEPRECATION")
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

            // Get device info safely (name requires permission on Android 12+)
            val deviceName = try {
                device?.name ?: "Unknown"
            } catch (e: SecurityException) {
                "Unknown"
            }
            val deviceAddress = device?.address ?: ""

            when (intent.action) {
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    Log.d(TAG, "Bluetooth device connected: $deviceName")
                    executeTrigger(
                        TriggerType.BLUETOOTH_CONNECTED,
                        mapOf(
                            "connected" to "true",
                            "deviceName" to deviceName,
                            "deviceAddress" to deviceAddress
                        )
                    )
                }
                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    Log.d(TAG, "Bluetooth device disconnected: $deviceName")
                    executeTrigger(
                        TriggerType.BLUETOOTH_CONNECTED,
                        mapOf(
                            "connected" to "false",
                            "deviceName" to deviceName,
                            "deviceAddress" to deviceAddress
                        )
                    )
                }
            }
        }
    }

    /**
     * Headset broadcast receiver - handles wired headset plug/unplug
     */
    private val headsetReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_HEADSET_PLUG) {
                val state = intent.getIntExtra("state", -1)
                val connected = state == 1
                Log.d(TAG, "Headset ${if (connected) "plugged" else "unplugged"}")
                executeTrigger(
                    TriggerType.HEADPHONES_CONNECTED,
                    mapOf("connected" to connected.toString())
                )
            }
        }
    }

    // ==================== INITIALIZATION ====================

    /**
     * Initialize all trigger listeners
     * Called by TriggerMonitorService when it starts
     */
    fun initialize() {
        if (isInitialized) {
            Log.w(TAG, "TriggerManager already initialized")
            return
        }

        Log.d(TAG, "Initializing TriggerManager...")

        // Register broadcast receivers
        registerBroadcastReceivers()

        // Register network callback for WiFi
        registerNetworkCallback()

        // Start collecting AccessibilityService events
        collectAccessibilityEvents()

        isInitialized = true
        Log.d(TAG, "TriggerManager initialized successfully")
    }

    /**
     * Cleanup all listeners
     * Called by TriggerMonitorService when it stops
     */
    fun cleanup() {
        if (!isInitialized) {
            Log.w(TAG, "TriggerManager not initialized, skipping cleanup")
            return
        }

        Log.d(TAG, "Cleaning up TriggerManager...")

        // Unregister broadcast receivers
        unregisterBroadcastReceivers()

        // Unregister network callback
        unregisterNetworkCallback()

        // Cancel coroutine scope
        managerScope.cancel()

        isInitialized = false
        Log.d(TAG, "TriggerManager cleaned up")
    }

    // ==================== BROADCAST REGISTRATION ====================

    private fun registerBroadcastReceivers() {
        // Battery receiver
        val batteryFilter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        context.registerReceiver(batteryReceiver, batteryFilter)
        Log.d(TAG, "Battery receiver registered")


        // Bluetooth receiver
        val bluetoothFilter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        context.registerReceiver(bluetoothReceiver, bluetoothFilter)
        Log.d(TAG, "Bluetooth receiver registered")

        // Headset receiver
        val headsetFilter = IntentFilter().apply {
            addAction(Intent.ACTION_HEADSET_PLUG)
        }
        context.registerReceiver(headsetReceiver, headsetFilter)
        Log.d(TAG, "Headset receiver registered")
    }

    private fun unregisterBroadcastReceivers() {
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Battery receiver not registered")
        }


        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Bluetooth receiver not registered")
        }

        try {
            context.unregisterReceiver(headsetReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Headset receiver not registered")
        }

        Log.d(TAG, "All broadcast receivers unregistered")
    }

    // ==================== NETWORK CALLBACK ====================

    private fun registerNetworkCallback() {
        val networkRequest = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Log.d(TAG, "WiFi network available")
                if (lastWifiConnected != true) {
                    lastWifiConnected = true
                    executeTrigger(
                        TriggerType.WIFI_CONNECTED,
                        mapOf("connected" to "true")
                    )
                }
            }

            override fun onLost(network: Network) {
                Log.d(TAG, "WiFi network lost")
                if (lastWifiConnected != false) {
                    lastWifiConnected = false
                    executeTrigger(
                        TriggerType.WIFI_CONNECTED,
                        mapOf("connected" to "false")
                    )
                }
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val hasWifi = networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
                Log.d(TAG, "Network capabilities changed, WiFi: $hasWifi")
            }
        }

        networkCallback?.let {
            connectivityManager.registerNetworkCallback(networkRequest, it)
        }
        Log.d(TAG, "Network callback registered")
    }

    private fun unregisterNetworkCallback() {
        networkCallback?.let {
            try {
                connectivityManager.unregisterNetworkCallback(it)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "Network callback not registered")
            }
        }
        networkCallback = null
        Log.d(TAG, "Network callback unregistered")
    }

    // ==================== ACCESSIBILITY SERVICE EVENTS ====================

    /**
     * Collect app launch events from AccessibilityService
     */
    private fun collectAccessibilityEvents() {
        managerScope.launch {
            try {
                AutomationAccessibilityService.currentForegroundApp.collect { packageName ->
                    if (packageName != null && packageName != lastForegroundApp) {
                        lastForegroundApp = packageName
                        Log.d(TAG, "App launched: $packageName")
                        executeTrigger(
                            TriggerType.APP_OPENED,
                            mapOf("packageName" to packageName)
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error collecting accessibility events", e)
            }
        }
        Log.d(TAG, "Accessibility event collector started")
    }

    // ==================== NOTIFICATION LISTENER SERVICE EVENTS ====================


    // ==================== BATTERY HANDLING ====================

    private fun handleBatteryChanged(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val batteryPct = if (level != -1 && scale != -1) {
            (level * 100) / scale
        } else {
            -1
        }

        // Only trigger on significant battery level changes (every 5%)
        if (batteryPct > 0 && batteryPct != lastBatteryLevel) {
            val shouldTrigger = (batteryPct % 5 == 0) || // Every 5%
                    (batteryPct <= 20 && lastBatteryLevel > 20) || // Crossed 20% threshold
                    (batteryPct <= 10 && lastBatteryLevel > 10) || // Crossed 10% threshold
                    (batteryPct <= 5 && lastBatteryLevel > 5) // Crossed 5% threshold

            if (shouldTrigger || lastBatteryLevel == -1) {
                Log.d(TAG, "Battery level: $batteryPct%")
                lastBatteryLevel = batteryPct
                executeTrigger(
                    TriggerType.BATTERY_LEVEL,
                    mapOf("level" to batteryPct.toString())
                )
            }
        }

        // Handle charging status
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        if (lastChargingState != isCharging) {
            lastChargingState = isCharging
            Log.d(TAG, "Charging status: $isCharging")
            executeTrigger(
                TriggerType.CHARGING_STATUS,
                mapOf("charging" to isCharging.toString())
            )
        }
    }

    private fun handlePowerConnected() {
        if (lastChargingState != true) {
            lastChargingState = true
            Log.d(TAG, "Power connected")
            executeTrigger(
                TriggerType.CHARGING_STATUS,
                mapOf("charging" to "true")
            )
        }
    }

    private fun handlePowerDisconnected() {
        if (lastChargingState != false) {
            lastChargingState = false
            Log.d(TAG, "Power disconnected")
            executeTrigger(
                TriggerType.CHARGING_STATUS,
                mapOf("charging" to "false")
            )
        }
    }

    // ==================== RULE EXECUTION ====================

    /**
     * Execute all rules matching the given trigger type IMMEDIATELY
     * This bypasses WorkManager for instant response
     */
    private fun executeTrigger(triggerType: TriggerType, metadata: Map<String, String> = emptyMap()) {
        managerScope.launch {
            try {
                Log.d(TAG, "Executing rules for trigger: $triggerType, metadata: $metadata")

                // Get all rules with this trigger type
                val rules = repository.getRulesByTriggerType(triggerType).first()
                Log.d(TAG, "Found ${rules.size} rules with trigger type $triggerType")

                var executedCount = 0
                rules.forEach { rule ->
                    if (rule.isEnabled) {
                        Log.d(TAG, "Executing rule: ${rule.name} (id=${rule.id})")

                        val result = executeRuleUseCase(
                            ruleId = rule.id,
                            triggeredBy = "TriggerManager:${triggerType.name}"
                        )

                        result.onSuccess { executionResult ->
                            if (executionResult.executed) {
                                executedCount++
                                Log.d(TAG, "Rule '${rule.name}' executed: ${executionResult.actionsExecuted} actions")
                            } else {
                                Log.d(TAG, "Rule '${rule.name}' skipped: ${executionResult.reason}")
                            }
                        }

                        result.onFailure { error ->
                            Log.e(TAG, "Rule '${rule.name}' failed: ${error.message}")
                        }
                    } else {
                        Log.d(TAG, "Rule '${rule.name}' is disabled, skipping")
                    }
                }

                Log.d(TAG, "Trigger $triggerType complete: $executedCount rules executed")
            } catch (e: Exception) {
                Log.e(TAG, "Error executing trigger $triggerType", e)
            }
        }
    }

    // ==================== STATUS METHODS ====================

    /**
     * Check if TriggerManager is currently initialized
     */
    fun isActive(): Boolean = isInitialized

    /**
     * Get current trigger states for debugging
     */
    fun getDebugInfo(): Map<String, Any?> = mapOf(
        "isInitialized" to isInitialized,
        "lastBatteryLevel" to lastBatteryLevel,
        "lastChargingState" to lastChargingState,
        "lastWifiConnected" to lastWifiConnected,
        "lastForegroundApp" to lastForegroundApp,
        "accessibilityServiceConnected" to AutomationAccessibilityService.isServiceEnabled(),
        "notificationListenerConnected" to AutomationNotificationListenerService.isServiceEnabled()
    )

    // ==================== RULE UPDATE HANDLING ====================

    /**
     * Called when a specific rule has been updated.
     * This method can be used to perform any necessary cleanup or state updates
     * when a rule's triggers/actions change.
     *
     * @param ruleId The ID of the updated rule
     */
    fun onRuleUpdated(ruleId: Long) {
        Log.d(TAG, "Rule $ruleId updated, refreshing internal state if needed")
        // Currently, TriggerManager doesn't cache rule data - it queries the repository
        // on each trigger event. So no special handling is needed here.
        // This method is provided as a hook for future enhancements where we might
        // cache rule data for performance.
    }

    /**
     * Reload all rules from the repository.
     * Call this when you need to ensure TriggerManager has the latest data.
     *
     * Currently, TriggerManager queries the repository on each trigger event,
     * so this is a no-op. But it's provided as a hook for future caching implementation.
     */
    fun reloadRules() {
        Log.d(TAG, "Reloading rules (no-op: TriggerManager queries repository on each event)")
        // TriggerManager doesn't cache rules - it queries repository.getRulesByTriggerType()
        // on each trigger event. If we add caching in the future, this method would
        // invalidate/reload that cache.
    }
}

