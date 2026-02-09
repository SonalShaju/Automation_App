package com.example.automationapp.ui.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.data.local.entity.Action
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.data.local.entity.Trigger
import com.example.automationapp.domain.model.AppInfo
import com.example.automationapp.domain.repository.AutomationRepository
import com.example.automationapp.domain.usecase.CreateRuleUseCase
import com.example.automationapp.domain.usecase.UpdateRuleUseCase
import com.example.automationapp.domain.usecase.ValidateRuleUseCase
import com.example.automationapp.domain.usecase.ValidationResult
import com.example.automationapp.util.PermissionHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateRuleViewModel @Inject constructor(
    private val createRuleUseCase: CreateRuleUseCase,
    private val updateRuleUseCase: UpdateRuleUseCase,
    private val validateRuleUseCase: ValidateRuleUseCase,
    private val permissionHelper: PermissionHelper,
    private val repository: AutomationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateRuleUiState())
    val uiState: StateFlow<CreateRuleUiState> = _uiState.asStateFlow()

    private val _navigationEvent = MutableSharedFlow<NavigationEvent>()
    val navigationEvent: SharedFlow<NavigationEvent> = _navigationEvent.asSharedFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private val _permissionRequest = MutableSharedFlow<PermissionRequestEvent>()
    val permissionRequest: SharedFlow<PermissionRequestEvent> = _permissionRequest.asSharedFlow()

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    // Real-time validation
    val isFormValid: StateFlow<Boolean> = uiState.map { state ->
        state.ruleName.isNotBlank() &&
                state.triggers.isNotEmpty() &&
                state.actions.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Track if we're editing an existing rule
    private var editingRuleId: Long? = null

    /**
     * Load an existing rule for editing
     */
    fun loadRule(ruleId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val ruleDetails = repository.getRuleDetails(ruleId)
                if (ruleDetails != null) {
                    editingRuleId = ruleId
                    _uiState.value = _uiState.value.copy(
                        ruleName = ruleDetails.rule.name,
                        description = ruleDetails.rule.description,
                        triggers = ruleDetails.triggers,
                        actions = ruleDetails.actions,
                        isLoading = false,
                        error = null
                    )
                    updatePermissionWarnings()
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Rule not found"
                    )
                    _snackbarMessage.emit("Rule not found")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load rule"
                )
                _snackbarMessage.emit(e.message ?: "Failed to load rule")
            }
        }
    }

    /**
     * Check permissions for a trigger type before adding it
     */
    fun checkTriggerPermissions(trigger: Trigger): PermissionHelper.PermissionCheckResult {
        return permissionHelper.checkPermissionsForTrigger(trigger.type)
    }

    /**
     * Check permissions for an action type before adding it
     */
    fun checkActionPermissions(action: Action): PermissionHelper.PermissionCheckResult {
        return permissionHelper.checkPermissionsForAction(action.type)
    }

    /**
     * Get all missing permissions for the current rule configuration
     */
    fun getMissingPermissions(): PermissionHelper.PermissionCheckResult {
        val triggerTypes = _uiState.value.triggers.map { it.type }
        val actionTypes = _uiState.value.actions.map { it.type }

        val triggerResult = permissionHelper.checkPermissionsForTriggers(triggerTypes)
        val actionResult = permissionHelper.checkPermissionsForActions(actionTypes)

        val allMissingRegular = (triggerResult.missingPermissions + actionResult.missingPermissions)
            .distinctBy { it.permission }
        val allMissingSpecial = (triggerResult.missingSpecialPermissions + actionResult.missingSpecialPermissions)
            .distinctBy { it.permission }

        return PermissionHelper.PermissionCheckResult(
            hasAllPermissions = allMissingRegular.isEmpty() && allMissingSpecial.isEmpty(),
            missingPermissions = allMissingRegular,
            missingSpecialPermissions = allMissingSpecial
        )
    }

    /**
     * Get runtime permissions that need to be requested
     */
    fun getRuntimePermissionsToRequest(): List<String> {
        val triggerTypes = _uiState.value.triggers.map { it.type }
        val actionTypes = _uiState.value.actions.map { it.type }
        return permissionHelper.getRuntimePermissionsForRule(triggerTypes, actionTypes)
    }

    fun updateRuleName(name: String) {
        _uiState.value = _uiState.value.copy(
            ruleName = name,
            nameError = null
        )
    }

    fun updateDescription(description: String) {
        _uiState.value = _uiState.value.copy(
            description = description,
            descriptionError = null
        )
    }

    fun addTrigger(trigger: Trigger) {
        val currentTriggers = _uiState.value.triggers.toMutableList()
        currentTriggers.add(trigger)

        // Check permissions for the trigger
        val permissionCheck = checkTriggerPermissions(trigger)
        val permissionWarning = if (!permissionCheck.hasAllPermissions) {
            val missing = permissionCheck.allMissing.joinToString(", ") { it.displayName }
            "Trigger requires permissions: $missing"
        } else null

        _uiState.value = _uiState.value.copy(
            triggers = currentTriggers,
            triggersError = null,
            permissionWarning = permissionWarning ?: _uiState.value.permissionWarning
        )

        // Request permissions if needed
        if (permissionCheck.missingPermissions.isNotEmpty()) {
            viewModelScope.launch {
                _permissionRequest.emit(
                    PermissionRequestEvent.RequestRuntimePermissions(
                        permissionCheck.missingPermissions.map { it.permission }
                    )
                )
            }
        }
    }

    fun updateTrigger(index: Int, trigger: Trigger) {
        val currentTriggers = _uiState.value.triggers.toMutableList()
        if (index in currentTriggers.indices) {
            currentTriggers[index] = trigger
            _uiState.value = _uiState.value.copy(triggers = currentTriggers)
            updatePermissionWarnings()
        }
    }

    fun removeTrigger(index: Int) {
        val currentTriggers = _uiState.value.triggers.toMutableList()
        if (index in currentTriggers.indices) {
            currentTriggers.removeAt(index)
            _uiState.value = _uiState.value.copy(triggers = currentTriggers)
            updatePermissionWarnings()
        }
    }

    fun addAction(action: Action) {
        val currentActions = _uiState.value.actions.toMutableList()
        currentActions.add(action)

        // Check permissions for the action
        val permissionCheck = checkActionPermissions(action)
        val permissionWarning = if (!permissionCheck.hasAllPermissions) {
            val missing = permissionCheck.allMissing.joinToString(", ") { it.displayName }
            "Action requires permissions: $missing"
        } else null

        _uiState.value = _uiState.value.copy(
            actions = currentActions,
            actionsError = null,
            permissionWarning = permissionWarning ?: _uiState.value.permissionWarning
        )

        // Request permissions if needed
        if (permissionCheck.missingPermissions.isNotEmpty()) {
            viewModelScope.launch {
                _permissionRequest.emit(
                    PermissionRequestEvent.RequestRuntimePermissions(
                        permissionCheck.missingPermissions.map { it.permission }
                    )
                )
            }
        }
    }

    fun updateAction(index: Int, action: Action) {
        val currentActions = _uiState.value.actions.toMutableList()
        if (index in currentActions.indices) {
            currentActions[index] = action
            _uiState.value = _uiState.value.copy(actions = currentActions)
            updatePermissionWarnings()
        }
    }

    private fun updatePermissionWarnings() {
        val missingPermissions = getMissingPermissions()
        val warning = if (!missingPermissions.hasAllPermissions) {
            val missing = missingPermissions.allMissing.joinToString(", ") { it.displayName }
            "Rule requires permissions: $missing"
        } else null
        _uiState.value = _uiState.value.copy(permissionWarning = warning)
    }

    fun removeAction(index: Int) {
        val currentActions = _uiState.value.actions.toMutableList()
        if (index in currentActions.indices) {
            currentActions.removeAt(index)
            _uiState.value = _uiState.value.copy(actions = currentActions)
        }
    }

    fun reorderActions(fromIndex: Int, toIndex: Int) {
        val currentActions = _uiState.value.actions.toMutableList()
        if (fromIndex in currentActions.indices && toIndex in currentActions.indices) {
            val action = currentActions.removeAt(fromIndex)
            currentActions.add(toIndex, action)
            _uiState.value = _uiState.value.copy(actions = currentActions)
        }
    }

    fun validateForm(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validate name
        if (state.ruleName.isBlank()) {
            _uiState.value = state.copy(nameError = "Rule name cannot be empty")
            isValid = false
        } else if (state.ruleName.length < 3) {
            _uiState.value = state.copy(nameError = "Rule name must be at least 3 characters")
            isValid = false
        }

        // Validate description
        if (state.description.length > 200) {
            _uiState.value = state.copy(descriptionError = "Description must not exceed 200 characters")
            isValid = false
        }

        // Validate triggers
        if (state.triggers.isEmpty()) {
            _uiState.value = state.copy(triggersError = "At least one trigger is required")
            isValid = false
        }

        // Validate actions
        if (state.actions.isEmpty()) {
            _uiState.value = state.copy(actionsError = "At least one action is required")
            isValid = false
        }

        return isValid
    }

    /**
     * Save the rule - creates new or updates existing based on editingRuleId
     */
    fun saveRule() {
        if (!validateForm()) {
            viewModelScope.launch {
                _snackbarMessage.emit("Please fix validation errors")
            }
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // Optional: Validate with use case
                val validationResult = validateRuleUseCase(
                    name = _uiState.value.ruleName,
                    description = _uiState.value.description,
                    triggers = _uiState.value.triggers,
                    actions = _uiState.value.actions
                )

                if (validationResult.isFailure()) {
                    val errorMessage = validationResult.getErrorMessage()
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = errorMessage
                    )
                    _snackbarMessage.emit("Validation failed: $errorMessage")
                    return@launch
                }

                val ruleId = editingRuleId
                if (ruleId != null && ruleId > 0) {
                    // Update existing rule
                    updateExistingRule(ruleId)
                } else {
                    // Create new rule
                    createNewRule()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
                _snackbarMessage.emit(e.message ?: "Failed to save rule")
            }
        }
    }

    private suspend fun createNewRule() {
        val result = createRuleUseCase(
            name = _uiState.value.ruleName,
            description = _uiState.value.description,
            triggers = _uiState.value.triggers,
            actions = _uiState.value.actions,
            exitActionType = _uiState.value.exitActionType,
            exitActionParams = _uiState.value.exitActionParams
        )

        _uiState.value = _uiState.value.copy(isLoading = false)

        result.fold(
            onSuccess = { ruleId ->
                _snackbarMessage.emit("Rule created successfully")
                _navigationEvent.emit(NavigationEvent.NavigateBack(ruleId))
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to create rule"
                _uiState.value = _uiState.value.copy(error = errorMessage)
                _snackbarMessage.emit(errorMessage)
            }
        )
    }

    private suspend fun updateExistingRule(ruleId: Long) {
        // Use UpdateRuleUseCase for full synchronization (cancel old triggers, update DB, reschedule)
        val result = updateRuleUseCase(
            ruleId = ruleId,
            name = _uiState.value.ruleName,
            description = _uiState.value.description,
            triggers = _uiState.value.triggers,
            actions = _uiState.value.actions,
            isEnabled = true
        )

        _uiState.value = _uiState.value.copy(isLoading = false)

        result.fold(
            onSuccess = { updatedRuleId ->
                _snackbarMessage.emit("Rule updated successfully")
                _navigationEvent.emit(NavigationEvent.NavigateBack(updatedRuleId))
            },
            onFailure = { error ->
                val errorMessage = error.message ?: "Failed to update rule"
                _uiState.value = _uiState.value.copy(error = errorMessage)
                _snackbarMessage.emit(errorMessage)
            }
        )
    }

    /**
     * @deprecated Use saveRule() instead
     */
    fun createRule() {
        saveRule()
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun resetForm() {
        _uiState.value = CreateRuleUiState()
    }

    /**
     * Update the exit action for Time Range triggers
     * This action will execute when the time range ends
     */
    fun updateExitAction(actionType: com.example.automationapp.data.local.entity.ActionType?, params: String?) {
        _uiState.value = _uiState.value.copy(
            exitActionType = actionType,
            exitActionParams = params
        )
    }

    /**
     * Check if the current rule has a Time Range trigger (for showing exit action UI)
     */
    fun hasTimeRangeTrigger(): Boolean {
        return _uiState.value.triggers.any { it.type == com.example.automationapp.data.local.entity.TriggerType.TIME_RANGE }
    }

    fun loadInstalledApps() {
        viewModelScope.launch {
            _installedApps.value = repository.getInstalledUserApps()
        }
    }
}

data class CreateRuleUiState(
    val ruleName: String = "",
    val description: String = "",
    val triggers: List<Trigger> = emptyList(),
    val actions: List<Action> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val nameError: String? = null,
    val descriptionError: String? = null,
    val triggersError: String? = null,
    val actionsError: String? = null,
    val permissionWarning: String? = null,
    // Exit Action for Time Range triggers
    val exitActionType: com.example.automationapp.data.local.entity.ActionType? = null,
    val exitActionParams: String? = null
)

sealed class NavigationEvent {
    data class NavigateBack(val ruleId: Long) : NavigationEvent()
}

/**
 * Event for requesting permissions from the UI
 */
sealed class PermissionRequestEvent {
    data class RequestRuntimePermissions(val permissions: List<String>) : PermissionRequestEvent()
    data class RequestSpecialPermission(val permissionInfo: PermissionHelper.PermissionInfo) : PermissionRequestEvent()
}
