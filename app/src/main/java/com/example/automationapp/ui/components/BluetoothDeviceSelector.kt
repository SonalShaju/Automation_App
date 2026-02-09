package com.example.automationapp.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat

/**
 * Data class for paired Bluetooth device info
 */
data class BluetoothDeviceInfo(
    val name: String,
    val macAddress: String
)

/**
 * Bluetooth Device Selector Composable
 *
 * Shows a dropdown/modal list of paired Bluetooth devices.
 * User can select a device and the selection returns the device name.
 *
 * Requires BLUETOOTH_CONNECT permission on Android 12+
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothDeviceSelector(
    selectedDeviceName: String?,
    onDeviceSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select Bluetooth Device"
) {
    val context = LocalContext.current
    var showSheet by remember { mutableStateOf(false) }
    var pairedDevices by remember { mutableStateOf<List<BluetoothDeviceInfo>>(emptyList()) }
    var hasPermission by remember { mutableStateOf(checkBluetoothPermission(context)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Permission launcher for Android 12+
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            pairedDevices = getPairedBluetoothDevices(context)
            showSheet = true
        } else {
            errorMessage = "Bluetooth permission required"
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        OutlinedTextField(
            value = selectedDeviceName ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = false,
            leadingIcon = {
                Icon(
                    if (selectedDeviceName != null) Icons.Default.BluetoothConnected
                    else Icons.Default.Bluetooth,
                    contentDescription = null,
                    tint = if (selectedDeviceName != null) MaterialTheme.colorScheme.primary
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            placeholder = { Text("Tap to select a paired device") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    if (hasPermission) {
                        pairedDevices = getPairedBluetoothDevices(context)
                        if (pairedDevices.isEmpty()) {
                            errorMessage = "No paired devices found. Pair a device in Bluetooth settings first."
                        } else {
                            showSheet = true
                        }
                    } else {
                        // Request permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            permissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                        } else {
                            // On older Android versions, try to get devices directly
                            pairedDevices = getPairedBluetoothDevices(context)
                            if (pairedDevices.isEmpty()) {
                                errorMessage = "No paired devices found"
                            } else {
                                showSheet = true
                            }
                        }
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

    // Bottom Sheet for device selection
    if (showSheet) {
        BluetoothDeviceBottomSheet(
            devices = pairedDevices,
            selectedDeviceName = selectedDeviceName,
            onDeviceSelected = { device ->
                onDeviceSelected(device.name)
                errorMessage = null
                showSheet = false
            },
            onDismiss = { showSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BluetoothDeviceBottomSheet(
    devices: List<BluetoothDeviceInfo>,
    selectedDeviceName: String?,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Paired Bluetooth Devices",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "${devices.size} device(s) found",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No paired devices",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(devices, key = { it.macAddress }) { device ->
                        BluetoothDeviceItem(
                            device = device,
                            isSelected = device.name == selectedDeviceName,
                            onClick = { onDeviceSelected(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BluetoothDeviceItem(
    device: BluetoothDeviceInfo,
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
            Icon(
                Icons.Default.Bluetooth,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = device.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = device.macAddress,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
 * Check if Bluetooth permission is granted
 */
private fun checkBluetoothPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.BLUETOOTH_CONNECT
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        // On older versions, BLUETOOTH permission is granted at install time
        true
    }
}

/**
 * Get list of paired Bluetooth devices
 */
@SuppressLint("MissingPermission")
private fun getPairedBluetoothDevices(context: Context): List<BluetoothDeviceInfo> {
    return try {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            return emptyList()
        }

        bluetoothAdapter.bondedDevices
            ?.mapNotNull { device ->
                val name = device.name ?: "Unknown Device"
                val address = device.address ?: return@mapNotNull null
                BluetoothDeviceInfo(name = name, macAddress = address)
            }
            ?.sortedBy { it.name.lowercase() }
            ?: emptyList()
    } catch (e: Exception) {
        android.util.Log.e("BluetoothSelector", "Failed to get paired devices", e)
        emptyList()
    }
}

