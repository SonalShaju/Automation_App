package com.example.automationapp.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.automationapp.domain.model.AppInfo
import com.example.automationapp.domain.repository.AutomationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppListViewModel @Inject constructor(
    private val repository: AutomationRepository
) : ViewModel() {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _isLoading.value = true
            _apps.value = repository.getInstalledUserApps()
            _isLoading.value = false
        }
    }
}

