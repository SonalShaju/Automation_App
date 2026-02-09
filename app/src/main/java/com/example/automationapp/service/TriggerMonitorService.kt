package com.example.automationapp.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.automationapp.MainActivity
import com.example.automationapp.R
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.domain.usecase.ExecuteRuleUseCase
import com.example.automationapp.domain.usecase.UpdateRuleUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Foreground Service that keeps the app alive for trigger monitoring.
 *
 * Android 16 Compliant Architecture:
 * - Delegates all trigger listening to TriggerManager
 * - TriggerManager handles 4 listener types:
 *   1. Broadcasts (Battery, Screen, Bluetooth, Headset)
 *   2. NetworkCallback (WiFi state)
 *   3. AccessibilityService events (App launches)
 *   4. NotificationListenerService events (Notifications)
 *
 * All triggers execute rules IMMEDIATELY via ExecuteRuleUseCase (bypasses WorkManager).
 *
 * Listens for ACTION_RULES_UPDATED broadcasts to refresh rules when they are modified.
 */
@AndroidEntryPoint
class TriggerMonitorService : Service() {

    @Inject
    lateinit var triggerManager: TriggerManager

    @Inject
    lateinit var ruleSchedulingManager: RuleSchedulingManager

    @Inject
    lateinit var repository: AutomationRepository

    @Inject
    lateinit var executeRuleUseCase: ExecuteRuleUseCase

    @Inject
    lateinit var actionExecutor: com.example.automationapp.domain.executor.ActionExecutor

    @Inject
    lateinit var json: Json

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var wakeLock: PowerManager.WakeLock? = null
    private var timeTickerJob: Job? = null

    // Idempotency: Track fired rules with "RuleID_Hour_Minute" format
    private val firedRules = mutableSetOf<String>()
    private var lastClearedHour: Int = -1

    // Heartbeat tracking for notification update
    private var lastHeartbeatTime: String = "Starting..."

    /**
     * Broadcast receiver to listen for rule update notifications
     */
    private val ruleUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                UpdateRuleUseCase.ACTION_RULES_UPDATED -> {
                    val ruleId = intent.getLongExtra(UpdateRuleUseCase.EXTRA_RULE_ID, -1)
                    Log.d(TAG, "Received rule update broadcast for ruleId: $ruleId")
                    refreshRules(ruleId)
                }
                ACTION_REFRESH_RULES -> {
                    Log.d(TAG, "Received manual refresh rules broadcast")
                    refreshAllRules()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TriggerMonitorService created")
        createNotificationChannel()
        registerRuleUpdateReceiver()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TriggerMonitorService onStartCommand: ${intent?.action}")

        try {
            when (intent?.action) {
                ACTION_START -> {
                    startForegroundService()
                    initializeTriggerManager()
                    initializeScheduling()
                    startTimeTicker()
                }
                ACTION_STOP -> {
                    stopForegroundService()
                    return START_NOT_STICKY
                }
                else -> {
                    // Handle null action (service restart by system)
                    Log.d(TAG, "Service restarted by system, reinitializing...")
                    startForegroundService()
                    initializeTriggerManager()
                    initializeScheduling()
                    startTimeTicker()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            // Still try to start foreground to prevent crash
            try {
                startForegroundService()
            } catch (fe: Exception) {
                Log.e(TAG, "Failed to start foreground service", fe)
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        Log.d(TAG, "TriggerMonitorService destroyed")
        unregisterRuleUpdateReceiver()
        cleanupTriggerManager()
        stopTimeTicker()
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    // ==================== BROADCAST RECEIVER ====================

    private fun registerRuleUpdateReceiver() {
        try {
            val filter = IntentFilter().apply {
                addAction(UpdateRuleUseCase.ACTION_RULES_UPDATED)
                addAction(ACTION_REFRESH_RULES)
            }
            // Use ContextCompat for backwards compatibility
            ContextCompat.registerReceiver(
                this,
                ruleUpdateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            Log.d(TAG, "Rule update receiver registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register rule update receiver", e)
        }
    }

    private fun unregisterRuleUpdateReceiver() {
        try {
            unregisterReceiver(ruleUpdateReceiver)
            Log.d(TAG, "Rule update receiver unregistered")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering rule update receiver", e)
        }
    }

    // ==================== RULE REFRESH ====================

    /**
     * Refresh a specific rule's triggers after an update
     */
    private fun refreshRules(ruleId: Long) {
        serviceScope.launch {
            try {
                Log.d(TAG, "Refreshing triggers for rule: $ruleId")
                // The UpdateRuleUseCase already handles cancelling and rescheduling,
                // but TriggerManager might need to reload its internal state
                triggerManager.onRuleUpdated(ruleId)
                Log.d(TAG, "Rule refresh completed for rule: $ruleId")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing rule $ruleId", e)
            }
        }
    }

    /**
     * Refresh all rules - useful for full resync
     */
    private fun refreshAllRules() {
        serviceScope.launch {
            try {
                Log.d(TAG, "Refreshing all rules")
                ruleSchedulingManager.rescheduleAllEnabledRules()
                triggerManager.reloadRules()
                Log.d(TAG, "All rules refresh completed")
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing all rules", e)
            }
        }
    }

    // ==================== SERVICE LIFECYCLE ====================

    private fun startForegroundService() {
        acquireWakeLock()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        Log.d(TAG, "Foreground service started")
    }

    private fun stopForegroundService() {
        cleanupTriggerManager()
        releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d(TAG, "Foreground service stopped")
    }

    // ==================== TRIGGER MANAGER ====================

    private fun initializeTriggerManager() {
        try {
            triggerManager.initialize()
            Log.d(TAG, "TriggerManager initialized")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize TriggerManager", e)
        }
    }

    private fun cleanupTriggerManager() {
        try {
            triggerManager.cleanup()
            Log.d(TAG, "TriggerManager cleaned up")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up TriggerManager", e)
        }
    }

    // ==================== SCHEDULING ====================

    private fun initializeScheduling() {
        serviceScope.launch {
            try {
                ruleSchedulingManager.initializeScheduling()
                Log.d(TAG, "Rule scheduling initialized")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize scheduling", e)
            }
        }
    }

    // ==================== TIME TICKER (10s Loop - Aggressive Reliability) ====================

    /**
     * Start the 10-second ticker for precise time-based trigger detection.
     * Uses aggressive 10s interval for demo reliability (battery usage is secondary).
     * This bypasses WorkManager's 15+ minute minimum for instant trigger response.
     */
    private fun startTimeTicker() {
        if (timeTickerJob?.isActive == true) {
            Log.w(TAG, "Time ticker already running")
            return
        }

        timeTickerJob = serviceScope.launch {
            Log.d(TAG, "Time ticker started with 10s interval")
            while (isActive) {
                try {
                    checkTimeRangeTriggers()
                    updateHeartbeatNotification()
                } catch (e: Exception) {
                    Log.e(TAG, "Error in time ticker loop", e)
                }
                delay(10_000L) // 10 seconds - aggressive for demo reliability
            }
        }
    }

    /**
     * Stop the time ticker
     */
    private fun stopTimeTicker() {
        timeTickerJob?.cancel()
        timeTickerJob = null
        Log.d(TAG, "Time ticker stopped")
    }

    /**
     * Check all active TIME_RANGE triggers and execute rules at start/end times.
     * Handles midnight crossover case (e.g., 10 PM to 7 AM).
     * Uses HashSet-based idempotency to prevent duplicate execution.
     */
    private suspend fun checkTimeRangeTriggers() {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        val currentTimeMinutes = currentHour * 60 + currentMinute
        val currentTimeStr = String.format("%02d:%02d", currentHour, currentMinute)

        // Clear firedRules set when hour changes to prevent memory bloat
        if (currentHour != lastClearedHour) {
            firedRules.clear()
            lastClearedHour = currentHour
            Log.d(TAG, "Cleared firedRules set at hour $currentHour")
        }

        try {
            // Get all rules with TIME_RANGE triggers
            val timeRangeRules = repository.getRulesByTriggerType(TriggerType.TIME_RANGE).first()
            val activeRulesCount = timeRangeRules.count { it.isEnabled }

            Log.e("TriggerDebug", "Checking Rules at $currentTimeStr - Found $activeRulesCount active TIME_RANGE rules")

            for (rule in timeRangeRules) {
                if (!rule.isEnabled) continue

                // Get triggers for this rule
                val triggers = repository.getTriggersForRule(rule.id).first()
                val timeRangeTrigger = triggers.find { it.type == TriggerType.TIME_RANGE && it.isActive }
                    ?: continue

                try {
                    val params = json.parseToJsonElement(timeRangeTrigger.parameters).jsonObject
                    val startTimeStr = params["start_time"]?.jsonPrimitive?.content ?: continue
                    val endTimeStr = params["end_time"]?.jsonPrimitive?.content ?: continue

                    val startParts = startTimeStr.split(":")
                    val endParts = endTimeStr.split(":")
                    if (startParts.size != 2 || endParts.size != 2) continue

                    val startHour = startParts[0].toIntOrNull() ?: continue
                    val startMin = startParts[1].toIntOrNull() ?: continue
                    val endHour = endParts[0].toIntOrNull() ?: continue
                    val endMin = endParts[1].toIntOrNull() ?: continue

                    val startTimeMinutes = startHour * 60 + startMin
                    val endTimeMinutes = endHour * 60 + endMin

                    // Idempotency keys: "RuleID_START_Hour_Minute" and "RuleID_END_Hour_Minute"
                    val startKey = "${rule.id}_START_${currentHour}_${currentMinute}"
                    val endKey = "${rule.id}_END_${currentHour}_${currentMinute}"

                    // Check if current time matches START time
                    if (currentTimeMinutes == startTimeMinutes && !firedRules.contains(startKey)) {
                        firedRules.add(startKey)
                        Log.e("TriggerDebug", ">>> FIRING START for rule '${rule.name}' (ID: ${rule.id}) at $currentTimeStr")
                        executeRuleWithExitAction(rule.id, isExitAction = false)
                    }

                    // Check if current time matches END time
                    if (currentTimeMinutes == endTimeMinutes && !firedRules.contains(endKey)) {
                        firedRules.add(endKey)
                        Log.e("TriggerDebug", ">>> FIRING END for rule '${rule.name}' (ID: ${rule.id}) at $currentTimeStr")
                        executeRuleWithExitAction(rule.id, isExitAction = true)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing time range for rule ${rule.id}", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking time range triggers", e)
        }
    }

    /**
     * Execute a rule, optionally using the exit action if defined.
     *
     * @param ruleId The ID of the rule to execute
     * @param isExitAction If true, execute the exit action (if defined) instead of the main action
     */
    private suspend fun executeRuleWithExitAction(ruleId: Long, isExitAction: Boolean) {
        try {
            if (isExitAction) {
                // For exit actions, check if the rule has an exit action defined in the entity
                val rule = repository.getRuleById(ruleId)
                if (rule != null && rule.exitActionType != null) {
                    Log.d(TAG, "Executing exit action (${rule.exitActionType}) for rule $ruleId")

                    // Create a temporary action for execution
                    val exitAction = com.example.automationapp.data.local.entity.Action(
                        id = 0,
                        ruleId = ruleId,
                        type = rule.exitActionType,
                        parameters = rule.exitActionParams ?: "{}",
                        sequence = 0,
                        isEnabled = true
                    )

                    // Execute the exit action using the injected ActionExecutor
                    try {
                        actionExecutor.executeAction(exitAction)
                        Log.d(TAG, "Exit action executed successfully for rule $ruleId")
                    } catch (e: Exception) {
                        Log.e(TAG, "Exit action failed for rule $ruleId: ${e.message}")
                    }
                } else {
                    Log.d(TAG, "No exit action defined for rule $ruleId")
                }
            } else {
                // Execute main action
                val result = executeRuleUseCase(
                    ruleId = ruleId,
                    triggeredBy = "TimeTicker:TIME_RANGE"
                )
                result.onSuccess { executionResult ->
                    if (executionResult.executed) {
                        Log.d(TAG, "Rule $ruleId executed: ${executionResult.actionsExecuted} actions")
                    }
                }
                result.onFailure { error ->
                    Log.e(TAG, "Rule $ruleId failed: ${error.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing rule $ruleId", e)
        }
    }

    // ==================== HEARTBEAT NOTIFICATION ====================

    /**
     * Update the foreground notification with the current timestamp.
     * This provides visual debugging - user can see if service is alive.
     */
    private fun updateHeartbeatNotification() {
        try {
            val dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            lastHeartbeatTime = dateFormat.format(Date())

            val notificationManager = getSystemService(NotificationManager::class.java)
            val notification = createNotificationWithHeartbeat(lastHeartbeatTime)
            notificationManager.notify(NOTIFICATION_ID, notification)

            Log.d(TAG, "Heartbeat notification updated: $lastHeartbeatTime")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update heartbeat notification", e)
        }
    }

    // ==================== NOTIFICATION ====================

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Trigger Monitor Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Keeps automation triggers active in the background"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return createNotificationWithHeartbeat("Starting...")
    }

    private fun createNotificationWithHeartbeat(heartbeatTime: String): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, TriggerMonitorService::class.java).apply {
                action = ACTION_STOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Automation Service Running")
            .setContentText("Last check: $heartbeatTime")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setOnlyAlertOnce(true)  // Don't vibrate/sound on every update
            .build()
    }

    // ==================== WAKELOCK ====================

    private fun acquireWakeLock() {
        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$TAG::WakeLock"
        ).apply {
            acquire(30 * 60 * 1000L) // 30 minutes timeout
        }
        Log.d(TAG, "WakeLock acquired")
    }

    private fun releaseWakeLock() {
        wakeLock?.let {
            if (it.isHeld) {
                it.release()
                Log.d(TAG, "WakeLock released")
            }
        }
        wakeLock = null
    }

    // ==================== COMPANION OBJECT ====================

    companion object {
        private const val TAG = "TriggerMonitorService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "trigger_monitor_channel"

        const val ACTION_START = "com.example.automationapp.action.START_TRIGGER_MONITOR"
        const val ACTION_STOP = "com.example.automationapp.action.STOP_TRIGGER_MONITOR"
        const val ACTION_REFRESH_RULES = "com.example.automationapp.action.REFRESH_RULES"

        /**
         * Start the trigger monitor service
         */
        fun start(context: Context) {
            val intent = Intent(context, TriggerMonitorService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * Stop the trigger monitor service
         */
        fun stop(context: Context) {
            val intent = Intent(context, TriggerMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        /**
         * Request the service to refresh all rules
         * Call this after making changes to rules that need immediate effect
         */
        fun refreshRules(context: Context) {
            val intent = Intent(ACTION_REFRESH_RULES).apply {
                setPackage(context.packageName)
            }
            context.sendBroadcast(intent)
        }
    }
}

