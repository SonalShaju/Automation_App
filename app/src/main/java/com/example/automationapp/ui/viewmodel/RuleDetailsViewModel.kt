package com.example.automationapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.domain.model.RuleDetails
import com.example.automationapp.domain.usecase.DeleteRuleUseCase
import com.example.automationapp.domain.usecase.GetRuleDetailsUseCase
import com.example.automationapp.domain.usecase.ToggleRuleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RuleDetailsViewModel @Inject constructor(
    private val getRuleDetailsUseCase: GetRuleDetailsUseCase,
    private val toggleRuleUseCase: ToggleRuleUseCase,
    private val deleteRuleUseCase: DeleteRuleUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<RuleDetailsUiState>(RuleDetailsUiState.Loading)
    val uiState: StateFlow<RuleDetailsUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    private var currentRuleId: Long = 0L

    fun loadRuleDetails(ruleId: Long) {
        currentRuleId = ruleId
        viewModelScope.launch {
            _uiState.value = RuleDetailsUiState.Loading

            val result = getRuleDetailsUseCase(ruleId)

            result.fold(
                onSuccess = { details ->
                    _uiState.value = RuleDetailsUiState.Success(details)
                },
                onFailure = { error ->
                    _uiState.value = RuleDetailsUiState.Error(
                        error.message ?: "Failed to load rule details"
                    )
                }
            )
        }
    }

    fun toggleRuleEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val result = toggleRuleUseCase(currentRuleId, enabled)

            result.fold(
                onSuccess = {
                    _snackbarMessage.emit(if (enabled) "Rule enabled" else "Rule disabled")
                    loadRuleDetails(currentRuleId)
                },
                onFailure = { error ->
                    _snackbarMessage.emit(
                        error.message ?: "Failed to toggle rule"
                    )
                }
            )
        }
    }

    fun deleteRule() {
        viewModelScope.launch {
            val result = deleteRuleUseCase(currentRuleId)

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
}

sealed class RuleDetailsUiState {
    object Loading : RuleDetailsUiState()
    data class Success(val details: RuleDetails) : RuleDetailsUiState()
    data class Error(val message: String) : RuleDetailsUiState()
}
