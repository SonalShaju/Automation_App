package com.example.automationapp.util

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Centralized Permission Manager for handling both Runtime and Special System permissions.
 *
 * This manager provides:
 * - Check functions for all permission types
 * - Intent creation for special permission settings pages
 * - Permission request helpers for Activities
 */
@Singleton
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ==================== Special Permission Types ====================

    /**
     * Enum representing special permissions that require Settings page navigation
     */
    enum class SpecialPermission(
        val permissionName: String,
        val displayName: String,
        val description: String
    ) {
        WRITE_SETTINGS(
            Manifest.permission.WRITE_SETTINGS,
            "Modify System Settings",
            "Required to change brightness, screen timeout, and other display settings"
        ),
        OVERLAY(
            Manifest.permission.SYSTEM_ALERT_WINDOW,
            "Display Over Other Apps",
            "Required to show overlays and floating windows"
        ),
        NOTIFICATION_LISTENER(
            "android.permission.BIND_NOTIFICATION_LISTENER_SERVICE",
            "Notification Access",
            "Required to read and respond to notifications from other apps"
        ),
        ACCESSIBILITY(
            "android.permission.BIND_ACCESSIBILITY_SERVICE",
            "Accessibility Service",
            "Required to perform global actions like pressing Back or Home"
        ),
        DND_POLICY(
            Manifest.permission.ACCESS_NOTIFICATION_POLICY,
            "Do Not Disturb Access",
            "Required to control Do Not Disturb mode and ringer settings"
        ),
        EXACT_ALARM(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                Manifest.permission.SCHEDULE_EXACT_ALARM
            else "",
            "Schedule Exact Alarms",
            "Required to trigger automations at precise times"
        ),
        USAGE_STATS(
            Manifest.permission.PACKAGE_USAGE_STATS,
            "Usage Access",
            "Required to detect which apps are currently running"
        )
    }

    // ==================== Runtime Permission Checks ====================

    /**
     * Check if a standard runtime permission is granted
     */
    fun hasRuntimePermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        val fine = hasRuntimePermission(Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = hasRuntimePermission(Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine || coarse
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            hasRuntimePermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else {
            hasLocationPermission()
        }
    }

    /**
     * Check if Bluetooth permissions are granted
     */
    fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            hasRuntimePermission(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            hasRuntimePermission(Manifest.permission.BLUETOOTH)
        }
    }

    /**
     * Check if notification posting permission is granted (Android 13+)
     */
    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasRuntimePermission(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            true // Not required before Android 13
        }
    }

    /**
     * Check if phone permissions are granted
     */
    fun hasPhonePermission(): Boolean {
        return hasRuntimePermission(Manifest.permission.READ_PHONE_STATE)
    }

    /**
     * Check if SMS permissions are granted
     */
    fun hasSmsPermission(): Boolean {
        return hasRuntimePermission(Manifest.permission.RECEIVE_SMS) &&
               hasRuntimePermission(Manifest.permission.READ_SMS)
    }

    // ==================== Special Permission Checks ====================

    /**
     * Check if Write Settings permission is granted
     */
    fun hasWriteSettingsPermission(): Boolean {
        return Settings.System.canWrite(context)
    }

    /**
     * Check if Overlay (Draw Over Apps) permission is granted
     */
    fun hasOverlayPermission(): Boolean {
        return Settings.canDrawOverlays(context)
    }

    /**
     * Check if Notification Listener permission is granted
     */
    fun hasNotificationListenerPermission(): Boolean {
        val packageName = context.packageName
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        )
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":").toTypedArray()
            for (name in names) {
                val componentName = ComponentName.unflattenFromString(name)
                if (componentName != null && componentName.packageName == packageName) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Check if Accessibility Service is enabled
     */
    fun hasAccessibilityPermission(): Boolean {
        val accessibilityServiceName = "${context.packageName}/.service.AutomationAccessibilityService"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return enabledServices.contains(context.packageName)
    }

    /**
     * Check if DND Policy Access is granted
     */
    fun hasDndPolicyPermission(): Boolean {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        return notificationManager.isNotificationPolicyAccessGranted
    }

    /**
     * Check if Exact Alarm permission is granted (Android 12+)
     */
    fun hasExactAlarmPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                as android.app.AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true // Not required before Android 12
        }
    }

    /**
     * Check if Usage Stats permission is granted
     */
    fun hasUsageStatsPermission(): Boolean {
        val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE)
            as android.app.AppOpsManager
        val mode = appOpsManager.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            context.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }

    /**
     * Check if a special permission is granted
     */
    fun hasSpecialPermission(permission: SpecialPermission): Boolean {
        return when (permission) {
            SpecialPermission.WRITE_SETTINGS -> hasWriteSettingsPermission()
            SpecialPermission.OVERLAY -> hasOverlayPermission()
            SpecialPermission.NOTIFICATION_LISTENER -> hasNotificationListenerPermission()
            SpecialPermission.ACCESSIBILITY -> hasAccessibilityPermission()
            SpecialPermission.DND_POLICY -> hasDndPolicyPermission()
            SpecialPermission.EXACT_ALARM -> hasExactAlarmPermission()
            SpecialPermission.USAGE_STATS -> hasUsageStatsPermission()
        }
    }

    // ==================== Intent Creators for Special Permissions ====================

    /**
     * Get the Intent to open the Write Settings permission page
     */
    fun getWriteSettingsIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the Overlay permission page
     */
    fun getOverlayPermissionIntent(): Intent {
        return Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the Notification Listener settings
     */
    fun getNotificationListenerIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the Accessibility settings
     */
    fun getAccessibilityIntent(): Intent {
        return Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the DND Policy Access settings
     */
    fun getDndPolicyIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the Exact Alarm settings (Android 12+)
     */
    fun getExactAlarmIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            getAppSettingsIntent()
        }
    }

    /**
     * Get the Intent to open the Usage Stats settings
     */
    fun getUsageStatsIntent(): Intent {
        return Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the Intent to open the app's settings page
     */
    fun getAppSettingsIntent(): Intent {
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Get the appropriate Intent for a special permission
     */
    fun getIntentForSpecialPermission(permission: SpecialPermission): Intent {
        return when (permission) {
            SpecialPermission.WRITE_SETTINGS -> getWriteSettingsIntent()
            SpecialPermission.OVERLAY -> getOverlayPermissionIntent()
            SpecialPermission.NOTIFICATION_LISTENER -> getNotificationListenerIntent()
            SpecialPermission.ACCESSIBILITY -> getAccessibilityIntent()
            SpecialPermission.DND_POLICY -> getDndPolicyIntent()
            SpecialPermission.EXACT_ALARM -> getExactAlarmIntent()
            SpecialPermission.USAGE_STATS -> getUsageStatsIntent()
        }
    }

    /**
     * Launch the settings page for a special permission
     */
    fun launchSpecialPermissionSettings(permission: SpecialPermission) {
        val intent = getIntentForSpecialPermission(permission)
        context.startActivity(intent)
    }

    // ==================== Permission Request Helpers ====================

    /**
     * Request runtime permissions from an Activity
     */
    fun requestRuntimePermissions(
        activity: Activity,
        permissions: Array<String>,
        requestCode: Int
    ) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }

    /**
     * Get all location permissions needed
     */
    fun getLocationPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    /**
     * Get all Bluetooth permissions needed
     */
    fun getBluetoothPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            )
        }
    }

    /**
     * Get notification permission (Android 13+)
     */
    fun getNotificationPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            emptyArray()
        }
    }

    // ==================== Action-specific Permission Info ====================

    /**
     * Data class containing permission information for an action
     */
    data class ActionPermissionInfo(
        val requiresRuntimePermission: Boolean,
        val runtimePermissions: List<String>,
        val requiresSpecialPermission: Boolean,
        val specialPermission: SpecialPermission?,
        val rationale: String
    )

    /**
     * Get permission requirements for common actions
     */
    fun getPermissionInfoForBrightnessAction(): ActionPermissionInfo {
        return ActionPermissionInfo(
            requiresRuntimePermission = false,
            runtimePermissions = emptyList(),
            requiresSpecialPermission = !hasWriteSettingsPermission(),
            specialPermission = SpecialPermission.WRITE_SETTINGS,
            rationale = "To change screen brightness, you must allow 'Modify System Settings' on the next screen."
        )
    }

    fun getPermissionInfoForDndAction(): ActionPermissionInfo {
        return ActionPermissionInfo(
            requiresRuntimePermission = false,
            runtimePermissions = emptyList(),
            requiresSpecialPermission = !hasDndPolicyPermission(),
            specialPermission = SpecialPermission.DND_POLICY,
            rationale = "To control Do Not Disturb mode, you must grant 'Notification Policy Access' on the next screen."
        )
    }

    fun getPermissionInfoForRingerAction(): ActionPermissionInfo {
        return ActionPermissionInfo(
            requiresRuntimePermission = false,
            runtimePermissions = emptyList(),
            requiresSpecialPermission = !hasDndPolicyPermission(),
            specialPermission = SpecialPermission.DND_POLICY,
            rationale = "To change ringer mode, you must grant 'Notification Policy Access' on the next screen."
        )
    }

    fun getPermissionInfoForAccessibilityAction(): ActionPermissionInfo {
        return ActionPermissionInfo(
            requiresRuntimePermission = false,
            runtimePermissions = emptyList(),
            requiresSpecialPermission = !hasAccessibilityPermission(),
            specialPermission = SpecialPermission.ACCESSIBILITY,
            rationale = "To perform system actions (Home, Back, etc.), you must enable the Accessibility Service on the next screen."
        )
    }

    fun getPermissionInfoForNotificationListenerAction(): ActionPermissionInfo {
        return ActionPermissionInfo(
            requiresRuntimePermission = false,
            runtimePermissions = emptyList(),
            requiresSpecialPermission = !hasNotificationListenerPermission(),
            specialPermission = SpecialPermission.NOTIFICATION_LISTENER,
            rationale = "To read and clear notifications, you must grant 'Notification Access' on the next screen."
        )
    }
}

