package com.example.automationapp

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.automationapp.service.TriggerMonitorService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class SmartAutomationApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()

        // Start the trigger monitor service to keep triggers active
        TriggerMonitorService.start(this)
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channels = listOf(
                NotificationChannel(
                    AUTOMATION_CHANNEL_ID,
                    "Automation Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications from automation rules"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    SYSTEM_CHANNEL_ID,
                    "System Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Important system notifications"
                    enableLights(true)
                    enableVibration(true)
                },
                NotificationChannel(
                    TRIGGER_MONITOR_CHANNEL_ID,
                    "Trigger Monitor Service",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Keeps automation triggers active in the background"
                    setShowBadge(false)
                }
            )

            val notificationManager = getSystemService(NotificationManager::class.java)
            channels.forEach { channel ->
                notificationManager.createNotificationChannel(channel)
            }
        }
    }

    companion object {
        const val AUTOMATION_CHANNEL_ID = "automation_channel"
        const val SYSTEM_CHANNEL_ID = "system_channel"
        const val TRIGGER_MONITOR_CHANNEL_ID = "trigger_monitor_channel"
    }
}