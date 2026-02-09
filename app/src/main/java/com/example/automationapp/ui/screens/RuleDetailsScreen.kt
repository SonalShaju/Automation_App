package com.example.automationapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.automationapp.ui.viewmodel.RuleDetailsViewModel
import com.example.automationapp.ui.viewmodel.RuleDetailsUiState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDetailsScreen(
    ruleId: Long,
    viewModel: RuleDetailsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToEditRule: (Long) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(ruleId) {
        viewModel.loadRuleDetails(ruleId)
    }

    // Handle snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rule Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Rule",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onNavigateToEditRule(ruleId) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(Icons.Default.Edit, contentDescription = "Edit Rule")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is RuleDetailsUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is RuleDetailsUiState.Success -> {
                    RuleDetailsContent(
                        details = state.details,
                        onToggleEnabled = { enabled ->
                            viewModel.toggleRuleEnabled(enabled)
                        }
                    )
                }
                is RuleDetailsUiState.Error -> {
                    ErrorContent(
                        message = state.message,
                        onRetry = { viewModel.loadRuleDetails(ruleId) }
                    )
                }
            }

            if (showDeleteDialog) {
                DeleteRuleDialog(
                    onDismiss = { showDeleteDialog = false },
                    onConfirm = {
                        viewModel.deleteRule()
                        showDeleteDialog = false
                        onNavigateBack()
                    }
                )
            }
        }
    }
}

@Composable
private fun RuleDetailsContent(
    details: com.example.automationapp.domain.model.RuleDetails,
    onToggleEnabled: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Rule Info Card
        item {
            RuleInfoCard(
                name = details.rule.name,
                description = details.rule.description,
                isEnabled = details.rule.isEnabled,
                executionCount = details.rule.executionCount,
                createdAt = details.rule.createdAt,
                updatedAt = details.rule.updatedAt,
                onToggleEnabled = onToggleEnabled
            )
        }

        // Triggers Section
        item {
            SectionTitle(
                title = "Triggers",
                subtitle = "${details.triggers.size} condition(s)",
                icon = Icons.Default.PlayArrow
            )
        }

        if (details.triggers.isEmpty()) {
            item {
                EmptyStateMessage("No triggers configured")
            }
        } else {
            items(details.triggers) { trigger ->
                TriggerDetailCard(trigger = trigger)
            }
        }

        // Actions Section
        item {
            Spacer(modifier = Modifier.height(8.dp))
            SectionTitle(
                title = "Actions",
                subtitle = "${details.actions.size} action(s)",
                icon = Icons.Default.FlashOn
            )
        }

        if (details.actions.isEmpty()) {
            item {
                EmptyStateMessage("No actions configured")
            }
        } else {
            items(details.actions) { action ->
                ActionDetailCard(action = action)
            }
        }
    }
}

@Composable
private fun RuleInfoCard(
    name: String,
    description: String,
    isEnabled: Boolean,
    executionCount: Int,
    createdAt: Long,
    updatedAt: Long,
    onToggleEnabled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = isEnabled,
                    onCheckedChange = onToggleEnabled
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Statistics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                StatItem(label = "Executions", value = executionCount.toString())
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Created",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDateTime(createdAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Updated",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatDateTime(updatedAt),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun SectionTitle(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TriggerDetailCard(
    trigger: com.example.automationapp.data.local.entity.Trigger
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getTriggerIcon(trigger.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getTriggerTypeName(trigger.type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Operator: ${trigger.logicalOperator.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!trigger.isActive) {
                    Text(
                        text = "Inactive",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionDetailCard(
    action: com.example.automationapp.data.local.entity.Action
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getActionIcon(action.type),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = getActionTypeName(action.type),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Sequence: ${action.sequence}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!action.isEnabled) {
                    Text(
                        text = "Disabled",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ErrorContent(
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Retry")
        }
    }
}

@Composable
private fun DeleteRuleDialog(
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
        title = { Text("Delete Rule") },
        text = {
            Text("Are you sure you want to delete this rule? This action cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

// Helper functions (reuse from CreateRuleScreen)
private fun getTriggerTypeName(type: com.example.automationapp.data.local.entity.TriggerType): String = when (type) {
    com.example.automationapp.data.local.entity.TriggerType.TIME_BASED -> "Time Based"
    com.example.automationapp.data.local.entity.TriggerType.TIME_RANGE -> "Time Range"
    com.example.automationapp.data.local.entity.TriggerType.LOCATION_BASED -> "Location Based"
    com.example.automationapp.data.local.entity.TriggerType.BATTERY_LEVEL -> "Battery Level"
    com.example.automationapp.data.local.entity.TriggerType.CHARGING_STATUS -> "Charging Status"
    com.example.automationapp.data.local.entity.TriggerType.WIFI_CONNECTED -> "WiFi Connected"
    com.example.automationapp.data.local.entity.TriggerType.BLUETOOTH_CONNECTED -> "Bluetooth Connected"
    com.example.automationapp.data.local.entity.TriggerType.HEADPHONES_CONNECTED -> "Headphones Connected"
    com.example.automationapp.data.local.entity.TriggerType.APP_OPENED -> "App Opened"
    com.example.automationapp.data.local.entity.TriggerType.AIRPLANE_MODE -> "Airplane Mode"
    com.example.automationapp.data.local.entity.TriggerType.DO_NOT_DISTURB -> "Do Not Disturb"
}

private fun getTriggerIcon(type: com.example.automationapp.data.local.entity.TriggerType) = when (type) {
    com.example.automationapp.data.local.entity.TriggerType.TIME_BASED -> Icons.Default.Schedule
    com.example.automationapp.data.local.entity.TriggerType.TIME_RANGE -> Icons.Default.DateRange
    com.example.automationapp.data.local.entity.TriggerType.LOCATION_BASED -> Icons.Default.LocationOn
    com.example.automationapp.data.local.entity.TriggerType.BATTERY_LEVEL -> Icons.Default.BatteryStd
    com.example.automationapp.data.local.entity.TriggerType.CHARGING_STATUS -> Icons.Default.Power
    com.example.automationapp.data.local.entity.TriggerType.WIFI_CONNECTED -> Icons.Default.Wifi
    com.example.automationapp.data.local.entity.TriggerType.BLUETOOTH_CONNECTED -> Icons.Default.Bluetooth
    com.example.automationapp.data.local.entity.TriggerType.HEADPHONES_CONNECTED -> Icons.Default.Headphones
    com.example.automationapp.data.local.entity.TriggerType.APP_OPENED -> Icons.Default.Apps
    com.example.automationapp.data.local.entity.TriggerType.AIRPLANE_MODE -> Icons.Default.AirplanemodeActive
    com.example.automationapp.data.local.entity.TriggerType.DO_NOT_DISTURB -> Icons.Default.DoNotDisturb
}

private fun getActionTypeName(type: com.example.automationapp.data.local.entity.ActionType): String = when (type) {
    com.example.automationapp.data.local.entity.ActionType.TOGGLE_SILENT_MODE -> "Toggle Silent"
    com.example.automationapp.data.local.entity.ActionType.ADJUST_VOLUME -> "Adjust Volume"
    com.example.automationapp.data.local.entity.ActionType.TOGGLE_VIBRATE -> "Toggle Vibrate"
    com.example.automationapp.data.local.entity.ActionType.LAUNCH_APP -> "Launch App"
    com.example.automationapp.data.local.entity.ActionType.BLOCK_APP -> "Block App"
    com.example.automationapp.data.local.entity.ActionType.SEND_NOTIFICATION -> "Send Notification"
    com.example.automationapp.data.local.entity.ActionType.ADJUST_BRIGHTNESS -> "Adjust Brightness"
    com.example.automationapp.data.local.entity.ActionType.ENABLE_DND -> "Enable DND"
    com.example.automationapp.data.local.entity.ActionType.DISABLE_DND -> "Disable DND"
    else -> type.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}

private fun getActionIcon(type: com.example.automationapp.data.local.entity.ActionType) = when (type) {
    com.example.automationapp.data.local.entity.ActionType.TOGGLE_SILENT_MODE -> Icons.Default.VolumeOff
    com.example.automationapp.data.local.entity.ActionType.ADJUST_VOLUME -> Icons.Default.VolumeUp
    com.example.automationapp.data.local.entity.ActionType.LAUNCH_APP -> Icons.Default.Launch
    com.example.automationapp.data.local.entity.ActionType.BLOCK_APP -> Icons.Default.Block
    com.example.automationapp.data.local.entity.ActionType.SEND_NOTIFICATION -> Icons.Default.Notifications
    com.example.automationapp.data.local.entity.ActionType.ADJUST_BRIGHTNESS -> Icons.Default.Brightness6
    com.example.automationapp.data.local.entity.ActionType.ENABLE_DND,
    com.example.automationapp.data.local.entity.ActionType.DISABLE_DND -> Icons.Default.DoNotDisturb
    else -> Icons.Default.Settings
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
