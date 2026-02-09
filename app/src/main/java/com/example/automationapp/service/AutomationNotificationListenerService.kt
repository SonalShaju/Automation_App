package com.example.automationapp.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NotificationListenerService for:
 * - Detecting incoming notifications (for triggers)
 * - Dismissing all notifications (for actions)
 */
class AutomationNotificationListenerService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotificationListener"

        @Volatile
        private var instance: AutomationNotificationListenerService? = null

        // StateFlow for service connection status
        private val _isServiceConnected = MutableStateFlow(false)
        val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

        // SharedFlow for notification events
        private val _notificationEvents = MutableSharedFlow<NotificationEvent>(
            replay = 1,
            extraBufferCapacity = 20
        )
        val notificationEvents: SharedFlow<NotificationEvent> = _notificationEvents.asSharedFlow()

        /**
         * Get the current instance of the service
         */
        fun getInstance(): AutomationNotificationListenerService? = instance

        /**
         * Check if the service is enabled and connected
         */
        fun isServiceEnabled(): Boolean = instance != null && _isServiceConnected.value
    }

    /**
     * Data class representing a notification event
     */
    data class NotificationEvent(
        val type: NotificationEventType,
        val packageName: String,
        val title: String?,
        val text: String?,
        val timestamp: Long = System.currentTimeMillis()
    )

    enum class NotificationEventType {
        POSTED,
        REMOVED
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        _isServiceConnected.value = true
        Log.d(TAG, "NotificationListenerService connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        instance = null
        _isServiceConnected.value = false
        Log.d(TAG, "NotificationListenerService disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        Log.d(TAG, "Notification posted from $packageName: $title")

        val event = NotificationEvent(
            type = NotificationEventType.POSTED,
            packageName = packageName,
            title = title,
            text = text
        )

        _notificationEvents.tryEmit(event)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        sbn ?: return

        val packageName = sbn.packageName
        val notification = sbn.notification
        val extras = notification.extras

        val title = extras.getCharSequence("android.title")?.toString()
        val text = extras.getCharSequence("android.text")?.toString()

        Log.d(TAG, "Notification removed from $packageName: $title")

        val event = NotificationEvent(
            type = NotificationEventType.REMOVED,
            packageName = packageName,
            title = title,
            text = text
        )

        _notificationEvents.tryEmit(event)
    }

    // ==================== Action Methods ====================

    /**
     * Dismiss all active notifications
     */
    fun dismissAllNotifications() {
        try {
            cancelAllNotifications()
            Log.d(TAG, "All notifications dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss all notifications", e)
        }
    }

    /**
     * Dismiss notifications from a specific package
     */
    fun dismissNotificationsFromPackage(packageName: String) {
        try {
            activeNotifications?.filter { it.packageName == packageName }?.forEach { sbn ->
                cancelNotification(sbn.key)
            }
            Log.d(TAG, "Notifications from $packageName dismissed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to dismiss notifications from $packageName", e)
        }
    }

    /**
     * Get all active notifications
     */
    fun getActiveNotificationsList(): List<NotificationInfo> {
        return try {
            activeNotifications?.map { sbn ->
                val extras = sbn.notification.extras
                NotificationInfo(
                    key = sbn.key,
                    packageName = sbn.packageName,
                    title = extras.getCharSequence("android.title")?.toString(),
                    text = extras.getCharSequence("android.text")?.toString(),
                    postTime = sbn.postTime
                )
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get active notifications", e)
            emptyList()
        }
    }

    /**
     * Data class for notification info
     */
    data class NotificationInfo(
        val key: String,
        val packageName: String,
        val title: String?,
        val text: String?,
        val postTime: Long
    )
}

