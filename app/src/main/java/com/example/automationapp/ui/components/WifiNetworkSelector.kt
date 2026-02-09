package com.example.automationapp.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

/**
 * Data class for WiFi network info
 */
data class WifiNetworkInfo(
    val ssid: String,
    val signalStrength: Int, // 0-4 bars
    val isSecure: Boolean,
    val isCurrentNetwork: Boolean = false
)

/**
 * WiFi Network Selector Composable
 *
 * Shows a dropdown/modal list of nearby WiFi networks.
 * Includes "Scan for Networks" button and shows current connected network.
 *
 * Requires ACCESS_FINE_LOCATION (for WiFi scanning) and ACCESS_WIFI_STATE
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiNetworkSelector(
    selectedSsid: String?,
    onNetworkSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select WiFi Network"
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var networks by remember { mutableStateOf<List<WifiNetworkInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(checkWifiPermissions(context)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        hasPermission = allGranted
        if (allGranted) {
            showSheet = true
        } else {
            errorMessage = "Location permission required to scan WiFi networks"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = selectedSsid ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = {
                Icon(
                    if (selectedSsid != null) Icons.Default.WifiPassword else Icons.Default.Wifi,
                    contentDescription = null,
                    tint = if (selectedSsid != null) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = { Text("Tap to select a WiFi network") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hasPermission) {
                        showSheet = true
                    } else {
                        // Request permissions
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_WIFI_STATE,
                                Manifest.permission.CHANGE_WIFI_STATE
                            )
                        )
                    }
                },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Error message
        errorMessage?.let { error ->
            Text(
                text = error,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    // Bottom Sheet for network selection
    if (showSheet) {
        WifiNetworkBottomSheet(
            context = context,
            selectedSsid = selectedSsid,
            onNetworkSelected = { network ->
                onNetworkSelected(network.ssid)
                errorMessage = null
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WifiNetworkBottomSheet(
    context: Context,
    selectedSsid: String?,
    onNetworkSelected: (WifiNetworkInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var networks by remember { mutableStateOf<List<WifiNetworkInfo>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var scanError by remember { mutableStateOf<String?>(null) }

    // Get current connected network on open
    LaunchedEffect(Unit) {
        val currentNetwork = getCurrentWifiNetwork(context)
        if (currentNetwork != null) {
            networks = listOf(currentNetwork)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "WiFi Networks",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Scan button
            FilledTonalButton(
                onClick = {
                    isScanning = true
                    scanError = null
                    scanForWifiNetworks(context) { results, error ->
                        isScanning = false
                        if (error != null) {
                            scanError = error
                        } else {
                            networks = results
                        }
                    }
                },
                enabled = !isScanning,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning...")
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan for Networks")
                }
            }

            // Error message
            scanError?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (networks.isEmpty() && !isScanning) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.WifiFind,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Tap 'Scan for Networks' to find nearby WiFi",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                Text(
                    text = "${networks.size} network(s) found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(networks, key = { it.ssid }) { network ->
                        WifiNetworkItem(
                            network = network,
                            isSelected = network.ssid == selectedSsid,
                            onClick = { onNetworkSelected(network) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WifiNetworkItem(
    network: WifiNetworkInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Signal strength icon
            Icon(
                imageVector = when (network.signalStrength) {
                    0 -> Icons.Default.SignalWifi0Bar
                    1 -> Icons.Default.NetworkWifi1Bar
                    2 -> Icons.Default.NetworkWifi2Bar
                    3 -> Icons.Default.NetworkWifi3Bar
                    else -> Icons.Default.SignalWifi4Bar
                },
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = network.ssid,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (network.isCurrentNetwork) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "Connected",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (network.isSecure) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Secured",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Secured",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Open",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Check if WiFi permissions are granted
 */
private fun checkWifiPermissions(context: Context): Boolean {
    val locationPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    val wifiStatePermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_WIFI_STATE
    ) == PackageManager.PERMISSION_GRANTED

    return locationPermission && wifiStatePermission
}

/**
 * Get current connected WiFi network
 */
@Suppress("DEPRECATION")
private fun getCurrentWifiNetwork(context: Context): WifiNetworkInfo? {
    return try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val connectionInfo = wifiManager.connectionInfo

        if (connectionInfo != null && connectionInfo.networkId != -1) {
            var ssid = connectionInfo.ssid
            // Remove quotes from SSID if present
            if (ssid.startsWith("\"") && ssid.endsWith("\"")) {
                ssid = ssid.substring(1, ssid.length - 1)
            }

            if (ssid.isNotBlank() && ssid != "<unknown ssid>") {
                WifiNetworkInfo(
                    ssid = ssid,
                    signalStrength = WifiManager.calculateSignalLevel(connectionInfo.rssi, 5),
                    isSecure = true, // Assume secured if connected
                    isCurrentNetwork = true
                )
            } else null
        } else null
    } catch (e: Exception) {
        android.util.Log.e("WifiSelector", "Failed to get current network", e)
        null
    }
}

/**
 * Scan for nearby WiFi networks
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private fun scanForWifiNetworks(
    context: Context,
    onResult: (List<WifiNetworkInfo>, String?) -> Unit
) {
    @SuppressLint("MissingPermission")
    fun getScanResults(wifiManager: WifiManager, currentNetwork: WifiNetworkInfo?): List<WifiNetworkInfo> {
        return wifiManager.scanResults
            .filter { it.SSID.isNotBlank() }
            .distinctBy { it.SSID }
            .map { result ->
                WifiNetworkInfo(
                    ssid = result.SSID,
                    signalStrength = WifiManager.calculateSignalLevel(result.level, 5),
                    isSecure = result.capabilities.contains("WPA") ||
                               result.capabilities.contains("WEP") ||
                               result.capabilities.contains("PSK"),
                    isCurrentNetwork = currentNetwork?.ssid == result.SSID
                )
            }
            .sortedByDescending { it.signalStrength }
    }

    try {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        if (!wifiManager.isWifiEnabled) {
            onResult(emptyList(), "WiFi is disabled. Please enable WiFi to scan.")
            return
        }

        // Get current network first
        val currentNetwork = getCurrentWifiNetwork(context)

        // Register receiver for scan results
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                try {
                    context.unregisterReceiver(this)
                } catch (_: Exception) {
                    // Already unregistered
                }

                val scanResults = getScanResults(wifiManager, currentNetwork)

                // Add current network at top if not in scan results
                val finalResults = if (currentNetwork != null &&
                    scanResults.none { it.ssid == currentNetwork.ssid }) {
                    listOf(currentNetwork) + scanResults
                } else {
                    scanResults
                }

                onResult(finalResults, null)
            }
        }

        val intentFilter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(receiver, intentFilter)

        val scanStarted = wifiManager.startScan()
        if (!scanStarted) {
            try {
                context.unregisterReceiver(receiver)
            } catch (_: Exception) {}

            // Return cached results if scan couldn't start
            val cachedResults = getScanResults(wifiManager, currentNetwork)

            if (cachedResults.isNotEmpty()) {
                val finalResults = if (currentNetwork != null &&
                    cachedResults.none { it.ssid == currentNetwork.ssid }) {
                    listOf(currentNetwork) + cachedResults
                } else {
                    cachedResults
                }
                onResult(finalResults, null)
            } else {
                onResult(
                    if (currentNetwork != null) listOf(currentNetwork) else emptyList(),
                    "Could not scan. Showing cached results."
                )
            }
        }
    } catch (e: Exception) {
        android.util.Log.e("WifiSelector", "WiFi scan failed", e)
        onResult(emptyList(), "Failed to scan: ${e.message}")
    }
}

