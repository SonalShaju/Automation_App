package com.example.automationapp.domain.executor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.ActionType
import com.example.automationapp.service.AutomationAccessibilityService
import com.example.automationapp.service.AutomationNotificationListenerService
import com.example.automationapp.util.PermissionHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ActionExecutor - Android 16 Native API Compliant
 *
 * Supports ONLY these categories:
 * - Hardware: Flashlight (CameraManager), Vibrate
 * - Audio: Volume, Ringer Mode, Media Keys (dispatchMediaKeyEvent)
 * - Display: Brightness, Timeout, Rotation (Settings.System)
 * - System: Global Actions (Home, Back, Lock Screen, Screenshot) via AccessibilityService
 * - Apps: Launch App (startActivity with FLAG_ACTIVITY_NEW_TASK)
 * - Notifications: Send Notification, Dismiss All (via NotificationListenerService)
 */
@Singleton
class ActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val json: Json,
    private val permissionHelper: PermissionHelper
) {
    private val TAG = "ActionExecutor"

    // System Services
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    // Flashlight state tracking
    private var isFlashlightOn = false

    /**
     * Execute an automation action using only Android Native APIs
     */
    suspend fun executeAction(action: Action) {
        android.util.Log.d(TAG, "Executing action: ${action.type}")

        when (action.type) {
            // ==================== HARDWARE ====================
            ActionType.TOGGLE_FLASHLIGHT -> toggleFlashlight()
            ActionType.ENABLE_FLASHLIGHT -> setFlashlight(true)
            ActionType.DISABLE_FLASHLIGHT -> setFlashlight(false)
            ActionType.VIBRATE -> vibrate(action.parameters)

            // ==================== AUDIO ====================
            ActionType.ADJUST_VOLUME -> adjustVolume(action.parameters)
            ActionType.SET_RINGER_MODE -> setRingerMode(action.parameters)
            ActionType.TOGGLE_SILENT_MODE -> toggleSilentMode()
            ActionType.TOGGLE_VIBRATE -> toggleVibrateMode()

            // ==================== DISPLAY (Settings.System) ====================
            ActionType.ADJUST_BRIGHTNESS -> adjustBrightness(action.parameters)
            ActionType.TOGGLE_AUTO_BRIGHTNESS -> toggleAutoBrightness()
            ActionType.TOGGLE_AUTO_ROTATE -> toggleAutoRotate()

            // ==================== SYSTEM (AccessibilityService Global Actions) ====================
            ActionType.GLOBAL_ACTION_LOCK_SCREEN -> performGlobalAction { it.lockScreen() }
            ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT -> performGlobalActionWithPrompt { it.takeScreenshot() }
            ActionType.GLOBAL_ACTION_POWER_DIALOG -> performGlobalAction { it.openPowerDialog() }

            // ==================== APPS ====================
            ActionType.LAUNCH_APP -> launchApp(action.parameters)
            ActionType.BLOCK_APP -> blockApp(action.parameters)

            // ==================== NOTIFICATIONS ====================
            ActionType.SEND_NOTIFICATION -> sendNotification(action.parameters)
            ActionType.CLEAR_NOTIFICATIONS -> dismissAllNotifications()

            // ==================== DND (Native API) ====================
            ActionType.ENABLE_DND -> setDoNotDisturb(true)
            ActionType.DISABLE_DND -> setDoNotDisturb(false)
        }

        android.util.Log.d(TAG, "Action ${action.type} completed")
    }

    // ==================== HARDWARE ACTIONS ====================

    /**
     * Toggle flashlight using CameraManager
     */
    private fun toggleFlashlight() {
        setFlashlight(!isFlashlightOn)
    }

    /**
     * Set flashlight state using CameraManager
     */
    private fun setFlashlight(enabled: Boolean) {
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }

            if (cameraId != null) {
                cameraManager.setTorchMode(cameraId, enabled)
                isFlashlightOn = enabled
                android.util.Log.d(TAG, "Flashlight ${if (enabled) "ON" else "OFF"}")
            } else {
                android.util.Log.w(TAG, "No camera with flash available")
            }
        } catch (e: CameraAccessException) {
            android.util.Log.e(TAG, "Failed to access camera for flashlight", e)
            throw ActionExecutionException("Failed to control flashlight: ${e.message}")
        }
    }

    /**
     * Vibrate the device using modern API with VibrationAttributes.
     * Uses USAGE_ALARM to ensure vibration works even if 'Touch Haptics' are disabled.
     *
     * Parameters: { "duration": 500, "pattern": [0, 100, 50, 100] }
     */
    private fun vibrate(params: String) {
        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val duration = jsonParams["duration"]?.jsonPrimitive?.content?.toLongOrNull() ?: 500L
            val patternStr = jsonParams["pattern"]?.jsonPrimitive?.content

            val effect = if (patternStr != null) {
                // Parse pattern like "0,100,50,100"
                val pattern = patternStr.split(",").mapNotNull { it.trim().toLongOrNull() }.toLongArray()
                if (pattern.isNotEmpty()) {
                    VibrationEffect.createWaveform(pattern, -1)
                } else {
                    VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                }
            } else {
                VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
            }

            // Use VibrationAttributes with USAGE_ALARM to ensure vibration triggers
            // even when 'Touch Haptics' is disabled in system settings
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ (API 33+): Use VibrationAttributes with vibrate()
                val vibrationAttributes = VibrationAttributes.Builder()
                    .setUsage(VibrationAttributes.USAGE_ALARM)
                    .build()
                vibrator.vibrate(effect, vibrationAttributes)
            } else {
                // Android 12 and below: Use AudioAttributes approach
                @Suppress("DEPRECATION")
                val audioAttributes = android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
                @Suppress("DEPRECATION")
                vibrator.vibrate(effect, audioAttributes)
            }

            android.util.Log.d(TAG, "Vibration triggered with duration: $duration ms")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to vibrate", e)
            throw ActionExecutionException("Failed to vibrate: ${e.message}")
        }
    }

    // ==================== AUDIO ACTIONS ====================

    /**
     * Adjust volume for a specific stream
     * Parameters: { "level": 50, "streamType": "MUSIC" }
     */
    private fun adjustVolume(params: String) {
        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val level = jsonParams["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
            val streamType = parseStreamType(jsonParams["streamType"]?.jsonPrimitive?.content)

            // Check DND permission for ring/notification streams
            if ((streamType == AudioManager.STREAM_RING || streamType == AudioManager.STREAM_NOTIFICATION)
                && !notificationManager.isNotificationPolicyAccessGranted) {
                android.util.Log.w(TAG, "DND access required for ring/notification volume")
                openDndSettings()
                return
            }

            val maxVolume = audioManager.getStreamMaxVolume(streamType)
            val targetVolume = (level * maxVolume) / 100
            audioManager.setStreamVolume(streamType, targetVolume, 0)
            android.util.Log.d(TAG, "Volume set to $level% for stream $streamType")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to adjust volume", e)
            throw ActionExecutionException("Failed to adjust volume: ${e.message}")
        }
    }

    /**
     * Set ringer mode
     * Parameters: { "mode": "NORMAL" | "SILENT" | "VIBRATE" }
     */
    private fun setRingerMode(params: String) {
        try {
            if (!notificationManager.isNotificationPolicyAccessGranted) {
                android.util.Log.w(TAG, "DND access required to change ringer mode")
                openDndSettings()
                return
            }

            val jsonParams = json.parseToJsonElement(params).jsonObject
            val mode = jsonParams["mode"]?.jsonPrimitive?.content?.uppercase() ?: "NORMAL"

            audioManager.ringerMode = when (mode) {
                "SILENT" -> AudioManager.RINGER_MODE_SILENT
                "VIBRATE" -> AudioManager.RINGER_MODE_VIBRATE
                else -> AudioManager.RINGER_MODE_NORMAL
            }
            android.util.Log.d(TAG, "Ringer mode set to $mode")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to set ringer mode", e)
        }
    }

    /**
     * Toggle silent mode
     */
    private fun toggleSilentMode() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            openDndSettings()
            return
        }
        audioManager.ringerMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {
            AudioManager.RINGER_MODE_NORMAL
        } else {
            AudioManager.RINGER_MODE_SILENT
        }
    }

    /**
     * Toggle vibrate mode
     */
    private fun toggleVibrateMode() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            openDndSettings()
            return
        }
        audioManager.ringerMode = if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            AudioManager.RINGER_MODE_NORMAL
        } else {
            AudioManager.RINGER_MODE_VIBRATE
        }
    }


    private fun parseStreamType(type: String?): Int {
        return when (type?.uppercase()) {
            "MUSIC" -> AudioManager.STREAM_MUSIC
            "RING" -> AudioManager.STREAM_RING
            "ALARM" -> AudioManager.STREAM_ALARM
            "NOTIFICATION" -> AudioManager.STREAM_NOTIFICATION
            "SYSTEM" -> AudioManager.STREAM_SYSTEM
            "VOICE_CALL" -> AudioManager.STREAM_VOICE_CALL
            else -> AudioManager.STREAM_MUSIC
        }
    }

    // ==================== DISPLAY ACTIONS (Settings.System) ====================

    /**
     * Adjust screen brightness
     * Parameters: { "level": 75 }
     */
    private fun adjustBrightness(params: String) {
        if (!Settings.System.canWrite(context)) {
            android.util.Log.w(TAG, "WRITE_SETTINGS permission required")
            openWriteSettings()
            return
        }

        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val level = jsonParams["level"]?.jsonPrimitive?.content?.toIntOrNull() ?: return
            val brightness = (level * 255) / 100

            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, brightness)
            android.util.Log.d(TAG, "Brightness set to $level%")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to adjust brightness", e)
            throw ActionExecutionException("Failed to adjust brightness: ${e.message}")
        }
    }

    /**
     * Toggle auto brightness
     */
    private fun toggleAutoBrightness() {
        if (!Settings.System.canWrite(context)) {
            openWriteSettings()
            return
        }

        try {
            val currentMode = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            val newMode = if (currentMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            } else {
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            }
            Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, newMode)
            android.util.Log.d(TAG, "Auto brightness toggled to ${if (newMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) "ON" else "OFF"}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to toggle auto brightness", e)
        }
    }

    /**
     * Toggle auto rotate
     */
    private fun toggleAutoRotate() {
        if (!Settings.System.canWrite(context)) {
            openWriteSettings()
            return
        }

        try {
            val current = Settings.System.getInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                0
            )
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.ACCELEROMETER_ROTATION,
                if (current == 1) 0 else 1
            )
            android.util.Log.d(TAG, "Auto rotate toggled")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to toggle auto rotate", e)
        }
    }

    // ==================== SYSTEM ACTIONS (AccessibilityService) ====================

    /**
     * Perform a global action via AccessibilityService
     */
    private fun performGlobalAction(action: (AutomationAccessibilityService) -> Boolean) {
        val service = AutomationAccessibilityService.getInstance()
        if (service != null) {
            val success = action(service)
            android.util.Log.d(TAG, "Global action performed: $success")
        } else {
            android.util.Log.w(TAG, "AccessibilityService not available")
            openAccessibilitySettings()
        }
    }

    /**
     * Perform a global action with accessibility service check - specifically for screenshot
     * This provides user-friendly messaging when accessibility service is not enabled
     */
    private fun performGlobalActionWithPrompt(action: (AutomationAccessibilityService) -> Boolean) {
        val service = AutomationAccessibilityService.getInstance()
        if (service != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val success = action(service)
                if (success) {
                    android.util.Log.d(TAG, "Screenshot action performed successfully")
                } else {
                    android.util.Log.w(TAG, "Screenshot action failed")
                    // Send a notification to inform user
                    sendSystemNotification(
                        "Screenshot Failed",
                        "Unable to take screenshot. Please try again."
                    )
                }
            } else {
                android.util.Log.w(TAG, "Screenshot requires Android 9 (Pie) or higher")
                sendSystemNotification(
                    "Screenshot Unavailable",
                    "Taking screenshots requires Android 9 or higher."
                )
            }
        } else {
            android.util.Log.w(TAG, "AccessibilityService not available for screenshot")
            // Prompt user to enable accessibility service
            sendSystemNotification(
                "Enable Accessibility Service",
                "To take screenshots, please enable the Smart Automation Accessibility Service in Settings."
            )
            openAccessibilitySettings()
        }
    }

    /**
     * Send a system notification for action feedback
     */
    private fun sendSystemNotification(title: String, message: String) {
        try {
            val channelId = "automation_system"
            val channel = NotificationChannel(
                channelId,
                "Automation System",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send system notification", e)
        }
    }

    // ==================== APP ACTIONS ====================

    /**
     * Launch an app by package name
     * Parameters: { "packageName": "com.example.app" }
     *
     * Critical: FLAG_ACTIVITY_NEW_TASK is REQUIRED when launching from a background service/receiver.
     * Without it, Android will silently block the launch.
     */
    private fun launchApp(params: String) {
        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val packageName = jsonParams["packageName"]?.jsonPrimitive?.content

            if (packageName.isNullOrBlank()) {
                android.util.Log.w(TAG, "Launch app: No package name provided")
                return
            }

            // Verify the app is installed before attempting launch
            val appInfo = try {
                context.packageManager.getApplicationInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) {
                android.util.Log.e(TAG, "App not installed: $packageName")
                sendSystemNotification(
                    "App Not Found",
                    "The app '$packageName' is not installed."
                )
                return
            }

            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                // CRITICAL FLAGS for background launch:
                // - FLAG_ACTIVITY_NEW_TASK: Required when starting from non-Activity context
                // - FLAG_ACTIVITY_CLEAR_TOP: Brings existing instance to front if running
                // - FLAG_ACTIVITY_RESET_TASK_IF_NEEDED: Ensures clean launch state
                intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                )

                context.startActivity(intent)
                android.util.Log.d(TAG, "Launched app: $packageName")
            } else {
                // App exists but has no launchable activity (e.g., system service)
                android.util.Log.w(TAG, "No launchable activity for: $packageName")
                sendSystemNotification(
                    "Cannot Launch App",
                    "The app has no launchable activity."
                )
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to launch app", e)
            throw ActionExecutionException("Failed to launch app: ${e.message}")
        }
    }

    /**
     * Block an app from opening (App Locker feature)
     * Parameters: { "packageName": "com.example.app" }
     *
     * When executed, this adds the package to the AccessibilityService's blocked apps list.
     * The AccessibilityService will detect when the blocked app is launched and immediately
     * send the user back to the home screen.
     */
    private fun blockApp(params: String) {
        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val packageName = jsonParams["packageName"]?.jsonPrimitive?.content

            if (packageName.isNullOrBlank()) {
                android.util.Log.w(TAG, "Block app: No package name provided")
                return
            }

            // Check if accessibility service is available
            val service = AutomationAccessibilityService.getInstance()
            if (service == null) {
                android.util.Log.w(TAG, "AccessibilityService not available for app blocking")
                sendSystemNotification(
                    "Enable Accessibility Service",
                    "To block apps, please enable the Smart Automation Accessibility Service in Settings."
                )
                openAccessibilitySettings()
                return
            }

            // Add to blocked apps list
            AutomationAccessibilityService.blockApp(packageName)
            android.util.Log.d(TAG, "App blocked: $packageName")

            // Send confirmation notification
            sendSystemNotification(
                "App Blocked",
                "The app will be blocked when opened."
            )
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to block app", e)
            throw ActionExecutionException("Failed to block app: ${e.message}")
        }
    }


    // ==================== NOTIFICATION ACTIONS ====================

    /**
     * Send a notification
     * Parameters: { "title": "Title", "message": "Message", "channelId": "channel_id" }
     */
    private fun sendNotification(params: String) {
        try {
            val jsonParams = json.parseToJsonElement(params).jsonObject
            val title = jsonParams["title"]?.jsonPrimitive?.content ?: "Automation"
            val message = jsonParams["message"]?.jsonPrimitive?.content ?: ""
            val channelId = jsonParams["channelId"]?.jsonPrimitive?.content ?: "automation_actions"

            // Create notification channel
            val channel = NotificationChannel(
                channelId,
                "Automation Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)

            val notification = NotificationCompat.Builder(context, channelId)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(System.currentTimeMillis().toInt(), notification)
            android.util.Log.d(TAG, "Notification sent: $title")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to send notification", e)
            throw ActionExecutionException("Failed to send notification: ${e.message}")
        }
    }

    /**
     * Dismiss all notifications via NotificationListenerService
     */
    private fun dismissAllNotifications() {
        val service = AutomationNotificationListenerService.getInstance()
        if (service != null) {
            service.cancelAllNotifications()
            android.util.Log.d(TAG, "All notifications dismissed")
        } else {
            android.util.Log.w(TAG, "NotificationListenerService not available")
            openNotificationListenerSettings()
        }
    }

    // ==================== DND ACTIONS ====================

    /**
     * Set Do Not Disturb mode
     */
    private fun setDoNotDisturb(enabled: Boolean) {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            openDndSettings()
            return
        }

        val filter = if (enabled) {
            NotificationManager.INTERRUPTION_FILTER_NONE
        } else {
            NotificationManager.INTERRUPTION_FILTER_ALL
        }
        notificationManager.setInterruptionFilter(filter)
        android.util.Log.d(TAG, "DND ${if (enabled) "enabled" else "disabled"}")
    }

    // ==================== SETTINGS HELPERS ====================

    private fun openDndSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openWriteSettings() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.data = android.net.Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openOverlaySettings() {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
        intent.data = android.net.Uri.parse("package:${context.packageName}")
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun openNotificationListenerSettings() {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

/**
 * Custom exception for action execution failures
 */
class ActionExecutionException(message: String) : Exception(message)
