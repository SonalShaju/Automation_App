package com.example.automationapp.service

import android.content.Context
import com.example.automationapp.domain.repository.AutomationRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to manage the TriggerMonitorService lifecycle.
 * Provides methods to start/stop the service and check its status.
 */
@Singleton
class TriggerMonitorServiceManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: AutomationRepository
) {

    /**
     * Start the trigger monitor service.
     * The service will register broadcast receivers and monitor for trigger events.
     */
    fun startService() {
        TriggerMonitorService.start(context)
    }

    /**
     * Stop the trigger monitor service.
     */
    fun stopService() {
        TriggerMonitorService.stop(context)
    }

    /**
     * Check if there are any enabled rules that require the service to be running.
     * If there are enabled rules, the service should be started.
     */
    suspend fun shouldServiceBeRunning(): Boolean {
        return try {
            val enabledRules = repository.getEnabledRules().first()
            enabledRules.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Start or stop the service based on whether there are enabled rules.
     */
    suspend fun updateServiceState() {
        if (shouldServiceBeRunning()) {
            startService()
        } else {
            stopService()
        }
    }
}

