package com.example.automationapp.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AccessibilityService for automation features:
 * - Detecting app launches (APP_OPENED trigger)
 * - Performing "Ghost Finger" gestures for system settings toggles
 * - Reading window content for UI automation
 */
class AutomationAccessibilityService : AccessibilityService() {

    companion object {
        private const val TAG = "AutomationAccessibility"

        // Singleton instance for accessing from other parts of the app
        @Volatile
        private var instance: AutomationAccessibilityService? = null

        // SharedFlow for accessibility events that other components can observe
        private val _accessibilityEvents = MutableSharedFlow<AccessibilityEventData>(
            replay = 1,
            extraBufferCapacity = 10
        )
        val accessibilityEvents: SharedFlow<AccessibilityEventData> = _accessibilityEvents.asSharedFlow()

        // StateFlow for currently foreground app package
        private val _currentForegroundApp = MutableStateFlow<String?>(null)
        val currentForegroundApp: StateFlow<String?> = _currentForegroundApp.asStateFlow()

        // StateFlow for service connection status
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        // Set of package names that should be blocked (App Locker feature)
        private val blockedApps = mutableSetOf<String>()
        private val blockedAppsLock = Any()

        /**
         * Get the current instance of the service (if connected)
         */
        fun getInstance(): AutomationAccessibilityService? = instance

        /**
         * Check if accessibility service is enabled
         */
        fun isServiceEnabled(): Boolean = instance != null && _isServiceConnected.value

        /**
         * Add an app to the blocked list
         */
        fun blockApp(packageName: String) {
            synchronized(blockedAppsLock) {
                blockedApps.add(packageName)
                Log.d(TAG, "Added app to block list: $packageName. Total blocked: ${blockedApps.size}")
            }
        }

        /**
         * Remove an app from the blocked list
         */
        fun unblockApp(packageName: String) {
            synchronized(blockedAppsLock) {
                blockedApps.remove(packageName)
                Log.d(TAG, "Removed app from block list: $packageName. Total blocked: ${blockedApps.size}")
            }
        }

        /**
         * Clear all blocked apps
         */
        fun clearBlockedApps() {
            synchronized(blockedAppsLock) {
                blockedApps.clear()
                Log.d(TAG, "Cleared all blocked apps")
            }
        }

        /**
         * Check if an app is blocked
         */
        fun isAppBlocked(packageName: String): Boolean {
            synchronized(blockedAppsLock) {
                return blockedApps.contains(packageName)
            }
        }

        /**
         * Get the current list of blocked apps
         */
        fun getBlockedApps(): Set<String> {
            synchronized(blockedAppsLock) {
                return blockedApps.toSet()
            }
        }
    }

    /**
     * Data class representing an accessibility event
     */
    data class AccessibilityEventData(
        val eventType: Int,
        val packageName: String?,
        val className: String?,
        val text: List<CharSequence>,
        val contentDescription: CharSequence?,
        val timestamp: Long = System.currentTimeMillis()
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        _isServiceConnected.value = true

        // Configure the service
        serviceInfo = serviceInfo?.apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                    AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS or
                    AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE or
                    AccessibilityServiceInfo.DEFAULT
            notificationTimeout = 100
        }

        Log.d(TAG, "Accessibility service connected and configured")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val packageName = event.packageName?.toString()
        val className = event.className?.toString()

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // App/window changed - this is how we detect app launches
                handleWindowStateChanged(packageName, className, event)
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Content within a window changed
                handleWindowContentChanged(packageName, className, event)
            }
        }

        // Emit event to SharedFlow for observers
        val eventData = AccessibilityEventData(
            eventType = event.eventType,
            packageName = packageName,
            className = className,
            text = event.text.toList(),
            contentDescription = event.contentDescription
        )

        _accessibilityEvents.tryEmit(eventData)
    }

    private fun handleWindowStateChanged(
        packageName: String?,
        className: String?,
        @Suppress("UNUSED_PARAMETER") event: AccessibilityEvent
    ) {
        if (packageName == null) return

        // Filter out system UI components that aren't actual app launches
        val isSystemUI = packageName == "com.android.systemui"
        val isKeyboard = className?.contains("InputMethod") == true
        val isLauncher = className?.contains("Launcher") == true ||
                         packageName.contains("launcher") ||
                         packageName.contains("home")

        if (!isSystemUI && !isKeyboard) {
            val previousApp = _currentForegroundApp.value
            if (previousApp != packageName) {
                _currentForegroundApp.value = packageName
                Log.d(TAG, "App changed: $previousApp -> $packageName (class: $className)")

                // Check if this app is blocked (App Locker feature)
                // Don't block launchers to avoid infinite loops
                if (!isLauncher && isAppBlocked(packageName)) {
                    Log.d(TAG, "BLOCKED APP DETECTED: $packageName - Sending to home screen")
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }
            }
        }
    }

    private fun handleWindowContentChanged(
        packageName: String?,
        @Suppress("UNUSED_PARAMETER") className: String?,
        event: AccessibilityEvent
    ) {
        // This can be used to detect specific content changes
        // Useful for finding "Mobile Data" toggle text in Quick Settings
        Log.v(TAG, "Content changed in $packageName: ${event.text}")
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        _isServiceConnected.value = false
        _currentForegroundApp.value = null
        Log.d(TAG, "Accessibility service destroyed")
    }

    // ==================== Global Action Helpers ====================

    /**
     * Press the Back button
     */
    fun pressBack(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_BACK)
    }

    /**
     * Press the Home button
     */
    fun pressHome(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_HOME)
    }

    /**
     * Press the Recents/Overview button
     */
    fun pressRecents(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    /**
     * Open the notifications panel
     */
    fun openNotifications(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
    }

    /**
     * Open Quick Settings panel
     */
    fun openQuickSettings(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
    }

    /**
     * Lock the screen (Android 9+)
     */
    fun lockScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            Log.w(TAG, "Lock screen action requires Android 9+")
            false
        }
    }

    /**
     * Take a screenshot (Android 9+)
     */
    fun takeScreenshot(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            Log.w(TAG, "Screenshot action requires Android 9+")
            false
        }
    }

    /**
     * Open power dialog (Android 5+)
     */
    fun openPowerDialog(): Boolean {
        return performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
    }

    // ==================== Gesture Helpers (Ghost Finger) ====================

    /**
     * Perform a tap gesture at the specified coordinates
     * This is the core "Ghost Finger" functionality
     */
    fun performTap(x: Float, y: Float, callback: ((Boolean) -> Unit)? = null): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Tap gesture completed at ($x, $y)")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Tap gesture cancelled at ($x, $y)")
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Perform a swipe gesture from one point to another
     */
    fun performSwipe(
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        duration: Long = 300,
        callback: ((Boolean) -> Unit)? = null
    ): Boolean {
        val path = Path().apply {
            moveTo(startX, startY)
            lineTo(endX, endY)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Swipe gesture completed from ($startX, $startY) to ($endX, $endY)")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Swipe gesture cancelled")
                callback?.invoke(false)
            }
        }, null)
    }

    /**
     * Perform a long press gesture at the specified coordinates
     */
    fun performLongPress(
        x: Float,
        y: Float,
        duration: Long = 1000,
        callback: ((Boolean) -> Unit)? = null
    ): Boolean {
        val path = Path().apply {
            moveTo(x, y)
        }

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Long press gesture completed at ($x, $y)")
                callback?.invoke(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Long press gesture cancelled at ($x, $y)")
                callback?.invoke(false)
            }
        }, null)
    }

    // ==================== Node Finding Helpers ====================

    /**
     * Find a node by text content
     */
    fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByText(text)
        return nodes.firstOrNull()
    }

    /**
     * Find a node by view ID
     */
    fun findNodeByViewId(viewId: String): AccessibilityNodeInfo? {
        val rootNode = rootInActiveWindow ?: return null
        val nodes = rootNode.findAccessibilityNodeInfosByViewId(viewId)
        return nodes.firstOrNull()
    }

    /**
     * Click on a node found by text
     */
    @Suppress("DEPRECATION")
    fun clickByText(text: String): Boolean {
        val node = findNodeByText(text)
        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle() // Still needed for compatibility
            result
        } else {
            Log.w(TAG, "Could not find node with text: $text")
            false
        }
    }

    /**
     * Click on a node found by view ID
     */
    @Suppress("DEPRECATION")
    fun clickByViewId(viewId: String): Boolean {
        val node = findNodeByViewId(viewId)
        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle() // Still needed for compatibility
            result
        } else {
            Log.w(TAG, "Could not find node with viewId: $viewId")
            false
        }
    }

    /**
     * Get all text content from current window (useful for debugging)
     */
    @Suppress("DEPRECATION")
    fun getAllWindowText(): List<String> {
        val texts = mutableListOf<String>()
        val rootNode = rootInActiveWindow ?: return texts

        fun traverseNode(node: AccessibilityNodeInfo?) {
            if (node == null) return

            node.text?.let { texts.add(it.toString()) }
            node.contentDescription?.let { texts.add(it.toString()) }

            for (i in 0 until node.childCount) {
                traverseNode(node.getChild(i))
            }
        }

        traverseNode(rootNode)
        rootNode.recycle()
        return texts
    }
}

