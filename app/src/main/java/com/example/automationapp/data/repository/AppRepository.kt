package com.example.automationapp.data.repository

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.example.automationapp.domain.model.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

// Lightweight repository retained for legacy callers; main flow uses AutomationRepository.
@Singleton
class AppRepository @Inject constructor(
    private val packageManager: PackageManager
) {
    suspend fun getInstalledApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        runCatching {
            packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
                .asSequence()
                .filter { app ->
                    val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                    val isUpdatedSystem = (app.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                    !isSystem || isUpdatedSystem
                }
                .map { info: ApplicationInfo ->
                    val appName = info.loadLabel(packageManager)?.toString().orEmpty().ifBlank { info.packageName }
                    AppInfo(name = appName, packageName = info.packageName)
                }
                .sortedBy { it.name.lowercase() }
                .toList()
        }.getOrDefault(emptyList())
    }
}
