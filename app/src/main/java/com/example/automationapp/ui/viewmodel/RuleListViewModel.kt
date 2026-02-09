package com.example.automationapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.data.local.entity.AutomationRule
import com.example.automationapp.domain.usecase.DeleteRuleUseCase
import com.example.automationapp.domain.usecase.GetAllRulesUseCase
import com.example.automationapp.domain.usecase.ToggleRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleListViewModel @Inject constructor(
    private val getAllRulesUseCase: GetAllRulesUseCase,
    private val toggleRuleUseCase: ToggleRuleUseCase,
    private val deleteRuleUseCase: DeleteRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RuleListUiState>(RuleListUiState.Loading)
    val uiState: StateFlow<RuleListUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    init {
        loadRules()
    }

    fun loadRules() {
        viewModelScope.launch {
            _uiState.value = RuleListUiState.Loading

            getAllRulesUseCase()
                .catch { e ->
                    _uiState.value = RuleListUiState.Error(
                        e.message ?: "Failed to load rules"
                    )
                }
                .collect { rules ->
                    _uiState.value = if (rules.isEmpty()) {
                        RuleListUiState.Empty
                    } else {
                        RuleListUiState.Success(rules)
                    }
                }
        }
    }

    fun toggleRuleEnabled(ruleId: Long, enabled: Boolean) {
        viewModelScope.launch {
            val result = toggleRuleUseCase(ruleId, enabled)

            result.fold(
                onSuccess = {
                    _snackbarMessage.emit(
                        if (enabled) "Rule enabled" else "Rule disabled"
                    )
                },
                onFailure = { error ->
                    _snackbarMessage.emit(
                        error.message ?: "Failed to toggle rule"
                    )
                    // Reload rules to revert UI state
                    loadRules()
                }
            )
        }
    }

    fun deleteRule(ruleId: Long) {
        viewModelScope.launch {
            val result = deleteRuleUseCase(ruleId)

            result.fold(
                onSuccess = {
                    _snackbarMessage.emit("Rule deleted successfully")
                },
                onFailure = { error ->
                    _snackbarMessage.emit(
                        error.message ?: "Failed to delete rule"
                    )
                }
            )
        }
    }

    fun retryLoading() {
        loadRules()
    }
}

sealed class RuleListUiState {
    object Loading : RuleListUiState()
    object Empty : RuleListUiState()
    data class Success(val rules: List<AutomationRule>) : RuleListUiState()
    data class Error(val message: String) : RuleListUiState()
}
