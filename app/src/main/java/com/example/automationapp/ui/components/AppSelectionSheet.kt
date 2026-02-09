package com.example.automationapp.ui.components

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AppSelectionSheet"

/**
 * Lightweight data class for app info with optional icon.
 * Icons are loaded lazily to prevent OOM issues.
 */
data class AppInfoWithIcon(
    val name: String,
    val packageName: String,
    var icon: Drawable? = null
)

/**
 * A Modal Bottom Sheet for selecting apps.
 *
 * Why Bottom Sheet instead of Dropdown?
 * - Dropdown tries to render all items which causes OOM with 100+ apps
 * - LazyColumn inside Dropdown has measurement issues
 * - Bottom Sheet provides full-screen scrollable list with proper lazy loading
 * - Icons are only loaded for visible items, cached in memory
 *
 * Performance Optimizations:
 * - Apps are loaded on Dispatchers.IO
 * - Only user apps shown (filtered system apps)
 * - Icons loaded lazily per-item when visible
 * - Search filters locally without reloading
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionBottomSheet(
    isVisible: Boolean,
    selectedPackageName: String?,
    onAppSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // App loading state
    var apps by remember { mutableStateOf<List<AppInfoWithIcon>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }

    // Icon cache - prevents reloading icons when scrolling
    val iconCache = remember { mutableStateMapOf<String, Drawable?>() }

    // Load apps on first show
    LaunchedEffect(isVisible) {
        if (isVisible && apps.isEmpty()) {
            isLoading = true
            apps = loadInstalledApps(context.packageManager)
            isLoading = false
            Log.d(TAG, "Loaded ${apps.size} apps")
        }
    }

    // Filter apps by search query
    val filteredApps = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) {
            apps
        } else {
            apps.filter { app ->
                app.name.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    if (isVisible) {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select App",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    placeholder = { Text("Search apps...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { /* Already filtering */ })
                )

                // Loading indicator
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredApps.isEmpty()) {
                    // Empty state
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Apps,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No apps match \"$searchQuery\""
                                       else "No apps found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    // App List
                    val listState = rememberLazyListState()

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(bottom = 32.dp)
                    ) {
                        items(
                            items = filteredApps,
                            key = { it.packageName }
                        ) { app ->
                            AppListItem(
                                app = app,
                                isSelected = app.packageName == selectedPackageName,
                                iconCache = iconCache,
                                onClick = {
                                    onAppSelected(app.packageName)
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single app item in the list.
 * Icon is loaded lazily when the item becomes visible.
 */
@Composable
private fun AppListItem(
    app: AppInfoWithIcon,
    isSelected: Boolean,
    iconCache: MutableMap<String, Drawable?>,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    // Load icon lazily when item becomes visible
    LaunchedEffect(app.packageName) {
        if (!iconCache.containsKey(app.packageName)) {
            withContext(Dispatchers.IO) {
                try {
                    val icon = context.packageManager.getApplicationIcon(app.packageName)
                    iconCache[app.packageName] = icon
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to load icon for ${app.packageName}", e)
                    iconCache[app.packageName] = null
                }
            }
        }
    }

    val icon = iconCache[app.packageName]

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap(96, 96).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                } else {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Icon(
                            Icons.Default.Apps,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // App Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Selected indicator
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }

    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    )
}

/**
 * Read-only text field that opens the app selection sheet when clicked.
 * This replaces the problematic ExposedDropdownMenuBox.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionField(
    selectedPackageName: String?,
    onAppSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select App"
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }

    // Get app name for display
    var selectedAppName by remember(selectedPackageName) { mutableStateOf("") }
    var selectedAppIcon by remember(selectedPackageName) { mutableStateOf<Drawable?>(null) }

    // Load selected app info
    LaunchedEffect(selectedPackageName) {
        if (selectedPackageName != null) {
            withContext(Dispatchers.IO) {
                try {
                    val appInfo = context.packageManager.getApplicationInfo(
                        selectedPackageName,
                        PackageManager.GET_META_DATA
                    )
                    selectedAppName = context.packageManager.getApplicationLabel(appInfo).toString()
                    selectedAppIcon = context.packageManager.getApplicationIcon(selectedPackageName)
                } catch (e: Exception) {
                    selectedAppName = selectedPackageName
                    selectedAppIcon = null
                }
            }
        } else {
            selectedAppName = ""
            selectedAppIcon = null
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = selectedAppName,
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = {
                if (selectedAppIcon != null) {
                    Image(
                        bitmap = selectedAppIcon!!.toBitmap(48, 48).asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Apps,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            placeholder = { Text("Tap to select an app") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showSheet = true },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )
    }

    // Bottom Sheet
    AppSelectionBottomSheet(
        isVisible = showSheet,
        selectedPackageName = selectedPackageName,
        onAppSelected = onAppSelected,
        onDismiss = { showSheet = false }
    )
}

/**
 * Load installed user apps on IO thread.
 * Filters out system apps to reduce memory usage.
 */
private suspend fun loadInstalledApps(
    packageManager: PackageManager
): List<AppInfoWithIcon> = withContext(Dispatchers.IO) {
    try {
        packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
            .asSequence()
            .filter { appInfo ->
                // Filter: only user apps OR updated system apps (like Google apps)
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem = (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                !isSystem || isUpdatedSystem
            }
            .map { appInfo ->
                val label = packageManager.getApplicationLabel(appInfo).toString()
                    .ifBlank { appInfo.packageName }
                AppInfoWithIcon(
                    name = label,
                    packageName = appInfo.packageName,
                    icon = null // Icon loaded lazily per-item
                )
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to load apps", e)
        emptyList()
    }
}

