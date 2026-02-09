package com.example.automationapp.domain.usecase

import com.example.automationapp.domain.model.AppInfo
import com.example.automationapp.domain.repository.AutomationRepository
import javax.inject.Inject

class GetInstalledUserAppsUseCase @Inject constructor(
    private val repository: AutomationRepository
) {
    suspend operator fun invoke(): List<AppInfo> = repository.getInstalledUserApps()
}

