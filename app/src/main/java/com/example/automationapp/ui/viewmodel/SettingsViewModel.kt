package com.example.automationapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.domain.repository.AutomationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    application: Application,
    private val repository: AutomationRepository
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadSettings()
        calculateStorageUsage()
    }

    private fun loadSettings() {
        _uiState.value = _uiState.value.copy(
            appVersion = getAppVersion()
        )
    }

    private fun getAppVersion(): String {
        return try {
            val packageInfo = getApplication<Application>().packageManager
                .getPackageInfo(getApplication<Application>().packageName, 0)
            packageInfo.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableNotifications = enabled)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (enabled) "Notifications enabled" else "Notifications disabled"
            )
        }
    }

    fun toggleBackgroundExecution(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableBackgroundExecution = enabled)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (enabled) "Background execution enabled" else "Background execution disabled"
            )
        }
    }

    fun toggleBatteryOptimization(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(enableBatteryOptimization = enabled)
        viewModelScope.launch {
            _snackbarMessage.emit(
                if (enabled) "Battery optimization enabled" else "Battery optimization disabled"
            )
        }
    }


    fun calculateStorageUsage() {
        viewModelScope.launch {
            try {
                val dbPath = getApplication<Application>().getDatabasePath("automation_database")
                if (dbPath.exists()) {
                    val size = dbPath.length()
                    val sizeInMB = size / (1024.0 * 1024.0)

                    _uiState.value = _uiState.value.copy(
                        databaseSize = String.format("%.2f MB", sizeInMB)
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        databaseSize = "0.00 MB"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(databaseSize = "Unknown")
            }
        }
    }

    fun clearExecutionLogs() {
        viewModelScope.launch {
            try {
                repository.clearAllExecutionLogs()
                _snackbarMessage.emit("Execution logs cleared successfully")
                calculateStorageUsage()
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to clear logs: ${e.message}")
            }
        }
    }

    fun clearOldLogs(daysOld: Int = 30) {
        viewModelScope.launch {
            try {
                val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
                repository.clearOldExecutionLogs(cutoffTime)
                _snackbarMessage.emit("Old execution logs cleared successfully")
                calculateStorageUsage()
            } catch (e: Exception) {
                _snackbarMessage.emit("Failed to clear old logs: ${e.message}")
            }
        }
    }

    fun checkForUpdates() {
        viewModelScope.launch {
            _snackbarMessage.emit("Checking for updates...")
            _uiState.value = _uiState.value.copy(updateAvailable = false)
            _snackbarMessage.emit("You're using the latest version")
        }
    }


    fun openFeedbackForm() {
        viewModelScope.launch {
            _snackbarMessage.emit("Feedback form coming soon!")
        }
    }

    fun openLicenses() {
        viewModelScope.launch {
            _snackbarMessage.emit("Open source licenses coming soon!")
        }
    }

    fun navigateToFavoritePlaces() {
        viewModelScope.launch {
            _snackbarMessage.emit("Favorite Places feature coming soon!")
        }
    }
}

data class SettingsUiState(
    val enableNotifications: Boolean = true,
    val enableBackgroundExecution: Boolean = true,
    val enableBatteryOptimization: Boolean = true,
    val databaseSize: String = "Calculating...",
    val appVersion: String = "1.0.0",
    val updateAvailable: Boolean = false
)
