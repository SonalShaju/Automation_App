package com.example.automationapp.util

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.data.local.entity.TriggerType
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage permission checks for triggers and actions.
 * Provides centralized permission verification for all automation features.
 */
@Singleton
class PermissionHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager: NotificationManager by lazy {
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    /**
     * Data class representing a required permission with its description
     */
    data class PermissionInfo(
        val permission: String,
        val displayName: String,
        val description: String,
        val isSpecialPermission: Boolean = false
    )

    /**
     * Result of permission check containing missing permissions and details
     */
    data class PermissionCheckResult(
        val hasAllPermissions: Boolean,
        val missingPermissions: List<PermissionInfo>,
        val missingSpecialPermissions: List<PermissionInfo>
    ) {
        val allMissing: List<PermissionInfo>
            get() = missingPermissions + missingSpecialPermissions
    }

    // ==================== Trigger Permission Mappings ====================

    /**
     * Get required permissions for a specific trigger type
     */
    fun getRequiredPermissionsForTrigger(triggerType: TriggerType): List<PermissionInfo> {
        return when (triggerType) {
            TriggerType.LOCATION_BASED -> {
                val permissions = mutableListOf(
                    PermissionInfo(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        "Fine Location",
                        "Required to detect your precise location for geofence triggers"
                    ),
                    PermissionInfo(
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        "Coarse Location",
                        "Required to detect your approximate location"
                    )
                )
                // Background location only required on Android 10+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    permissions.add(
                        PermissionInfo(
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            "Background Location",
                            "Required to detect location even when the app is closed"
                        )
                    )
                }
                permissions
            }

            TriggerType.WIFI_CONNECTED -> {
                // Android 8.0+ requires location permission to get WiFi SSID
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    listOf(
                        PermissionInfo(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            "Location",
                            "Required to read WiFi network name (SSID) on Android 8+"
                        )
                    )
                } else {
                    emptyList()
                }
            }

            TriggerType.BLUETOOTH_CONNECTED -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    listOf(
                        PermissionInfo(
                            Manifest.permission.BLUETOOTH_CONNECT,
                            "Bluetooth Connect",
                            "Required to detect Bluetooth device connections"
                        ),
                        PermissionInfo(
                            Manifest.permission.BLUETOOTH_SCAN,
                            "Bluetooth Scan",
                            "Required to scan for nearby Bluetooth devices"
                        )
                    )
                } else {
                    listOf(
                        PermissionInfo(
                            Manifest.permission.BLUETOOTH,
                            "Bluetooth",
                            "Required to check Bluetooth state"
                        )
                    )
                }
            }

            // Triggers that don't require runtime permissions
            TriggerType.TIME_BASED,
            TriggerType.TIME_RANGE,
            TriggerType.BATTERY_LEVEL,
            TriggerType.CHARGING_STATUS,
            TriggerType.HEADPHONES_CONNECTED,
            TriggerType.APP_OPENED,
            TriggerType.AIRPLANE_MODE,
            TriggerType.DO_NOT_DISTURB -> emptyList()
        }
    }

    // ==================== Action Permission Mappings ====================

    /**
     * Get required permissions for a specific action type
     */
    fun getRequiredPermissionsForAction(actionType: ActionType): List<PermissionInfo> {
        return when (actionType) {

            // Display settings actions
            ActionType.ADJUST_BRIGHTNESS,
            ActionType.TOGGLE_AUTO_BRIGHTNESS,
            ActionType.TOGGLE_AUTO_ROTATE -> listOf(
                PermissionInfo(
                    Manifest.permission.WRITE_SETTINGS,
                    "Write Settings",
                    "Required to modify system display settings",
                    isSpecialPermission = true
                )
            )

            // DND actions
            ActionType.ENABLE_DND,
            ActionType.DISABLE_DND,
            ActionType.TOGGLE_SILENT_MODE,
            ActionType.TOGGLE_VIBRATE,
            ActionType.SET_RINGER_MODE -> listOf(
                PermissionInfo(
                    Manifest.permission.ACCESS_NOTIFICATION_POLICY,
                    "Notification Policy Access",
                    "Required to control Do Not Disturb and ringer mode",
                    isSpecialPermission = true
                )
            )

            // Notification actions
            ActionType.SEND_NOTIFICATION -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    listOf(
                        PermissionInfo(
                            Manifest.permission.POST_NOTIFICATIONS,
                            "Post Notifications",
                            "Required to show notifications"
                        )
                    )
                } else {
                    emptyList()
                }
            }

            // Actions that don't require special permissions
            ActionType.TOGGLE_FLASHLIGHT,
            ActionType.ENABLE_FLASHLIGHT,
            ActionType.DISABLE_FLASHLIGHT,
            ActionType.VIBRATE,
            ActionType.ADJUST_VOLUME,
            ActionType.GLOBAL_ACTION_LOCK_SCREEN,
            ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT,
            ActionType.GLOBAL_ACTION_POWER_DIALOG,
            ActionType.LAUNCH_APP,
            ActionType.BLOCK_APP,
            ActionType.CLEAR_NOTIFICATIONS -> emptyList()
        }
    }

    // ==================== Permission Check Methods ====================

    /**
     * Check if a specific permission is granted
     */
    fun hasPermission(permission: String): Boolean {
        return when (permission) {
            Manifest.permission.WRITE_SETTINGS -> Settings.System.canWrite(context)
            Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }
            }
            else -> ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Check all required permissions for a trigger type
     */
    fun checkPermissionsForTrigger(triggerType: TriggerType): PermissionCheckResult {
        val requiredPermissions = getRequiredPermissionsForTrigger(triggerType)
        return checkPermissions(requiredPermissions)
    }

    /**
     * Check all required permissions for an action type
     */
    fun checkPermissionsForAction(actionType: ActionType): PermissionCheckResult {
        val requiredPermissions = getRequiredPermissionsForAction(actionType)
        return checkPermissions(requiredPermissions)
    }

    /**
     * Check a list of permissions and return the result
     */
    private fun checkPermissions(permissions: List<PermissionInfo>): PermissionCheckResult {
        val missingRegular = mutableListOf<PermissionInfo>()
        val missingSpecial = mutableListOf<PermissionInfo>()

        permissions.forEach { permissionInfo ->
            if (!hasPermission(permissionInfo.permission)) {
                if (permissionInfo.isSpecialPermission) {
                    missingSpecial.add(permissionInfo)
                } else {
                    missingRegular.add(permissionInfo)
                }
            }
        }

        return PermissionCheckResult(
            hasAllPermissions = missingRegular.isEmpty() && missingSpecial.isEmpty(),
            missingPermissions = missingRegular,
            missingSpecialPermissions = missingSpecial
        )
    }

    /**
     * Check permissions for multiple triggers
     */
    fun checkPermissionsForTriggers(triggers: List<TriggerType>): PermissionCheckResult {
        val allMissingRegular = mutableListOf<PermissionInfo>()
        val allMissingSpecial = mutableListOf<PermissionInfo>()

        triggers.forEach { triggerType ->
            val result = checkPermissionsForTrigger(triggerType)
            allMissingRegular.addAll(result.missingPermissions)
            allMissingSpecial.addAll(result.missingSpecialPermissions)
        }

        // Remove duplicates
        val uniqueMissingRegular = allMissingRegular.distinctBy { it.permission }
        val uniqueMissingSpecial = allMissingSpecial.distinctBy { it.permission }

        return PermissionCheckResult(
            hasAllPermissions = uniqueMissingRegular.isEmpty() && uniqueMissingSpecial.isEmpty(),
            missingPermissions = uniqueMissingRegular,
            missingSpecialPermissions = uniqueMissingSpecial
        )
    }

    /**
     * Check permissions for multiple actions
     */
    fun checkPermissionsForActions(actions: List<ActionType>): PermissionCheckResult {
        val allMissingRegular = mutableListOf<PermissionInfo>()
        val allMissingSpecial = mutableListOf<PermissionInfo>()

        actions.forEach { actionType ->
            val result = checkPermissionsForAction(actionType)
            allMissingRegular.addAll(result.missingPermissions)
            allMissingSpecial.addAll(result.missingSpecialPermissions)
        }

        // Remove duplicates
        val uniqueMissingRegular = allMissingRegular.distinctBy { it.permission }
        val uniqueMissingSpecial = allMissingSpecial.distinctBy { it.permission }

        return PermissionCheckResult(
            hasAllPermissions = uniqueMissingRegular.isEmpty() && uniqueMissingSpecial.isEmpty(),
            missingPermissions = uniqueMissingRegular,
            missingSpecialPermissions = uniqueMissingSpecial
        )
    }

    // ==================== Utility Methods ====================

    /**
     * Get all runtime permissions (non-special) needed for a trigger/action combination
     */
    fun getRuntimePermissionsForRule(
        triggers: List<TriggerType>,
        actions: List<ActionType>
    ): List<String> {
        val permissions = mutableSetOf<String>()

        triggers.forEach { trigger ->
            getRequiredPermissionsForTrigger(trigger)
                .filter { !it.isSpecialPermission }
                .forEach { permissions.add(it.permission) }
        }

        actions.forEach { action ->
            getRequiredPermissionsForAction(action)
                .filter { !it.isSpecialPermission }
                .forEach { permissions.add(it.permission) }
        }

        return permissions.toList()
    }

    /**
     * Check if exact alarms can be scheduled (Android 12+)
     */
    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
            alarmManager?.canScheduleExactAlarms() ?: false
        } else {
            true
        }
    }

    /**
     * Get human-readable description of missing permissions for a trigger
     */
    fun getMissingPermissionsDescription(triggerType: TriggerType): String? {
        val result = checkPermissionsForTrigger(triggerType)
        if (result.hasAllPermissions) return null

        val missing = result.allMissing.joinToString(", ") { it.displayName }
        return "Missing permissions: $missing"
    }

    /**
     * Get human-readable description of missing permissions for an action
     */
    fun getMissingPermissionsDescription(actionType: ActionType): String? {
        val result = checkPermissionsForAction(actionType)
        if (result.hasAllPermissions) return null

        val missing = result.allMissing.joinToString(", ") { it.displayName }
        return "Missing permissions: $missing"
    }

    companion object {
        private const val TAG = "PermissionHelper"
    }
}

