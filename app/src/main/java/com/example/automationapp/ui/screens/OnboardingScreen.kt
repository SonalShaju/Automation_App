package com.example.automationapp.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.automationapp.ui.viewmodel.OnboardingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = hiltViewModel(),
    onOnboardingComplete: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permissionStates by viewModel.permissionStates.collectAsState()
    val canProceed by viewModel.canProceed.collectAsState()
    val isSetupComplete by viewModel.isSetupComplete.collectAsState()

    // Navigate when setup is complete
    LaunchedEffect(isSetupComplete) {
        if (isSetupComplete) {
            onOnboardingComplete()
        }
    }

    // Refresh permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionStates()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Helper to check battery optimization
    fun isBatteryOptimizationExempt(): Boolean {
        val powerManager = context.getSystemService(android.content.Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // App Icon/Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.tertiary
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(50.dp),
                    tint = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Welcome Title
            Text(
                text = "Welcome to Automation",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Let's set up some permissions so your automations can work seamlessly in the background.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Permission Items
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Section Title
                    Text(
                        text = "Required Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Notification Access (Critical)
                    PermissionItem(
                        icon = Icons.Default.Notifications,
                        title = "Notification Access",
                        description = "Read and manage notifications from other apps",
                        isGranted = permissionStates.hasNotificationListenerPermission,
                        isCritical = true,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                            )
                        }
                    )

                    HorizontalDivider()

                    // Accessibility Service (Critical)
                    PermissionItem(
                        icon = Icons.Default.Accessibility,
                        title = "Accessibility Service",
                        description = "Perform global actions like Home, Back, and app detection",
                        isGranted = permissionStates.hasAccessibilityPermission,
                        isCritical = true,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            )
                        }
                    )

                    HorizontalDivider()

                    // Write Settings
                    PermissionItem(
                        icon = Icons.Default.Brightness6,
                        title = "Modify System Settings",
                        description = "Change brightness, screen timeout, and display settings",
                        isGranted = permissionStates.hasWriteSettingsPermission,
                        isCritical = false,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )

                    HorizontalDivider()

                    // DND Policy
                    PermissionItem(
                        icon = Icons.Default.DoNotDisturb,
                        title = "Do Not Disturb Access",
                        description = "Control Do Not Disturb and ringer mode",
                        isGranted = permissionStates.hasDndPolicyPermission,
                        isCritical = false,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                            )
                        }
                    )

                    HorizontalDivider()

                    // Battery Optimization
                    PermissionItem(
                        icon = Icons.Default.BatteryChargingFull,
                        title = "Ignore Battery Optimizations",
                        description = "Run reliably in the background without being killed",
                        isGranted = isBatteryOptimizationExempt(),
                        isCritical = false,
                        onClick = {
                            // Open app settings where user can manually adjust battery optimization
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )

                    HorizontalDivider()

                    // Location
                    PermissionItem(
                        icon = Icons.Default.LocationOn,
                        title = "Location Access",
                        description = "Trigger automations based on your location",
                        isGranted = permissionStates.hasLocationPermission,
                        isCritical = false,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Permissions marked with ★ are required to continue. You can grant other permissions later from Settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.height(24.dp))

            // Get Started Button
            Button(
                onClick = {
                    viewModel.completeOnboarding()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = canProceed,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (canProceed) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium
                    )
                } else {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Grant Required Permissions",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Skip option (for testing, can be removed in production)
            TextButton(
                onClick = {
                    viewModel.completeOnboarding()
                }
            ) {
                Text(
                    text = "Skip for now",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Reusable Permission Item Component
 */
@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    isCritical: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isGranted) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        },
        label = "backgroundColorAnimation"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Text Content
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isCritical) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "★",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 12.sp
                        )
                    }
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Status Icon
            if (isGranted) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Grant",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

