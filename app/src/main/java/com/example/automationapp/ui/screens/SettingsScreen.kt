package com.example.automationapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.automationapp.ui.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAboutDialog by remember { mutableStateOf(false) }
    var showClearDataDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    // Handle snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // General Settings Section
            item {
                SettingsSectionHeader(title = "General")
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.Notifications,
                    title = "Enable Notifications",
                    subtitle = "Show notifications when rules execute",
                    checked = uiState.enableNotifications,
                    onCheckedChange = viewModel::toggleNotifications
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.PlayArrow,
                    title = "Background Execution",
                    subtitle = "Allow rules to execute in background",
                    checked = uiState.enableBackgroundExecution,
                    onCheckedChange = viewModel::toggleBackgroundExecution
                )
            }

            item {
                SettingsSwitchItem(
                    icon = Icons.Default.BatteryChargingFull,
                    title = "Battery Optimization",
                    subtitle = "Optimize for battery life",
                    checked = uiState.enableBatteryOptimization,
                    onCheckedChange = viewModel::toggleBatteryOptimization
                )
            }


            // Data & Privacy Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Data & Privacy")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Storage,
                    title = "Storage Usage",
                    subtitle = "Database: ${uiState.databaseSize}",
                    onClick = { viewModel.calculateStorageUsage() }
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.DeleteForever,
                    title = "Clear Execution Logs",
                    subtitle = "Delete all execution history",
                    onClick = { showClearDataDialog = true },
                    isDestructive = true
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Security,
                    title = "Privacy Policy",
                    subtitle = "View our privacy policy",
                    onClick = { showPrivacyDialog = true }
                )
            }

            // Location Settings Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "Location")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.FavoriteBorder,
                    title = "Favorite Places",
                    subtitle = "Manage saved locations like Home, Work",
                    onClick = { viewModel.navigateToFavoritePlaces() }
                )
            }

            // About Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                SettingsSectionHeader(title = "About")
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Info,
                    title = "About App",
                    subtitle = "Version ${uiState.appVersion}",
                    onClick = { showAboutDialog = true }
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.BugReport,
                    title = "Report Issue",
                    subtitle = "Help us improve the app",
                    onClick = { viewModel.openFeedbackForm() }
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Update,
                    title = "Check for Updates",
                    subtitle = if (uiState.updateAvailable) "Update available" else "You're up to date",
                    onClick = { viewModel.checkForUpdates() }
                )
            }

            item {
                SettingsClickableItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    subtitle = "View third-party licenses",
                    onClick = { viewModel.openLicenses() }
                )
            }
        }

        // About Dialog
        if (showAboutDialog) {
            AboutDialog(
                appVersion = uiState.appVersion,
                onDismiss = { showAboutDialog = false }
            )
        }

        // Clear Data Dialog
        if (showClearDataDialog) {
            ClearDataDialog(
                onDismiss = { showClearDataDialog = false },
                onConfirm = {
                    viewModel.clearExecutionLogs()
                    showClearDataDialog = false
                }
            )
        }

        // Privacy Policy Dialog
        if (showPrivacyDialog) {
            PrivacyPolicyDialog(
                onDismiss = { showPrivacyDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingsSwitchItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun SettingsClickableItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isDestructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = if (isDestructive) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AboutDialog(
    appVersion: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("About Smart Automation Assistant") },
        text = {
            Column {
                Text(
                    text = "Version: $appVersion",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Smart Automation Assistant automates routine smartphone tasks based on triggers and conditions. Create powerful automation rules to enhance your productivity.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Developed as part of an engineering project.",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "© 2025 Smart Automation Team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Composable
private fun ClearDataDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Clear Execution Logs") },
        text = {
            Text("Are you sure you want to delete all execution logs? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Clear")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun PrivacyPolicyDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.Security,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = { Text("Privacy Policy") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
            ) {
                Text(
                    text = "Data Collection",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "This app stores automation rules, triggers, and actions locally on your device. No data is sent to external servers.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Permissions",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "The app requires various permissions to execute automation actions. All permissions are used solely for automation functionality.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "AI Features",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "When using AI features, rule data may be sent to Google's Gemini API for processing. No personal information is shared.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
