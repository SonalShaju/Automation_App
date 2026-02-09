package com.example.automationapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.automationapp.util.PermissionManager

/**
 * A reusable dialog that explains why a permission is needed
 * before sending the user to the Settings page.
 */
@Composable
fun PermissionRationaleDialog(
    title: String,
    description: String,
    icon: ImageVector = Icons.Default.Security,
    confirmButtonText: String = "Open Settings",
    dismissButtonText: String = "Cancel",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "You will be taken to Settings to grant this permission.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(confirmButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissButtonText)
            }
        }
    )
}

/**
 * Specialized dialog for Write Settings permission
 */
@Composable
fun WriteSettingsPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Modify System Settings",
        description = "To change brightness, screen timeout, or other display settings, you must allow this app to modify system settings.",
        icon = Icons.Default.Brightness6,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for DND Policy permission
 */
@Composable
fun DndPolicyPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Do Not Disturb Access",
        description = "To control Do Not Disturb mode, silent mode, or ringer settings, you must grant notification policy access.",
        icon = Icons.Default.DoNotDisturb,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for Accessibility permission
 */
@Composable
fun AccessibilityPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Accessibility Service",
        description = "To perform global actions like pressing Back, Home, or taking screenshots, you must enable the Accessibility Service.",
        icon = Icons.Default.Accessibility,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for Notification Listener permission
 */
@Composable
fun NotificationListenerPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Notification Access",
        description = "To read, manage, or respond to notifications from other apps, you must grant notification access.",
        icon = Icons.Default.Notifications,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for Overlay permission
 */
@Composable
fun OverlayPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Display Over Other Apps",
        description = "To show floating controls or overlays, you must allow this app to display over other apps.",
        icon = Icons.Default.Layers,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for Exact Alarm permission
 */
@Composable
fun ExactAlarmPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Schedule Exact Alarms",
        description = "To trigger automations at precise times, you must allow this app to schedule exact alarms.",
        icon = Icons.Default.Alarm,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Specialized dialog for Usage Stats permission
 */
@Composable
fun UsageStatsPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    PermissionRationaleDialog(
        title = "Usage Access",
        description = "To detect which apps are running or recently used, you must grant usage access.",
        icon = Icons.Default.BarChart,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * Generic permission dialog that can be used with any SpecialPermission
 */
@Composable
fun SpecialPermissionDialog(
    permission: PermissionManager.SpecialPermission,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, title, description) = when (permission) {
        PermissionManager.SpecialPermission.WRITE_SETTINGS -> Triple(
            Icons.Default.Brightness6,
            "Modify System Settings",
            "To change brightness, screen timeout, or other display settings, you must allow this app to modify system settings."
        )
        PermissionManager.SpecialPermission.OVERLAY -> Triple(
            Icons.Default.Layers,
            "Display Over Other Apps",
            "To show floating controls or overlays, you must allow this app to display over other apps."
        )
        PermissionManager.SpecialPermission.NOTIFICATION_LISTENER -> Triple(
            Icons.Default.Notifications,
            "Notification Access",
            "To read, manage, or respond to notifications from other apps, you must grant notification access."
        )
        PermissionManager.SpecialPermission.ACCESSIBILITY -> Triple(
            Icons.Default.Accessibility,
            "Accessibility Service",
            "To perform global actions like pressing Back, Home, or taking screenshots, you must enable the Accessibility Service."
        )
        PermissionManager.SpecialPermission.DND_POLICY -> Triple(
            Icons.Default.DoNotDisturb,
            "Do Not Disturb Access",
            "To control Do Not Disturb mode, silent mode, or ringer settings, you must grant notification policy access."
        )
        PermissionManager.SpecialPermission.EXACT_ALARM -> Triple(
            Icons.Default.Alarm,
            "Schedule Exact Alarms",
            "To trigger automations at precise times, you must allow this app to schedule exact alarms."
        )
        PermissionManager.SpecialPermission.USAGE_STATS -> Triple(
            Icons.Default.BarChart,
            "Usage Access",
            "To detect which apps are running or recently used, you must grant usage access."
        )
    }

    PermissionRationaleDialog(
        title = title,
        description = description,
        icon = icon,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

/**
 * State holder for managing permission dialog visibility
 */
data class PermissionDialogState(
    val isVisible: Boolean = false,
    val permission: PermissionManager.SpecialPermission? = null,
    val customTitle: String? = null,
    val customDescription: String? = null
)

/**
 * Composable that manages showing permission dialogs based on state
 */
@Composable
fun PermissionDialogHandler(
    state: PermissionDialogState,
    onConfirm: (PermissionManager.SpecialPermission) -> Unit,
    onDismiss: () -> Unit
) {
    if (state.isVisible && state.permission != null) {
        if (state.customTitle != null && state.customDescription != null) {
            PermissionRationaleDialog(
                title = state.customTitle,
                description = state.customDescription,
                onConfirm = { onConfirm(state.permission) },
                onDismiss = onDismiss
            )
        } else {
            SpecialPermissionDialog(
                permission = state.permission,
                onConfirm = { onConfirm(state.permission) },
                onDismiss = onDismiss
            )
        }
    }
}

