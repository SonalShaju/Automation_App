package com.example.automationapp.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.automationapp.data.local.entity.*
import com.example.automationapp.ui.components.AppSelectionDropdown
import com.example.automationapp.ui.components.BluetoothDeviceSelector
import com.example.automationapp.ui.components.DaySelector
import com.example.automationapp.ui.components.SpecialPermissionDialog
import com.example.automationapp.ui.components.WifiNetworkSelector
import com.example.automationapp.ui.viewmodel.CreateRuleViewModel
import com.example.automationapp.ui.viewmodel.NavigationEvent
import com.example.automationapp.ui.viewmodel.PermissionRequestEvent
import com.example.automationapp.util.PermissionManager
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Generates a human-readable summary of the rule based on triggers and actions.
 */
private fun generateRuleSummary(
    triggers: List<Trigger>,
    actions: List<Action>
): String {
    if (triggers.isEmpty() && actions.isEmpty()) {
        return "Add triggers and actions to define your automation rule."
    }

    val triggerText = if (triggers.isEmpty()) {
        "No trigger defined"
    } else {
        val triggerDescriptions = triggers.map { trigger ->
            getTriggerDescription(trigger.type, trigger.parameters)
        }
        when (triggerDescriptions.size) {
            1 -> triggerDescriptions.first()
            2 -> "${triggerDescriptions[0]} or ${triggerDescriptions[1]}"
            else -> triggerDescriptions.dropLast(1).joinToString(", ") + ", or ${triggerDescriptions.last()}"
        }
    }

    val actionText = if (actions.isEmpty()) {
        "no action will be performed"
    } else {
        val actionDescriptions = actions.map { action ->
            getActionDescription(action.type, action.parameters)
        }
        when (actionDescriptions.size) {
            1 -> actionDescriptions.first()
            2 -> "${actionDescriptions[0]} and ${actionDescriptions[1]}"
            else -> actionDescriptions.dropLast(1).joinToString(", ") + ", and ${actionDescriptions.last()}"
        }
    }

    return if (triggers.isEmpty()) {
        "When triggered, $actionText."
    } else if (actions.isEmpty()) {
        "When $triggerText, no actions defined yet."
    } else {
        "When $triggerText, then $actionText."
    }
}

private fun getTriggerDescription(type: TriggerType, parameters: String): String {
    return when (type) {
        TriggerType.TIME_BASED -> {
            val time = extractJsonValue(parameters, "time") ?: "specified time"
            "it's $time"
        }
        TriggerType.TIME_RANGE -> {
            val startTime = extractJsonValue(parameters, "start_time") ?: "00:00"
            val endTime = extractJsonValue(parameters, "end_time") ?: "23:59"
            "time is between $startTime and $endTime"
        }
        TriggerType.LOCATION_BASED -> "you arrive at a location"
        TriggerType.BATTERY_LEVEL -> {
            val level = extractJsonValue(parameters, "level") ?: "specified"
            val operator = extractJsonValue(parameters, "operator") ?: "equals"
            val opText = when (operator) {
                "less_than" -> "below"
                "greater_than" -> "above"
                else -> "at"
            }
            "battery is $opText $level%"
        }
        TriggerType.CHARGING_STATUS -> {
            val charging = extractJsonValue(parameters, "charging")?.toBoolean() ?: true
            if (charging) "device starts charging" else "device stops charging"
        }
        TriggerType.WIFI_CONNECTED -> {
            val ssid = extractJsonValue(parameters, "ssid")
            if (ssid.isNullOrBlank()) "WiFi connects" else "WiFi connects to '$ssid'"
        }
        TriggerType.BLUETOOTH_CONNECTED -> "Bluetooth is enabled"
        TriggerType.AIRPLANE_MODE -> {
            val enabled = extractJsonValue(parameters, "enabled")?.toBoolean() ?: true
            if (enabled) "airplane mode is enabled" else "airplane mode is disabled"
        }
        TriggerType.HEADPHONES_CONNECTED -> {
            val connected = extractJsonValue(parameters, "connected")?.toBoolean() ?: true
            if (connected) "headphones are connected" else "headphones are disconnected"
        }
        TriggerType.DO_NOT_DISTURB -> {
            val targetState = extractJsonValue(parameters, "target_state") ?: "ON"
            if (targetState == "ON") "Do Not Disturb is turned ON" else "Do Not Disturb is turned OFF"
        }
        TriggerType.APP_OPENED -> {
            val packageName = extractJsonValue(parameters, "package_name") ?: "an app"
            "app '$packageName' is opened"
        }
    }
}

private fun getActionDescription(type: ActionType, parameters: String): String {
    return when (type) {
        ActionType.TOGGLE_FLASHLIGHT -> "toggle flashlight"
        ActionType.ENABLE_FLASHLIGHT -> "turn on flashlight"
        ActionType.DISABLE_FLASHLIGHT -> "turn off flashlight"
        ActionType.VIBRATE -> "vibrate the device"
        ActionType.ADJUST_VOLUME -> {
            val level = extractJsonValue(parameters, "volume_level") ?: "specified"
            "set volume to $level%"
        }
        ActionType.SET_RINGER_MODE -> {
            val mode = extractJsonValue(parameters, "ringer_mode") ?: "normal"
            "set ringer to $mode mode"
        }
        ActionType.TOGGLE_SILENT_MODE -> "toggle silent mode"
        ActionType.TOGGLE_VIBRATE -> "toggle vibrate"
        ActionType.ADJUST_BRIGHTNESS -> {
            val level = extractJsonValue(parameters, "brightness_level") ?: "specified"
            "set brightness to $level%"
        }
        ActionType.TOGGLE_AUTO_BRIGHTNESS -> "toggle auto-brightness"
        ActionType.TOGGLE_AUTO_ROTATE -> "toggle auto-rotate"
        ActionType.GLOBAL_ACTION_LOCK_SCREEN -> "lock the screen"
        ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT -> "take a screenshot"
        ActionType.GLOBAL_ACTION_POWER_DIALOG -> "show power dialog"
        ActionType.LAUNCH_APP -> {
            val packageName = extractJsonValue(parameters, "package_name") ?: "an app"
            "launch '$packageName'"
        }
        ActionType.BLOCK_APP -> {
            val packageName = extractJsonValue(parameters, "packageName") ?: "an app"
            "block '$packageName'"
        }
        ActionType.SEND_NOTIFICATION -> {
            val title = extractJsonValue(parameters, "title") ?: "notification"
            "send a notification"
        }
        ActionType.CLEAR_NOTIFICATIONS -> "clear notifications"
        ActionType.ENABLE_DND -> "enable Do Not Disturb"
        ActionType.DISABLE_DND -> "disable Do Not Disturb"
    }
}

/**
 * Simple JSON value extractor for parameters.
 */
private fun extractJsonValue(json: String, key: String): String? {
    return try {
        val pattern = """"$key"\s*:\s*"?([^",}]+)"?""".toRegex()
        pattern.find(json)?.groupValues?.getOrNull(1)?.trim()
    } catch (e: Exception) {
        null
    }
}

@Composable
private fun RuleSummaryCard(
    triggers: List<Trigger>,
    actions: List<Action>,
    modifier: Modifier = Modifier
) {
    val summary = remember(triggers, actions) {
        generateRuleSummary(triggers, actions)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.padding(end = 12.dp, top = 2.dp)
            )
            Column {
                Text(
                    text = "Rule Summary",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleScreen(
    ruleId: Long? = null,
    viewModel: CreateRuleViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()
    val isEditMode = ruleId != null
    val context = LocalContext.current

    // Load rule data if editing
    LaunchedEffect(ruleId) {
        if (ruleId != null && ruleId > 0) {
            viewModel.loadRule(ruleId)
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showTriggerDialog by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf(false) }
    var showPermissionRationaleDialog by remember { mutableStateOf(false) }
    var pendingPermissions by remember { mutableStateOf<List<String>>(emptyList()) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys.toList()
        if (deniedPermissions.isNotEmpty()) {
            // Show snackbar about denied permissions
            kotlinx.coroutines.MainScope().launch {
                snackbarHostState.showSnackbar(
                    "Some permissions were denied. Features may not work correctly.",
                    actionLabel = "Settings"
                ).let { result ->
                    if (result == SnackbarResult.ActionPerformed) {
                        // Open app settings
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    // Handle navigation events
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is NavigationEvent.NavigateBack -> onNavigateBack()
                else -> ""
            }

        }
    }

    // Handle snackbar messages
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle permission requests
    LaunchedEffect(Unit) {
        viewModel.permissionRequest.collect { event ->
            when (event) {
                is PermissionRequestEvent.RequestRuntimePermissions -> {
                    pendingPermissions = event.permissions
                    permissionLauncher.launch(event.permissions.toTypedArray())
                }
                is PermissionRequestEvent.RequestSpecialPermission -> {
                    // Handle special permissions (WRITE_SETTINGS, etc.)
                    when (event.permissionInfo.permission) {
                        Manifest.permission.WRITE_SETTINGS -> {
                            val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                        Manifest.permission.ACCESS_NOTIFICATION_POLICY -> {
                            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            context.startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Automation Rule" else "Create Automation Rule") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Permission Warning Card
            if (uiState.permissionWarning != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Permission Required",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    text = uiState.permissionWarning!!,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }

            // Rule Summary Card - Dynamic preview of what the rule will do
            item {
                RuleSummaryCard(
                    triggers = uiState.triggers,
                    actions = uiState.actions
                )
            }

            // Rule Name Section
            item {
                OutlinedTextField(
                    value = uiState.ruleName,
                    onValueChange = viewModel::updateRuleName,
                    label = { Text("Rule Name") },
                    placeholder = { Text("e.g., Silent at College") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Edit, contentDescription = null)
                    },
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { { Text(it) } }
                )
            }

            // Description Section
            item {
                OutlinedTextField(
                    value = uiState.description,
                    onValueChange = viewModel::updateDescription,
                    label = { Text("Description") },
                    placeholder = { Text("Describe what this rule does") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    leadingIcon = {
                        Icon(Icons.Default.Description, contentDescription = null)
                    },
                    isError = uiState.descriptionError != null,
                    supportingText = uiState.descriptionError?.let { { Text(it) } }
                )
            }


            // Triggers Section
            item {
                SectionHeader(
                    title = "Triggers",
                    subtitle = "When should this rule activate?",
                    onAddClick = { showTriggerDialog = true },
                    error = uiState.triggersError
                )
            }

            itemsIndexed(uiState.triggers) { index, trigger ->
                TriggerCard(
                    trigger = trigger,
                    onRemove = { viewModel.removeTrigger(index) }
                )
            }

            // Actions Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader(
                    title = "Actions",
                    subtitle = "What should happen?",
                    onAddClick = { showActionDialog = true },
                    error = uiState.actionsError
                )
            }

            itemsIndexed(uiState.actions) { index, action ->
                ActionCard(
                    action = action,
                    onRemove = { viewModel.removeAction(index) }
                )
            }

            // Exit Action Section - Only shown when TIME_RANGE trigger is present
            if (viewModel.hasTimeRangeTrigger()) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    ExitActionSection(
                        exitActionType = uiState.exitActionType,
                        exitActionParams = uiState.exitActionParams,
                        onExitActionChanged = { actionType, params ->
                            viewModel.updateExitAction(actionType, params)
                        }
                    )
                }
            }

            // Create/Save Button
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { viewModel.saveRule() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !uiState.isLoading && isFormValid
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(if (isEditMode) Icons.Default.Save else Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (isEditMode) "Save Changes" else "Create Rule",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        // Dialogs
        if (showTriggerDialog) {
            AddTriggerDialog(
                onDismiss = { showTriggerDialog = false },
                onConfirm = { trigger ->
                    viewModel.addTrigger(trigger)
                    showTriggerDialog = false
                }
            )
        }

        if (showActionDialog) {
            AddActionDialog(
                onDismiss = { showActionDialog = false },
                onConfirm = { action ->
                    viewModel.addAction(action)
                    showActionDialog = false
                }
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    onAddClick: () -> Unit,
    error: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (error != null) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.secondaryContainer
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = error ?: subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    }
                )
            }
            FilledTonalButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add")
            }
        }
    }
}

/**
 * Exit Action Section for Time Range triggers.
 * Allows users to specify an action that executes when the time range ends.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExitActionSection(
    exitActionType: ActionType?,
    exitActionParams: String?,
    onExitActionChanged: (ActionType?, String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showConfigDialog by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(exitActionType) }

    // Available exit actions (subset of actions that make sense for "exit")
    val exitActions = listOf(
        ActionType.DISABLE_DND,
        ActionType.ENABLE_DND,
        ActionType.TOGGLE_SILENT_MODE,
        ActionType.TOGGLE_VIBRATE,
        ActionType.DISABLE_FLASHLIGHT,
        ActionType.SEND_NOTIFICATION,
        ActionType.ADJUST_BRIGHTNESS,
        ActionType.ADJUST_VOLUME
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Exit Action (Optional)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = "Action to perform when time range ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Dropdown for exit action selection
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedType?.let { getActionTypeName(it) } ?: "None (No exit action)",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Action when time ends") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    // None option
                    DropdownMenuItem(
                        text = { Text("None (No exit action)") },
                        onClick = {
                            selectedType = null
                            onExitActionChanged(null, null)
                            expanded = false
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Clear, contentDescription = null)
                        }
                    )
                    HorizontalDivider()
                    // Action options
                    exitActions.forEach { actionType ->
                        DropdownMenuItem(
                            text = { Text(getActionTypeName(actionType)) },
                            onClick = {
                                selectedType = actionType
                                expanded = false
                                // For simple actions, set default params
                                val defaultParams = getDefaultExitActionParams(actionType)
                                onExitActionChanged(actionType, defaultParams)
                            },
                            leadingIcon = {
                                Icon(getActionIcon(actionType), contentDescription = null)
                            }
                        )
                    }
                }
            }

            // Show current selection
            if (selectedType != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getActionIcon(selectedType!!),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "When time range ends:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = getActionTypeName(selectedType!!),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(
                            onClick = {
                                selectedType = null
                                onExitActionChanged(null, null)
                            }
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove exit action",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Get default parameters for exit actions
 */
private fun getDefaultExitActionParams(actionType: ActionType): String {
    return when (actionType) {
        ActionType.DISABLE_DND -> "{}"
        ActionType.ENABLE_DND -> "{}"
        ActionType.TOGGLE_SILENT_MODE -> "{}"
        ActionType.TOGGLE_VIBRATE -> "{}"
        ActionType.DISABLE_FLASHLIGHT -> "{}"
        ActionType.SEND_NOTIFICATION -> """{"title":"Time Range Ended","message":"Your scheduled automation has ended."}"""
        ActionType.ADJUST_BRIGHTNESS -> """{"level":50}"""
        ActionType.ADJUST_VOLUME -> """{"level":50,"streamType":"MUSIC"}"""
        else -> "{}"
    }
}

@Composable
private fun TriggerCard(
    trigger: Trigger,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getTriggerIcon(trigger.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getTriggerTypeName(trigger.type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatTriggerParameters(trigger),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Operator: ${trigger.logicalOperator.name}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatTriggerParameters(trigger: Trigger): String {
    return try {
        val params = trigger.parameters
        when (trigger.type) {
            TriggerType.TIME_BASED -> {
                val timeRegex = """"time":"(\d{2}:\d{2})"""".toRegex()
                val daysRegex = """"days":"([^"]+)"""".toRegex()
                val time = timeRegex.find(params)?.groupValues?.get(1) ?: "00:00"
                val days = daysRegex.find(params)?.groupValues?.get(1) ?: "Every day"
                "At $time on ${if (days.isBlank()) "every day" else days}"
            }
            TriggerType.LOCATION_BASED -> {
                val latRegex = """"latitude":(-?\d+\.?\d*)""".toRegex()
                val lngRegex = """"longitude":(-?\d+\.?\d*)""".toRegex()
                val radiusRegex = """"radius":(\d+)""".toRegex()
                val lat = latRegex.find(params)?.groupValues?.get(1) ?: "0"
                val lng = lngRegex.find(params)?.groupValues?.get(1) ?: "0"
                val radius = radiusRegex.find(params)?.groupValues?.get(1) ?: "100"
                "Location: $lat, $lng (${radius}m radius)"
            }
            TriggerType.BATTERY_LEVEL -> {
                val levelRegex = """"level":(\d+)""".toRegex()
                val opRegex = """"operator":"([^"]+)"""".toRegex()
                val level = levelRegex.find(params)?.groupValues?.get(1) ?: "20"
                val op = opRegex.find(params)?.groupValues?.get(1) ?: "less_than"
                val opText = when (op) {
                    "less_than" -> "below"
                    "greater_than" -> "above"
                    else -> "at"
                }
                "Battery $opText $level%"
            }
            TriggerType.CHARGING_STATUS -> {
                val chargingRegex = """"charging":(true|false)""".toRegex()
                val charging = chargingRegex.find(params)?.groupValues?.get(1)?.toBoolean() ?: true
                if (charging) "When charging" else "When not charging"
            }
            TriggerType.WIFI_CONNECTED -> {
                val ssidRegex = """"ssid":"([^"]*)"""".toRegex()
                val ssid = ssidRegex.find(params)?.groupValues?.get(1) ?: ""
                if (ssid.isBlank()) "Any WiFi network" else "WiFi: $ssid"
            }
            TriggerType.BLUETOOTH_CONNECTED -> {
                val deviceRegex = """"deviceName":"([^"]*)"""".toRegex()
                val device = deviceRegex.find(params)?.groupValues?.get(1) ?: ""
                if (device.isBlank()) "Any Bluetooth device" else "Device: $device"
            }
            else -> "Default settings"
        }
    } catch (e: Exception) {
        "Default settings"
    }
}

@Composable
private fun ActionCard(
    action: Action,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = getActionIcon(action.type),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = getActionTypeName(action.type),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formatActionParameters(action),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

private fun formatActionParameters(action: Action): String {
    return try {
        val params = action.parameters
        when (action.type) {
            ActionType.ADJUST_VOLUME -> {
                val levelRegex = """"level":(\d+)""".toRegex()
                val streamRegex = """"streamType":"([^"]+)"""".toRegex()
                val level = levelRegex.find(params)?.groupValues?.get(1) ?: "50"
                val stream = streamRegex.find(params)?.groupValues?.get(1) ?: "MUSIC"
                "$stream volume: $level%"
            }
            ActionType.ADJUST_BRIGHTNESS -> {
                val levelRegex = """"level":(\d+)""".toRegex()
                val level = levelRegex.find(params)?.groupValues?.get(1) ?: "50"
                "Brightness: $level%"
            }
            ActionType.SEND_NOTIFICATION -> {
                val titleRegex = """"title":"([^"]+)"""".toRegex()
                val title = titleRegex.find(params)?.groupValues?.get(1) ?: "Notification"
                "\"$title\""
            }
            ActionType.LAUNCH_APP -> {
                val pkgRegex = """"packageName":"([^"]+)"""".toRegex()
                val pkg = pkgRegex.find(params)?.groupValues?.get(1) ?: ""
                if (pkg.isBlank()) "Not configured" else pkg.substringAfterLast(".")
            }
            else -> "Default settings"
        }
    } catch (e: Exception) {
        "Default settings"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTriggerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Trigger) -> Unit
) {
    var selectedType by remember { mutableStateOf(TriggerType.TIME_BASED) }
    var selectedOperator by remember { mutableStateOf(LogicalOperator.AND) }
    var showConfigDialog by remember { mutableStateOf(false) }

    if (showConfigDialog) {
        TriggerConfigDialog(
            triggerType = selectedType,
            logicalOperator = selectedOperator,
            onDismiss = { showConfigDialog = false },
            onConfirm = { trigger ->
                onConfirm(trigger)
                showConfigDialog = false
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Trigger") },
            text = {
                LazyColumn {
                    item {
                        Text("Select trigger type:")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(TriggerType.values().size) { index ->
                        val type = TriggerType.values()[index]
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(getTriggerTypeName(type)) },
                            leadingIcon = {
                                Icon(getTriggerIcon(type), contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Logical Operator:")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    item {
                        Row {
                            FilterChip(
                                selected = selectedOperator == LogicalOperator.AND,
                                onClick = { selectedOperator = LogicalOperator.AND },
                                label = { Text("AND") },
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            FilterChip(
                                selected = selectedOperator == LogicalOperator.OR,
                                onClick = { selectedOperator = LogicalOperator.OR },
                                label = { Text("OR") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = true }) {
                    Text("Configure")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TriggerConfigDialog(
    triggerType: TriggerType,
    logicalOperator: LogicalOperator,
    onDismiss: () -> Unit,
    onConfirm: (Trigger) -> Unit
) {
    // Time-based trigger state
    var selectedHour by remember { mutableStateOf(8) }
    var selectedMinute by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf(setOf("MON", "TUE", "WED", "THU", "FRI")) }

    // Time Range trigger state
    var startHour by remember { mutableStateOf(9) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(17) }
    var endMinute by remember { mutableStateOf(0) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    // Location-based trigger state
    var latitude by remember { mutableStateOf("0.0") }
    var longitude by remember { mutableStateOf("0.0") }
    var radius by remember { mutableStateOf("100") }

    // Battery trigger state
    var batteryLevel by remember { mutableStateOf(20f) }
    var batteryOperator by remember { mutableStateOf("less_than") }

    // Charging trigger state
    var chargingRequired by remember { mutableStateOf(true) }

    // WiFi trigger state
    var wifiSsid by remember { mutableStateOf("") }

    // Bluetooth trigger state
    var bluetoothDeviceName by remember { mutableStateOf("") }

    // App Opened trigger state
    var selectedAppPackageName by remember { mutableStateOf<String?>(null) }

    // DND trigger state (ON/OFF target)
    var dndTargetState by remember { mutableStateOf("ON") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${getTriggerTypeName(triggerType)}") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (triggerType) {
                    TriggerType.TIME_BASED -> {
                        item {
                            Text("Select Time:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Material 3 TimePicker with 1-minute precision
                            val timePickerState = rememberTimePickerState(
                                initialHour = selectedHour,
                                initialMinute = selectedMinute,
                                is24Hour = true
                            )

                            // Update state when time changes
                            LaunchedEffect(timePickerState.hour, timePickerState.minute) {
                                selectedHour = timePickerState.hour
                                selectedMinute = timePickerState.minute
                            }

                            TimePicker(
                                state = timePickerState,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Select Days:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            DaySelector(
                                selectedDays = selectedDays,
                                onSelectionChanged = { newSelection ->
                                    selectedDays = newSelection
                                }
                            )
                        }
                    }

                    TriggerType.LOCATION_BASED -> {
                        item {
                            var showMapPicker by remember { mutableStateOf(false) }

                            Text("Location Coordinates:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Display current coordinates
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = "Latitude: $latitude",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "Longitude: $longitude",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        FilledTonalButton(
                                            onClick = { showMapPicker = true }
                                        ) {
                                            Icon(Icons.Default.Map, contentDescription = null)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Pick on Map")
                                        }
                                    }
                                }
                            }

                            // Map Picker Dialog
                            if (showMapPicker) {
                                MapPickerDialog(
                                    initialLatitude = latitude.toDoubleOrNull() ?: 0.0,
                                    initialLongitude = longitude.toDoubleOrNull() ?: 0.0,
                                    onDismiss = { showMapPicker = false },
                                    onLocationSelected = { location ->
                                        latitude = String.format(Locale.US, "%.6f", location.latitude)
                                        longitude = String.format(Locale.US, "%.6f", location.longitude)
                                        showMapPicker = false
                                    }
                                )
                            }
                        }
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = radius,
                                onValueChange = { radius = it },
                                label = { Text("Radius (meters)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.RadioButtonChecked, null) }
                            )
                        }
                        item {
                            Text(
                                "Tip: Use the map picker to select your location, or enter coordinates manually below.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Manual entry fields (collapsible/alternative)
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude (manual)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.MyLocation, null) }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude (manual)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                            )
                        }
                    }

                    TriggerType.BATTERY_LEVEL -> {
                        item {
                            Text("Battery Level: ${batteryLevel.toInt()}%", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = batteryLevel,
                                onValueChange = { batteryLevel = it },
                                valueRange = 5f..100f,
                                steps = 18,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Condition:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FilterChip(
                                    selected = batteryOperator == "less_than",
                                    onClick = { batteryOperator = "less_than" },
                                    label = { Text("Below") }
                                )
                                FilterChip(
                                    selected = batteryOperator == "equals",
                                    onClick = { batteryOperator = "equals" },
                                    label = { Text("Equals") }
                                )
                                FilterChip(
                                    selected = batteryOperator == "greater_than",
                                    onClick = { batteryOperator = "greater_than" },
                                    label = { Text("Above") }
                                )
                            }
                        }
                    }

                    TriggerType.CHARGING_STATUS -> {
                        item {
                            Text("Trigger when:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FilterChip(
                                    selected = chargingRequired,
                                    onClick = { chargingRequired = true },
                                    label = { Text("Charging") },
                                    leadingIcon = { Icon(Icons.Default.BatteryChargingFull, null) }
                                )
                                FilterChip(
                                    selected = !chargingRequired,
                                    onClick = { chargingRequired = false },
                                    label = { Text("Not Charging") },
                                    leadingIcon = { Icon(Icons.Default.BatteryStd, null) }
                                )
                            }
                        }
                    }

                    TriggerType.WIFI_CONNECTED -> {
                        item {
                            Text("WiFi Network:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Select a specific network or leave empty for any WiFi",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            WifiNetworkSelector(
                                selectedSsid = wifiSsid.ifBlank { null },
                                onNetworkSelected = { ssid ->
                                    wifiSsid = ssid
                                },
                                label = "Network Name (SSID)"
                            )
                            // Option to clear selection
                            if (wifiSsid.isNotBlank()) {
                                TextButton(
                                    onClick = { wifiSsid = "" },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Clear selection (trigger for any WiFi)")
                                }
                            }
                        }
                    }

                    TriggerType.BLUETOOTH_CONNECTED -> {
                        item {
                            Text("Bluetooth Device:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Select a paired device or leave empty for any connection",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            BluetoothDeviceSelector(
                                selectedDeviceName = bluetoothDeviceName.ifBlank { null },
                                onDeviceSelected = { deviceName ->
                                    bluetoothDeviceName = deviceName
                                },
                                label = "Paired Device"
                            )
                            // Option to clear selection
                            if (bluetoothDeviceName.isNotBlank()) {
                                TextButton(
                                    onClick = { bluetoothDeviceName = "" },
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text("Clear selection (trigger for any device)")
                                }
                            }
                        }
                    }

                    TriggerType.APP_OPENED -> {
                        item {
                            Text("Select App to Monitor:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            AppSelectionDropdown(
                                selectedPackageName = selectedAppPackageName,
                                onAppSelected = { packageName ->
                                    selectedAppPackageName = packageName
                                },
                                label = "Target App"
                            )
                        }
                    }

                    TriggerType.TIME_RANGE -> {
                        item {
                            Text("Active Time Range:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Trigger is active between these times",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                // Start Time
                                OutlinedButton(
                                    onClick = { showStartTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("From", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            String.format("%02d:%02d", startHour, startMinute),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // End Time
                                OutlinedButton(
                                    onClick = { showEndTimePicker = true },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("To", style = MaterialTheme.typography.labelSmall)
                                        Text(
                                            String.format("%02d:%02d", endHour, endMinute),
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }
                        item {
                            if (startHour * 60 + startMinute > endHour * 60 + endMinute) {
                                Text(
                                    "⚡ Midnight crossover: Active from ${String.format("%02d:%02d", startHour, startMinute)} to midnight, then midnight to ${String.format("%02d:%02d", endHour, endMinute)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    TriggerType.DO_NOT_DISTURB -> {
                        item {
                            Text("Trigger when DND is:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                FilterChip(
                                    selected = dndTargetState == "ON",
                                    onClick = { dndTargetState = "ON" },
                                    label = { Text("Turned ON") },
                                    leadingIcon = { Icon(Icons.Default.DoNotDisturb, null) }
                                )
                                FilterChip(
                                    selected = dndTargetState == "OFF",
                                    onClick = { dndTargetState = "OFF" },
                                    label = { Text("Turned OFF") },
                                    leadingIcon = { Icon(Icons.Default.NotificationsActive, null) }
                                )
                            }
                        }
                    }

                    else -> {
                        item {
                            Text(
                                "This trigger type uses default settings.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parameters = when (triggerType) {
                    TriggerType.TIME_BASED -> {
                        val timeStr = String.format("%02d:%02d", selectedHour, selectedMinute)
                        val daysStr = selectedDays.joinToString(",")
                        """{"time":"$timeStr","days":"$daysStr"}"""
                    }
                    TriggerType.TIME_RANGE -> {
                        val startTimeStr = String.format("%02d:%02d", startHour, startMinute)
                        val endTimeStr = String.format("%02d:%02d", endHour, endMinute)
                        """{"start_time":"$startTimeStr","end_time":"$endTimeStr"}"""
                    }
                    TriggerType.LOCATION_BASED -> {
                        val lat = latitude.toDoubleOrNull() ?: 0.0
                        val lng = longitude.toDoubleOrNull() ?: 0.0
                        val rad = radius.toIntOrNull() ?: 100
                        """{"latitude":$lat,"longitude":$lng,"radius":$rad}"""
                    }
                    TriggerType.BATTERY_LEVEL -> {
                        """{"level":${batteryLevel.toInt()},"operator":"$batteryOperator"}"""
                    }
                    TriggerType.CHARGING_STATUS -> {
                        """{"charging":$chargingRequired}"""
                    }
                    TriggerType.WIFI_CONNECTED -> {
                        """{"ssid":"$wifiSsid"}"""
                    }
                    TriggerType.BLUETOOTH_CONNECTED -> {
                        """{"deviceName":"$bluetoothDeviceName"}"""
                    }
                    TriggerType.APP_OPENED -> {
                        """{"package_name":"${selectedAppPackageName ?: ""}"}"""
                    }
                    TriggerType.DO_NOT_DISTURB -> {
                        """{"target_state":"$dndTargetState"}"""
                    }
                    else -> "{}"
                }

                onConfirm(
                    Trigger(
                        id = 0L,
                        ruleId = 0L,
                        type = triggerType,
                        parameters = parameters,
                        logicalOperator = logicalOperator,
                        isActive = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }) {
                Text("Add Trigger")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    )

    // TimePicker Dialog for Start Time (TIME_RANGE trigger)
    if (showStartTimePicker) {
        val startTimePickerState = rememberTimePickerState(
            initialHour = startHour,
            initialMinute = startMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showStartTimePicker = false },
            title = { Text("Select Start Time") },
            text = {
                TimePicker(state = startTimePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    startHour = startTimePickerState.hour
                    startMinute = startTimePickerState.minute
                    showStartTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // TimePicker Dialog for End Time (TIME_RANGE trigger)
    if (showEndTimePicker) {
        val endTimePickerState = rememberTimePickerState(
            initialHour = endHour,
            initialMinute = endMinute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showEndTimePicker = false },
            title = { Text("Select End Time") },
            text = {
                TimePicker(state = endTimePickerState)
            },
            confirmButton = {
                TextButton(onClick = {
                    endHour = endTimePickerState.hour
                    endMinute = endTimePickerState.minute
                    showEndTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddActionDialog(
    onDismiss: () -> Unit,
    onConfirm: (Action) -> Unit
) {
    val context = LocalContext.current
    var selectedType by remember { mutableStateOf(ActionType.TOGGLE_SILENT_MODE) }
    var showConfigDialog by remember { mutableStateOf(false) }

    // Permission dialog state
    var showPermissionDialog by remember { mutableStateOf(false) }
    var requiredPermission by remember { mutableStateOf<PermissionManager.SpecialPermission?>(null) }

    // Check which special permission is needed for an action type
    fun getRequiredSpecialPermission(actionType: ActionType): PermissionManager.SpecialPermission? {
        return when (actionType) {
            // Display/Settings actions require WRITE_SETTINGS
            ActionType.ADJUST_BRIGHTNESS,
            ActionType.TOGGLE_AUTO_BRIGHTNESS,
            ActionType.TOGGLE_AUTO_ROTATE -> PermissionManager.SpecialPermission.WRITE_SETTINGS

            // DND and ringer actions require DND_POLICY
            ActionType.ENABLE_DND,
            ActionType.DISABLE_DND,
            ActionType.TOGGLE_SILENT_MODE,
            ActionType.TOGGLE_VIBRATE,
            ActionType.SET_RINGER_MODE -> PermissionManager.SpecialPermission.DND_POLICY

            // Global actions require ACCESSIBILITY
            ActionType.GLOBAL_ACTION_LOCK_SCREEN,
            ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT,
            ActionType.GLOBAL_ACTION_POWER_DIALOG,
            ActionType.BLOCK_APP -> PermissionManager.SpecialPermission.ACCESSIBILITY

            // Clear notifications requires NOTIFICATION_LISTENER
            ActionType.CLEAR_NOTIFICATIONS -> PermissionManager.SpecialPermission.NOTIFICATION_LISTENER

            else -> null
        }
    }

    // Check if permission is granted
    fun hasPermission(permission: PermissionManager.SpecialPermission): Boolean {
        return when (permission) {
            PermissionManager.SpecialPermission.WRITE_SETTINGS ->
                Settings.System.canWrite(context)
            PermissionManager.SpecialPermission.DND_POLICY -> {
                val nm = context.getSystemService(android.content.Context.NOTIFICATION_SERVICE)
                    as android.app.NotificationManager
                nm.isNotificationPolicyAccessGranted
            }
            PermissionManager.SpecialPermission.ACCESSIBILITY -> {
                val enabledServices = Settings.Secure.getString(
                    context.contentResolver,
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
                ) ?: ""
                enabledServices.contains(context.packageName)
            }
            PermissionManager.SpecialPermission.NOTIFICATION_LISTENER -> {
                val flat = Settings.Secure.getString(
                    context.contentResolver,
                    "enabled_notification_listeners"
                ) ?: ""
                flat.contains(context.packageName)
            }
            PermissionManager.SpecialPermission.OVERLAY ->
                Settings.canDrawOverlays(context)
            else -> true
        }
    }

    // Get intent for permission
    fun getPermissionIntent(permission: PermissionManager.SpecialPermission): Intent {
        return when (permission) {
            PermissionManager.SpecialPermission.WRITE_SETTINGS ->
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            PermissionManager.SpecialPermission.DND_POLICY ->
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
            PermissionManager.SpecialPermission.ACCESSIBILITY ->
                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PermissionManager.SpecialPermission.NOTIFICATION_LISTENER ->
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            PermissionManager.SpecialPermission.OVERLAY ->
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
            else -> Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
    }

    // Permission rationale dialog
    if (showPermissionDialog && requiredPermission != null) {
        SpecialPermissionDialog(
            permission = requiredPermission!!,
            onConfirm = {
                context.startActivity(getPermissionIntent(requiredPermission!!))
                showPermissionDialog = false
            },
            onDismiss = {
                showPermissionDialog = false
            }
        )
    } else if (showConfigDialog) {
        ActionConfigDialog(
            actionType = selectedType,
            onDismiss = { showConfigDialog = false },
            onConfirm = { action ->
                onConfirm(action)
                showConfigDialog = false
            }
        )
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Add Action") },
            text = {
                LazyColumn {
                    item {
                        Text("Select action type:")
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    items(ActionType.values().size) { index ->
                        val type = ActionType.values()[index]
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(getActionTypeName(type)) },
                            leadingIcon = {
                                Icon(getActionIcon(type), contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    // Check if permission is needed
                    val neededPermission = getRequiredSpecialPermission(selectedType)
                    if (neededPermission != null && !hasPermission(neededPermission)) {
                        // Show permission dialog
                        requiredPermission = neededPermission
                        showPermissionDialog = true
                    } else {
                        // Proceed to configuration
                        showConfigDialog = true
                    }
                }) {
                    Text("Configure")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionConfigDialog(
    actionType: ActionType,
    onDismiss: () -> Unit,
    onConfirm: (Action) -> Unit
) {
    // Volume action state
    var volumeLevel by remember { mutableStateOf(50f) }
    var streamType by remember { mutableStateOf("MUSIC") }

    // Brightness action state
    var brightnessLevel by remember { mutableStateOf(50f) }

    // Notification action state
    var notificationTitle by remember { mutableStateOf("Automation") }
    var notificationMessage by remember { mutableStateOf("Action triggered") }

    // Launch app state
    var packageName by remember { mutableStateOf("") }

    // Open URL state
    var url by remember { mutableStateOf("https://") }


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure ${getActionTypeName(actionType)}") },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (actionType) {
                    ActionType.ADJUST_VOLUME -> {
                        item {
                            Text("Volume Level: ${volumeLevel.toInt()}%", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = volumeLevel,
                                onValueChange = { volumeLevel = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            Text("Stream Type:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                listOf("MUSIC", "RING", "ALARM", "NOTIFICATION").forEach { type ->
                                    FilterChip(
                                        selected = streamType == type,
                                        onClick = { streamType = type },
                                        label = { Text(type.take(4)) }
                                    )
                                }
                            }
                        }
                    }

                    ActionType.ADJUST_BRIGHTNESS -> {
                        item {
                            Text("Brightness Level: ${brightnessLevel.toInt()}%", style = MaterialTheme.typography.labelLarge)
                            Slider(
                                value = brightnessLevel,
                                onValueChange = { brightnessLevel = it },
                                valueRange = 0f..100f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    ActionType.SEND_NOTIFICATION -> {
                        item {
                            OutlinedTextField(
                                value = notificationTitle,
                                onValueChange = { notificationTitle = it },
                                label = { Text("Notification Title") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Default.Title, null) }
                            )
                        }
                        item {
                            OutlinedTextField(
                                value = notificationMessage,
                                onValueChange = { notificationMessage = it },
                                label = { Text("Message") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = { Icon(Icons.Default.Message, null) }
                            )
                        }
                    }

                    ActionType.LAUNCH_APP -> {
                        item {
                            Text("Select App to Launch:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(8.dp))
                            AppSelectionDropdown(
                                selectedPackageName = packageName.ifBlank { null },
                                onAppSelected = { selectedPkg ->
                                    packageName = selectedPkg
                                },
                                label = "App"
                            )
                        }
                    }

                    ActionType.BLOCK_APP -> {
                        item {
                            Text("Select App to Block:", style = MaterialTheme.typography.labelLarge)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "When this rule is active, opening this app will send you back to the home screen.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            AppSelectionDropdown(
                                selectedPackageName = packageName.ifBlank { null },
                                onAppSelected = { selectedPkg ->
                                    packageName = selectedPkg
                                },
                                label = "App to Block"
                            )
                        }
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Text(
                                        "Requires Accessibility Service to be enabled",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }


                    else -> {
                        item {
                            Text(
                                "This action type uses default settings.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val parameters = when (actionType) {
                    ActionType.ADJUST_VOLUME -> {
                        """{"level":${volumeLevel.toInt()},"streamType":"$streamType"}"""
                    }
                    ActionType.ADJUST_BRIGHTNESS -> {
                        """{"level":${brightnessLevel.toInt()}}"""
                    }
                    ActionType.SEND_NOTIFICATION -> {
                        """{"title":"$notificationTitle","message":"$notificationMessage"}"""
                    }
                    ActionType.LAUNCH_APP -> {
                        """{"packageName":"$packageName"}"""
                    }
                    ActionType.BLOCK_APP -> {
                        """{"packageName":"$packageName"}"""
                    }
                    else -> "{}"
                }

                onConfirm(
                    Action(
                        id = 0L,
                        ruleId = 0L,
                        type = actionType,
                        parameters = parameters,
                        sequence = 0,
                        isEnabled = true,
                        createdAt = System.currentTimeMillis()
                    )
                )
            }) {
                Text("Add Action")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Back")
            }
        }
    )
}

// Helper functions
private fun getTriggerTypeName(type: TriggerType): String = when (type) {
    TriggerType.TIME_BASED -> "Time Based"
    TriggerType.TIME_RANGE -> "Time Range"
    TriggerType.LOCATION_BASED -> "Location Based"
    TriggerType.BATTERY_LEVEL -> "Battery Level"
    TriggerType.CHARGING_STATUS -> "Charging Status"
    TriggerType.WIFI_CONNECTED -> "WiFi Connected"
    TriggerType.BLUETOOTH_CONNECTED -> "Bluetooth Connected"
    TriggerType.HEADPHONES_CONNECTED -> "Headphones Connected"
    TriggerType.APP_OPENED -> "App Opened"
    TriggerType.AIRPLANE_MODE -> "Airplane Mode"
    TriggerType.DO_NOT_DISTURB -> "Do Not Disturb"
}

private fun getTriggerIcon(type: TriggerType) = when (type) {
    TriggerType.TIME_BASED -> Icons.Default.Schedule
    TriggerType.TIME_RANGE -> Icons.Default.DateRange
    TriggerType.LOCATION_BASED -> Icons.Default.LocationOn
    TriggerType.BATTERY_LEVEL -> Icons.Default.BatteryStd
    TriggerType.CHARGING_STATUS -> Icons.Default.Power
    TriggerType.WIFI_CONNECTED -> Icons.Default.Wifi
    TriggerType.BLUETOOTH_CONNECTED -> Icons.Default.Bluetooth
    TriggerType.HEADPHONES_CONNECTED -> Icons.Default.Headphones
    TriggerType.APP_OPENED -> Icons.Default.Apps
    TriggerType.AIRPLANE_MODE -> Icons.Default.AirplanemodeActive
    TriggerType.DO_NOT_DISTURB -> Icons.Default.DoNotDisturb
}

private fun getActionTypeName(type: ActionType): String = when (type) {
    // Hardware
    ActionType.TOGGLE_FLASHLIGHT -> "Toggle Flashlight"
    ActionType.ENABLE_FLASHLIGHT -> "Enable Flashlight"
    ActionType.DISABLE_FLASHLIGHT -> "Disable Flashlight"
    ActionType.VIBRATE -> "Vibrate"
    // Audio
    ActionType.TOGGLE_SILENT_MODE -> "Toggle Silent Mode"
    ActionType.ADJUST_VOLUME -> "Adjust Volume"
    ActionType.TOGGLE_VIBRATE -> "Toggle Vibrate"
    ActionType.SET_RINGER_MODE -> "Set Ringer Mode"
    // Display
    ActionType.ADJUST_BRIGHTNESS -> "Adjust Brightness"
    ActionType.TOGGLE_AUTO_BRIGHTNESS -> "Toggle Auto Brightness"
    ActionType.TOGGLE_AUTO_ROTATE -> "Toggle Auto Rotate"
    // System
    ActionType.GLOBAL_ACTION_LOCK_SCREEN -> "Lock Screen"
    ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT -> "Take Screenshot"
    ActionType.GLOBAL_ACTION_POWER_DIALOG -> "Power Dialog"
    // Apps
    ActionType.LAUNCH_APP -> "Launch App"
    ActionType.BLOCK_APP -> "Block App"
    // Notifications
    ActionType.SEND_NOTIFICATION -> "Send Notification"
    ActionType.CLEAR_NOTIFICATIONS -> "Clear Notifications"
    // DND
    ActionType.ENABLE_DND -> "Enable Do Not Disturb"
    ActionType.DISABLE_DND -> "Disable Do Not Disturb"
}

private fun getActionIcon(type: ActionType) = when (type) {
    // Hardware
    ActionType.TOGGLE_FLASHLIGHT, ActionType.ENABLE_FLASHLIGHT, ActionType.DISABLE_FLASHLIGHT -> Icons.Default.FlashOn
    ActionType.VIBRATE -> Icons.Default.Vibration
    // Audio
    ActionType.TOGGLE_SILENT_MODE -> Icons.Default.VolumeOff
    ActionType.ADJUST_VOLUME, ActionType.TOGGLE_VIBRATE -> Icons.Default.VolumeUp
    ActionType.SET_RINGER_MODE -> Icons.Default.VolumeUp
    // Display
    ActionType.ADJUST_BRIGHTNESS, ActionType.TOGGLE_AUTO_BRIGHTNESS -> Icons.Default.Brightness6
    ActionType.TOGGLE_AUTO_ROTATE -> Icons.Default.ScreenRotation
    // System
    ActionType.GLOBAL_ACTION_LOCK_SCREEN -> Icons.Default.Lock
    ActionType.GLOBAL_ACTION_TAKE_SCREENSHOT -> Icons.Default.Screenshot
    ActionType.GLOBAL_ACTION_POWER_DIALOG -> Icons.Default.PowerSettingsNew
    // Apps
    ActionType.LAUNCH_APP -> Icons.Default.Launch
    ActionType.BLOCK_APP -> Icons.Default.Block
    // Notifications
    ActionType.SEND_NOTIFICATION, ActionType.CLEAR_NOTIFICATIONS -> Icons.Default.Notifications
    // DND
    ActionType.ENABLE_DND, ActionType.DISABLE_DND -> Icons.Default.DoNotDisturb
}

private fun getDefaultTriggerParams(type: TriggerType): String = when (type) {
    TriggerType.TIME_BASED -> """{"time":"08:00","days":"MON,TUE,WED,THU,FRI"}"""
    TriggerType.TIME_RANGE -> """{"start_time":"09:00","end_time":"17:00"}"""
    TriggerType.BATTERY_LEVEL -> """{"level":20,"operator":"less_than"}"""
    TriggerType.LOCATION_BASED -> """{"latitude":0.0,"longitude":0.0,"radius":100}"""
    TriggerType.CHARGING_STATUS -> """{"charging":true}"""
    TriggerType.WIFI_CONNECTED -> """{"ssid":""}"""
    TriggerType.DO_NOT_DISTURB -> """{"target_state":"ON"}"""
    else -> "{}"
}

private fun getDefaultActionParams(type: ActionType): String = when (type) {
    ActionType.SEND_NOTIFICATION -> """{"title":"Automation","message":"Action triggered"}"""
    ActionType.ADJUST_VOLUME -> """{"level":50,"streamType":"MUSIC"}"""
    ActionType.ADJUST_BRIGHTNESS -> """{"level":50}"""
    ActionType.LAUNCH_APP -> """{"packageName":""}"""
    ActionType.BLOCK_APP -> """{"packageName":""}"""
    else -> "{}"
}
