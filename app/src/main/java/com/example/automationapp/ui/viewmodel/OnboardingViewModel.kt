package com.example.automationapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.data.preferences.UserPreferencesRepository
import com.example.automationapp.util.PermissionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Onboarding screen
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val permissionManager: PermissionManager
) : ViewModel() {

    /**
     * Flow indicating if setup is complete
     */
    val isSetupComplete: StateFlow<Boolean> = userPreferencesRepository.isSetupComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    /**
     * Permission states - will be refreshed on resume
     */
    private val _permissionStates = MutableStateFlow(PermissionStates())
    val permissionStates: StateFlow<PermissionStates> = _permissionStates.asStateFlow()

    /**
     * Check if critical permissions are granted (required to proceed)
     */
    val canProceed: StateFlow<Boolean> = _permissionStates.map { states ->
        states.hasAccessibilityPermission && states.hasNotificationListenerPermission
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    init {
        refreshPermissionStates()
    }

    /**
     * Refresh all permission states - call this on resume
     */
    fun refreshPermissionStates() {
        _permissionStates.value = PermissionStates(
            hasNotificationListenerPermission = permissionManager.hasNotificationListenerPermission(),
            hasWriteSettingsPermission = permissionManager.hasWriteSettingsPermission(),
            hasAccessibilityPermission = permissionManager.hasAccessibilityPermission(),
            hasBatteryOptimizationExemption = hasBatteryOptimizationExemption(),
            hasLocationPermission = permissionManager.hasLocationPermission(),
            hasDndPolicyPermission = permissionManager.hasDndPolicyPermission()
        )
    }

    /**
     * Check if app is exempt from battery optimization
     */
    private fun hasBatteryOptimizationExemption(): Boolean {
        return try {
            permissionManager.hasExactAlarmPermission() // This often correlates with battery exemption
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Mark onboarding as complete
     */
    fun completeOnboarding() {
        viewModelScope.launch {
            userPreferencesRepository.completeSetup()
        }
    }

    /**
     * Get intent for specific permission
     */
    fun getPermissionIntent(permission: PermissionManager.SpecialPermission) =
        permissionManager.getIntentForSpecialPermission(permission)

    /**
     * Get intent for battery optimization settings
     */
    fun getBatteryOptimizationIntent() = permissionManager.getAppSettingsIntent()
}

/**
 * Data class holding all permission states
 */
data class PermissionStates(
    val hasNotificationListenerPermission: Boolean = false,
    val hasWriteSettingsPermission: Boolean = false,
    val hasAccessibilityPermission: Boolean = false,
    val hasBatteryOptimizationExemption: Boolean = false,
    val hasLocationPermission: Boolean = false,
    val hasDndPolicyPermission: Boolean = false
)

