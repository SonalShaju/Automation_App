package com.example.automationapp.ui.components

import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val TAG = "AppSelectionDropdown"

/**
 * App selection component using Modal Bottom Sheet.
 *
 * This replaces the old ExposedDropdownMenu approach which caused OOM crashes
 * when loading 100+ app icons. The Bottom Sheet uses LazyColumn which only
 * renders visible items, preventing memory issues.
 *
 * @param selectedPackageName Currently selected app package name
 * @param onAppSelected Callback when an app is selected
 * @param modifier Modifier for the text field
 * @param label Label text shown above the field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectionDropdown(
    selectedPackageName: String?,
    onAppSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Select App"
) {
    // Delegate to the new AppSelectionField which uses Bottom Sheet
    AppSelectionField(
        selectedPackageName = selectedPackageName,
        onAppSelected = onAppSelected,
        modifier = modifier,
        label = label
    )
}
