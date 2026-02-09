package com.example.automationapp.domain.executor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.bluetooth.BluetoothAdapter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.automationapp.data.local.entity.Condition
import com.example.automationapp.data.local.entity.ConditionType
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.data.local.entity.TriggerType
import com.example.automationapp.util.PermissionHelper
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Tasks
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Evaluates trigger conditions and state conditions.
 *
 * Triggers are events that START the evaluation (e.g., "when WiFi connects").
 * Conditions are state checks that must ALL be true (AND logic) for the rule to execute.
 */
@Singleton
class TriggerEvaluator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val permissionHelper: PermissionHelper
) {
    private val TAG = "TriggerEvaluator"

    /**
     * Evaluates whether a rule should execute based on a triggering event and conditions.
     *
     * @param eventTriggerType The type of event that triggered this evaluation
     * @param triggers The list of triggers configured for the rule (events that can start the rule)
     * @param conditions The list of conditions that must ALL be satisfied (AND logic)
     * @return true if the trigger matches AND all conditions are met, false otherwise
     */
    suspend fun evaluate(
        eventTriggerType: TriggerType,
        triggers: List<Trigger>,
        conditions: List<Condition>
    ): Boolean {
        Log.d(TAG, "Evaluating rule: eventType=$eventTriggerType, triggers=${triggers.size}, conditions=${conditions.size}")

        // Step 1: Check if the event trigger type matches any of the rule's triggers
        val matchingTrigger = triggers.find { it.type == eventTriggerType && it.isActive }
        if (matchingTrigger == null) {
            Log.d(TAG, "No matching active trigger found for event type: $eventTriggerType")
            return false
        }
        Log.d(TAG, "Found matching trigger: ${matchingTrigger.type}")

        // Step 2: Evaluate the matching trigger's specific parameters (e.g., specific WiFi SSID)
        val triggerMatches = evaluate(matchingTrigger)
        if (!triggerMatches) {
            Log.d(TAG, "Trigger parameters did not match for: ${matchingTrigger.type}")
            return false
        }

        // Step 3: Evaluate ALL conditions (AND logic) - all must pass
        val activeConditions = conditions.filter { it.isActive }
        if (activeConditions.isEmpty()) {
            Log.d(TAG, "No active conditions to evaluate, trigger matched - returning true")
            return true
        }

        for (condition in activeConditions) {
            val conditionMet = evaluateCondition(condition)
            Log.d(TAG, "Condition ${condition.type} evaluated: $conditionMet")
            if (!conditionMet) {
                Log.d(TAG, "Condition ${condition.type} not met - returning false")
                return false
            }
        }

        Log.d(TAG, "All conditions met - returning true")
        return true
    }

    /**
     * Evaluates a single condition (state check).
     *
     * @param condition The condition to evaluate
     * @return true if the condition is satisfied, false otherwise
     */
    suspend fun evaluateCondition(condition: Condition): Boolean {
        Log.d(TAG, "Evaluating condition: ${condition.type}, params: ${condition.parameters}")

        return when (condition.type) {
            ConditionType.TIME_RANGE -> evaluateTimeRangeCondition(condition)
            ConditionType.LOCATION_BASED -> evaluateLocationCondition(condition)
            ConditionType.BATTERY_LEVEL -> evaluateBatteryCondition(condition)
            ConditionType.CHARGING_STATUS -> evaluateChargingCondition(condition)
            ConditionType.WIFI_CONNECTED -> evaluateWifiCondition(condition)
            ConditionType.BLUETOOTH_CONNECTED -> evaluateBluetoothCondition(condition)
            ConditionType.HEADPHONES_CONNECTED -> evaluateHeadphonesCondition(condition)
            ConditionType.SCREEN_STATE -> evaluateScreenStateCondition(condition)
            ConditionType.NETWORK_TYPE -> evaluateNetworkTypeCondition(condition)
            ConditionType.AIRPLANE_MODE -> evaluateAirplaneModeCondition(condition)
            ConditionType.DO_NOT_DISTURB -> evaluateDoNotDisturbCondition(condition)
        }
    }

    /**
     * Legacy method: Evaluates a single trigger.
     * Still used for evaluating trigger-specific parameters.
     */
    suspend fun evaluate(trigger: Trigger): Boolean {
        Log.d(TAG, "Evaluating trigger: ${trigger.type}, params: ${trigger.parameters}")

        // Check if required permissions are granted for this trigger type
        val permissionCheck = permissionHelper.checkPermissionsForTrigger(trigger.type)
        if (!permissionCheck.hasAllPermissions) {
            val missingPerms = permissionCheck.allMissing.joinToString(", ") { it.displayName }
            Log.w(TAG, "Missing permissions for trigger ${trigger.type}: $missingPerms")
            return false
        }

        val result = when (trigger.type) {
            TriggerType.TIME_BASED -> evaluateTimeTrigger(trigger)
            TriggerType.TIME_RANGE -> evaluateTimeRangeTrigger(trigger)
            TriggerType.LOCATION_BASED -> evaluateLocationTrigger(trigger)
            TriggerType.BATTERY_LEVEL -> evaluateBatteryTrigger(trigger)
            TriggerType.CHARGING_STATUS -> evaluateChargingTrigger(trigger)
            TriggerType.WIFI_CONNECTED -> evaluateWifiTrigger(trigger)
            TriggerType.BLUETOOTH_CONNECTED -> evaluateBluetoothTrigger(trigger)
            TriggerType.HEADPHONES_CONNECTED -> evaluateHeadphonesTrigger(trigger)
            TriggerType.APP_OPENED -> {
                // App opened triggers are handled by AccessibilityService events
                // and evaluated contextually, not by polling current state
                Log.d(TAG, "APP_OPENED trigger - event-driven, returning true")
                true
            }
            TriggerType.AIRPLANE_MODE -> evaluateAirplaneMode(trigger)
            TriggerType.DO_NOT_DISTURB -> evaluateDoNotDisturb(trigger)
        }
        Log.d(TAG, "Trigger ${trigger.type} evaluation result: $result")
        return result
    }

    private fun evaluateTimeTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val targetTime = params["time"]?.jsonPrimitive?.content ?: return false
            val days = params["days"]?.jsonPrimitive?.content?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentDay = getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))

            val timeParts = targetTime.split(":")
            if (timeParts.size != 2) return false

            val targetHour = timeParts[0].toIntOrNull() ?: return false
            val targetMinute = timeParts[1].toIntOrNull() ?: return false

            val timeMatches = currentHour == targetHour &&
                    currentMinute >= targetMinute &&
                    currentMinute < targetMinute + 5 // 5-minute window

            val dayMatches = days.isEmpty() || days.contains(currentDay)

            Log.d(TAG, "Time trigger: current=$currentHour:$currentMinute ($currentDay), target=$targetHour:$targetMinute ($days), matches=${timeMatches && dayMatches}")
            timeMatches && dayMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating time trigger", e)
            false
        }
    }

    /**
     * Evaluates if current time is within a time range.
     * Handles midnight crossover case (e.g., 10 PM to 7 AM).
     *
     * Parameters expected:
     * - start_time: "HH:mm" format
     * - end_time: "HH:mm" format
     */
    private fun evaluateTimeRangeTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val startTimeStr = params["start_time"]?.jsonPrimitive?.content ?: return false
            val endTimeStr = params["end_time"]?.jsonPrimitive?.content ?: return false

            val startParts = startTimeStr.split(":")
            val endParts = endTimeStr.split(":")
            if (startParts.size != 2 || endParts.size != 2) return false

            val startHour = startParts[0].toIntOrNull() ?: return false
            val startMinute = startParts[1].toIntOrNull() ?: return false
            val endHour = endParts[0].toIntOrNull() ?: return false
            val endMinute = endParts[1].toIntOrNull() ?: return false

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            // Convert times to minutes since midnight for easier comparison
            val currentTimeMinutes = currentHour * 60 + currentMinute
            val startTimeMinutes = startHour * 60 + startMinute
            val endTimeMinutes = endHour * 60 + endMinute

            val isInRange = if (startTimeMinutes <= endTimeMinutes) {
                // Normal case: e.g., 9:00 to 17:00
                currentTimeMinutes in startTimeMinutes..endTimeMinutes
            } else {
                // Midnight crossover case: e.g., 22:00 to 07:00
                // Time is in range if it's >= start OR <= end
                currentTimeMinutes >= startTimeMinutes || currentTimeMinutes <= endTimeMinutes
            }

            Log.d(TAG, "Time range trigger: current=$currentHour:$currentMinute, range=$startTimeStr-$endTimeStr, isInRange=$isInRange")
            isInRange
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating time range trigger", e)
            false
        }
    }

    private fun evaluateLocationTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val targetLat = params["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return false
            val targetLng = params["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return false
            val radius = params["radius"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 100f

            // Check if we have location permission
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission not granted")
                return false
            }

            // Check if location services are enabled
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.w(TAG, "Location services disabled")
                return false
            }

            // Get current location using FusedLocationProviderClient
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            try {
                // Try to get last known location first (faster)
                val lastLocation = Tasks.await(fusedLocationClient.lastLocation, 5, TimeUnit.SECONDS)

                if (lastLocation != null) {
                    val distance = calculateDistance(
                        lastLocation.latitude, lastLocation.longitude,
                        targetLat, targetLng
                    )
                    Log.d(TAG, "Location trigger: distance=$distance meters, radius=$radius, isInside=${distance <= radius}")
                    return distance <= radius
                }

                // If no last location, try to get current location
                val currentLocationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                )
                val currentLocation = Tasks.await(currentLocationTask, 10, TimeUnit.SECONDS)

                if (currentLocation != null) {
                    val distance = calculateDistance(
                        currentLocation.latitude, currentLocation.longitude,
                        targetLat, targetLng
                    )
                    Log.d(TAG, "Location trigger (current): distance=$distance meters, radius=$radius, isInside=${distance <= radius}")
                    return distance <= radius
                }

                Log.w(TAG, "Could not get device location")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating location trigger", e)
            false
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    private fun evaluateBatteryTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val targetLevel = params["level"]?.jsonPrimitive?.content?.toInt() ?: return false
            val operator = params["operator"]?.jsonPrimitive?.content ?: "equals"

            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return false
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()

            when (operator) {
                "less_than" -> batteryPct < targetLevel
                "greater_than" -> batteryPct > targetLevel
                "equals" -> batteryPct == targetLevel
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun evaluateChargingTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val shouldBeCharging = params["charging"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            isCharging == shouldBeCharging
        } catch (e: Exception) {
            false
        }
    }

    private fun evaluateWifiTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val targetSsid = params["ssid"]?.jsonPrimitive?.content

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            // Check if WiFi is enabled first
            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "WiFi is disabled")
                return false
            }

            val connectionInfo = wifiManager.connectionInfo

            if (targetSsid == null || targetSsid.isBlank()) {
                // Just check if WiFi is connected
                val isConnected = connectionInfo.networkId != -1
                Log.d(TAG, "WiFi connection check: connected=$isConnected")
                return isConnected
            }

            // For SSID matching, check location permission on Android 8+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Location permission not granted - cannot read WiFi SSID on Android 8+")
                    return false
                }
            }

            val currentSsid = connectionInfo.ssid.replace("\"", "")
            val matches = currentSsid == targetSsid
            Log.d(TAG, "WiFi SSID check: current=$currentSsid, target=$targetSsid, matches=$matches")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating WiFi trigger", e)
            false
        }
    }

    private fun evaluateBluetoothTrigger(trigger: Trigger): Boolean {
        return try {
            // Check Bluetooth permission on Android 12+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted on Android 12+")
                    return false
                }
            }

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val isEnabled = bluetoothAdapter?.isEnabled ?: false
            Log.d(TAG, "Bluetooth state check: enabled=$isEnabled")
            isEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking Bluetooth state - missing permissions", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating Bluetooth trigger", e)
            false
        }
    }

    private fun evaluateHeadphonesTrigger(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val shouldBeConnected = params["connected"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val isConnected = audioManager.isWiredHeadsetOn ||
                              audioManager.isBluetoothA2dpOn ||
                              audioManager.isBluetoothScoOn

            Log.d(TAG, "Headphones trigger: connected=$isConnected, expected=$shouldBeConnected")
            isConnected == shouldBeConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating headphones trigger", e)
            false
        }
    }

    private fun evaluateAirplaneMode(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            val shouldBeEnabled = params["enabled"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val isAirplaneModeOn = android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0

            Log.d(TAG, "Airplane mode trigger: isOn=$isAirplaneModeOn, expected=$shouldBeEnabled")
            isAirplaneModeOn == shouldBeEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating airplane mode trigger", e)
            false
        }
    }

    private fun evaluateDoNotDisturb(trigger: Trigger): Boolean {
        return try {
            val params = json.parseToJsonElement(trigger.parameters).jsonObject
            // Support both legacy "dnd_on" and new "target_state" parameter
            val targetState = params["target_state"]?.jsonPrimitive?.content?.uppercase()
                ?: if (params["dnd_on"]?.jsonPrimitive?.content?.toBoolean() == true) "ON" else "OFF"

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val currentFilter = notificationManager.currentInterruptionFilter

            // INTERRUPTION_FILTER_ALL means DND is OFF
            // INTERRUPTION_FILTER_PRIORITY, ALARMS, or NONE means DND is ON
            val isDndOn = when (currentFilter) {
                android.app.NotificationManager.INTERRUPTION_FILTER_ALL -> false
                android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                android.app.NotificationManager.INTERRUPTION_FILTER_ALARMS,
                android.app.NotificationManager.INTERRUPTION_FILTER_NONE -> true
                else -> false
            }

            val matches = when (targetState) {
                "ON" -> isDndOn
                "OFF" -> !isDndOn
                else -> isDndOn // Default to checking if ON
            }

            Log.d(TAG, "DND trigger: currentFilter=$currentFilter, isDndOn=$isDndOn, targetState=$targetState, matches=$matches")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating DND trigger", e)
            false
        }
    }

    private fun getDayOfWeek(calendarDay: Int): String {
        return when (calendarDay) {
            Calendar.SUNDAY -> "SUN"
            Calendar.MONDAY -> "MON"
            Calendar.TUESDAY -> "TUE"
            Calendar.WEDNESDAY -> "WED"
            Calendar.THURSDAY -> "THU"
            Calendar.FRIDAY -> "FRI"
            Calendar.SATURDAY -> "SAT"
            else -> ""
        }
    }

    // ==================== Condition Evaluation Methods ====================

    private fun evaluateTimeRangeCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val startTime = params["start_time"]?.jsonPrimitive?.content ?: return false
            val endTime = params["end_time"]?.jsonPrimitive?.content ?: return false
            val days = params["days"]?.jsonPrimitive?.content?.split(",")?.filter { it.isNotBlank() } ?: emptyList()

            val calendar = Calendar.getInstance()
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)
            val currentDay = getDayOfWeek(calendar.get(Calendar.DAY_OF_WEEK))

            val startParts = startTime.split(":")
            val endParts = endTime.split(":")
            if (startParts.size != 2 || endParts.size != 2) return false

            val startHour = startParts[0].toIntOrNull() ?: return false
            val startMinuteVal = startParts[1].toIntOrNull() ?: return false
            val endHour = endParts[0].toIntOrNull() ?: return false
            val endMinuteVal = endParts[1].toIntOrNull() ?: return false

            val currentMinutes = currentHour * 60 + currentMinute
            val startMinutes = startHour * 60 + startMinuteVal
            val endMinutes = endHour * 60 + endMinuteVal

            val timeInRange = if (startMinutes <= endMinutes) {
                currentMinutes in startMinutes..endMinutes
            } else {
                // Handle overnight range (e.g., 22:00 - 06:00)
                currentMinutes >= startMinutes || currentMinutes <= endMinutes
            }

            val dayMatches = days.isEmpty() || days.contains(currentDay)

            Log.d(TAG, "Time range condition: current=$currentHour:$currentMinute ($currentDay), range=$startTime-$endTime ($days), inRange=${timeInRange && dayMatches}")
            timeInRange && dayMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating time range condition", e)
            false
        }
    }

    private fun evaluateLocationCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val targetLat = params["latitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return false
            val targetLng = params["longitude"]?.jsonPrimitive?.content?.toDoubleOrNull() ?: return false
            val radius = params["radius"]?.jsonPrimitive?.content?.toFloatOrNull() ?: 100f

            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location permission not granted for condition")
                return false
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                !locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                Log.w(TAG, "Location services disabled for condition")
                return false
            }

            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

            try {
                val lastLocation = Tasks.await(fusedLocationClient.lastLocation, 5, TimeUnit.SECONDS)
                if (lastLocation != null) {
                    val distance = calculateDistance(
                        lastLocation.latitude, lastLocation.longitude,
                        targetLat, targetLng
                    )
                    Log.d(TAG, "Location condition: distance=$distance meters, radius=$radius, isInside=${distance <= radius}")
                    return distance <= radius
                }

                val currentLocationTask = fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null
                )
                val currentLocation = Tasks.await(currentLocationTask, 10, TimeUnit.SECONDS)

                if (currentLocation != null) {
                    val distance = calculateDistance(
                        currentLocation.latitude, currentLocation.longitude,
                        targetLat, targetLng
                    )
                    return distance <= radius
                }

                Log.w(TAG, "Could not get device location for condition")
                false
            } catch (e: Exception) {
                Log.e(TAG, "Error getting location for condition", e)
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating location condition", e)
            false
        }
    }

    private fun evaluateBatteryCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val targetLevel = params["level"]?.jsonPrimitive?.content?.toInt() ?: return false
            val operator = params["operator"]?.jsonPrimitive?.content ?: "equals"

            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: return false
            val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val batteryPct = (level.toFloat() / scale.toFloat() * 100).toInt()

            val result = when (operator) {
                "less_than" -> batteryPct < targetLevel
                "greater_than" -> batteryPct > targetLevel
                "equals" -> batteryPct == targetLevel
                else -> false
            }
            Log.d(TAG, "Battery condition: current=$batteryPct%, target=$targetLevel, operator=$operator, result=$result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating battery condition", e)
            false
        }
    }

    private fun evaluateChargingCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val shouldBeCharging = params["charging"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val batteryStatus = context.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )

            val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: return false
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL

            Log.d(TAG, "Charging condition: isCharging=$isCharging, expected=$shouldBeCharging")
            isCharging == shouldBeCharging
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating charging condition", e)
            false
        }
    }

    private fun evaluateWifiCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val targetSsid = params["ssid"]?.jsonPrimitive?.content

            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

            if (!wifiManager.isWifiEnabled) {
                Log.d(TAG, "WiFi is disabled for condition")
                return false
            }

            val connectionInfo = wifiManager.connectionInfo

            if (targetSsid == null || targetSsid.isBlank()) {
                val isConnected = connectionInfo.networkId != -1
                Log.d(TAG, "WiFi condition (any network): connected=$isConnected")
                return isConnected
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "Location permission not granted for WiFi SSID check")
                    return false
                }
            }

            val currentSsid = connectionInfo.ssid.replace("\"", "")
            val matches = currentSsid == targetSsid
            Log.d(TAG, "WiFi condition: current=$currentSsid, target=$targetSsid, matches=$matches")
            matches
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating WiFi condition", e)
            false
        }
    }

    private fun evaluateBluetoothCondition(condition: Condition): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) {
                    Log.w(TAG, "BLUETOOTH_CONNECT permission not granted")
                    return false
                }
            }

            val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            val isEnabled = bluetoothAdapter?.isEnabled ?: false
            Log.d(TAG, "Bluetooth condition: enabled=$isEnabled")
            isEnabled
        } catch (e: SecurityException) {
            Log.e(TAG, "SecurityException checking Bluetooth for condition", e)
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating Bluetooth condition", e)
            false
        }
    }

    private fun evaluateHeadphonesCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val shouldBeConnected = params["connected"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
            val isConnected = audioManager.isWiredHeadsetOn ||
                              audioManager.isBluetoothA2dpOn ||
                              audioManager.isBluetoothScoOn

            Log.d(TAG, "Headphones condition: connected=$isConnected, expected=$shouldBeConnected")
            isConnected == shouldBeConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating headphones condition", e)
            false
        }
    }

    private fun evaluateScreenStateCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val shouldBeOn = params["screen_on"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            val isScreenOn = powerManager.isInteractive

            Log.d(TAG, "Screen state condition: isOn=$isScreenOn, expected=$shouldBeOn")
            isScreenOn == shouldBeOn
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating screen state condition", e)
            false
        }
    }

    private fun evaluateNetworkTypeCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val expectedType = params["network_type"]?.jsonPrimitive?.content ?: "any"

            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                val result = when (expectedType.lowercase()) {
                    "wifi" -> capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ?: false
                    "cellular", "mobile" -> capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
                    "any" -> capabilities != null
                    "none" -> capabilities == null
                    else -> capabilities != null
                }
                Log.d(TAG, "Network type condition: expected=$expectedType, result=$result")
                result
            } else {
                @Suppress("DEPRECATION")
                val activeNetwork = connectivityManager.activeNetworkInfo
                val isConnected = activeNetwork?.isConnected ?: false
                Log.d(TAG, "Network type condition (legacy): connected=$isConnected")
                isConnected
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating network type condition", e)
            false
        }
    }

    private fun evaluateAirplaneModeCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val shouldBeEnabled = params["enabled"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val isAirplaneModeOn = android.provider.Settings.Global.getInt(
                context.contentResolver,
                android.provider.Settings.Global.AIRPLANE_MODE_ON,
                0
            ) != 0

            Log.d(TAG, "Airplane mode condition: isOn=$isAirplaneModeOn, expected=$shouldBeEnabled")
            isAirplaneModeOn == shouldBeEnabled
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating airplane mode condition", e)
            false
        }
    }

    private fun evaluateDoNotDisturbCondition(condition: Condition): Boolean {
        return try {
            val params = json.parseToJsonElement(condition.parameters).jsonObject
            val shouldBeDndOn = params["dnd_on"]?.jsonPrimitive?.content?.toBoolean() ?: true

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val currentFilter = notificationManager.currentInterruptionFilter
            val isDndOn = currentFilter != android.app.NotificationManager.INTERRUPTION_FILTER_ALL

            Log.d(TAG, "DND condition: isDndOn=$isDndOn, expected=$shouldBeDndOn")
            isDndOn == shouldBeDndOn
        } catch (e: Exception) {
            Log.e(TAG, "Error evaluating DND condition", e)
            false
        }
    }
}
