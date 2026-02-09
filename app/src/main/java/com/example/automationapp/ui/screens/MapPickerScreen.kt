package com.example.automationapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView

/**
 * Data class representing selected location coordinates
 */
data class SelectedLocation(
    val latitude: Double,
    val longitude: Double
)

private const val TAG = "MapPickerScreen"

/**
 * Premium Map Picker Screen using OpenStreetMap (osmdroid)
 *
 * Features:
 * - Full-screen MapView with hidden zoom buttons
 * - Fixed center pin (Uber/Google Maps style)
 * - My Location FAB with GPS integration
 * - Smooth animations and premium UI
 * - Proper lifecycle handling for battery efficiency
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerScreen(
    initialLatitude: Double = 0.0,
    initialLongitude: Double = 0.0,
    onLocationSelected: (SelectedLocation) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // Map state
    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentLatitude by remember { mutableDoubleStateOf(initialLatitude) }
    var currentLongitude by remember { mutableDoubleStateOf(initialLongitude) }
    var isLocating by remember { mutableStateOf(false) }
    var showLocationError by remember { mutableStateOf(false) }

    // Location permission state
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            scope.launch {
                getCurrentLocation(context) { lat, lon ->
                    mapView?.controller?.animateTo(GeoPoint(lat, lon), 18.0, 1000L)
                    isLocating = false
                }
            }
        } else {
            isLocating = false
            showLocationError = true
        }
    }

    // Initialize osmdroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Lifecycle handling for MapView - prevents battery drain
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    mapView?.onResume()
                    Log.d(TAG, "MapView resumed")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView?.onPause()
                    Log.d(TAG, "MapView paused")
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
            Log.d(TAG, "MapView detached")
        }
    }

    // Poll map center for coordinate display
    LaunchedEffect(mapView) {
        while (true) {
            mapView?.let { map ->
                currentLatitude = map.mapCenter.latitude
                currentLongitude = map.mapCenter.longitude
            }
            delay(100)
        }
    }

    // Auto-dismiss error snackbar
    LaunchedEffect(showLocationError) {
        if (showLocationError) {
            delay(3000)
            showLocationError = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Full-screen OpenStreetMap View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                MapView(ctx).apply {
                    setTileSource(TileSourceFactory.MAPNIK)
                    setMultiTouchControls(true)
                    zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                    @Suppress("DEPRECATION")
                    setBuiltInZoomControls(false)

                    val startPoint = if (initialLatitude != 0.0 || initialLongitude != 0.0) {
                        GeoPoint(initialLatitude, initialLongitude)
                    } else {
                        GeoPoint(20.5937, 78.9629) // Default center
                    }
                    controller.setZoom(15.0)
                    controller.setCenter(startPoint)
                    mapView = this
                }
            },
            update = { view ->
                mapView = view
            }
        )

        // Fixed Center Pin - The map moves, pin stays fixed
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .offset(y = 2.dp)
                    .size(12.dp)
                    .shadow(4.dp, CircleShape)
                    .background(Color.Black.copy(alpha = 0.2f), CircleShape)
            )
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Selected Location",
                modifier = Modifier
                    .size(56.dp)
                    .offset(y = (-28).dp),
                tint = MaterialTheme.colorScheme.error
            )
        }

        // Top Bar with back button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledTonalIconButton(
                onClick = onNavigateBack,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(12.dp))
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 2.dp
            ) {
                Text(
                    text = "Pick Location",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }

        // My Location FAB
        FloatingActionButton(
            onClick = {
                if (hasLocationPermission) {
                    isLocating = true
                    scope.launch {
                        getCurrentLocation(context) { lat, lon ->
                            mapView?.controller?.animateTo(GeoPoint(lat, lon), 18.0, 1000L)
                            isLocating = false
                        }
                    }
                } else {
                    isLocating = true
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 160.dp)
                .navigationBarsPadding(),
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Icon(Icons.Default.MyLocation, contentDescription = "My Location")
            }
        }

        // Bottom Panel - Coordinates + Confirm Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                shadowElevation = 4.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Selected Coordinates",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Latitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.6f".format(currentLatitude), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                        VerticalDivider(modifier = Modifier.height(32.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Longitude", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("%.6f".format(currentLongitude), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    onLocationSelected(SelectedLocation(latitude = currentLatitude, longitude = currentLongitude))
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
        }

        // Error Snackbar
        AnimatedVisibility(
            visible = showLocationError,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp)
        ) {
            Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.errorContainer, shadowElevation = 4.dp) {
                Text(
                    text = "Location permission required",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * Get current device location using FusedLocationProviderClient
 */
@SuppressLint("MissingPermission")
private fun getCurrentLocation(
    context: Context,
    onLocationReceived: (Double, Double) -> Unit
) {
    try {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        val cancellationToken = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cancellationToken.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                onLocationReceived(location.latitude, location.longitude)
                Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
            } else {
                fusedLocationClient.lastLocation.addOnSuccessListener { lastLocation ->
                    if (lastLocation != null) {
                        onLocationReceived(lastLocation.latitude, lastLocation.longitude)
                    } else {
                        Log.w(TAG, "Could not get location")
                    }
                }
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Failed to get location", e)
        }
    } catch (e: Exception) {
        Log.e(TAG, "Error getting location", e)
    }
}

/**
 * Dialog version of Map Picker for use within other screens
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerDialog(
    initialLatitude: Double = 0.0,
    initialLongitude: Double = 0.0,
    onDismiss: () -> Unit,
    onLocationSelected: (SelectedLocation) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var mapView by remember { mutableStateOf<MapView?>(null) }
    var currentLatitude by remember { mutableDoubleStateOf(initialLatitude) }
    var currentLongitude by remember { mutableDoubleStateOf(initialLongitude) }
    var isLocating by remember { mutableStateOf(false) }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasLocationPermission = isGranted
        if (isGranted) {
            scope.launch {
                getCurrentLocation(context) { lat, lon ->
                    mapView?.controller?.animateTo(GeoPoint(lat, lon), 18.0, 1000L)
                    isLocating = false
                }
            }
        } else {
            isLocating = false
        }
    }

    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView?.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView?.onPause()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView?.onDetach()
        }
    }

    LaunchedEffect(mapView) {
        while (true) {
            mapView?.let { map ->
                currentLatitude = map.mapCenter.latitude
                currentLongitude = map.mapCenter.longitude
            }
            delay(100)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text("Pick Location")
            }
        },
        text = {
            Column {
                Box(modifier = Modifier.fillMaxWidth().height(300.dp)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            MapView(ctx).apply {
                                setTileSource(TileSourceFactory.MAPNIK)
                                setMultiTouchControls(true)
                                zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
                                @Suppress("DEPRECATION")
                                setBuiltInZoomControls(false)
                                val startPoint = if (initialLatitude != 0.0 || initialLongitude != 0.0) {
                                    GeoPoint(initialLatitude, initialLongitude)
                                } else {
                                    GeoPoint(20.5937, 78.9629)
                                }
                                controller.setZoom(15.0)
                                controller.setCenter(startPoint)
                                mapView = this
                            }
                        },
                        update = { view -> mapView = view }
                    )

                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.align(Alignment.Center).size(40.dp).offset(y = (-20).dp),
                        tint = MaterialTheme.colorScheme.error
                    )

                    FilledTonalIconButton(
                        onClick = {
                            if (hasLocationPermission) {
                                isLocating = true
                                scope.launch {
                                    getCurrentLocation(context) { lat, lon ->
                                        mapView?.controller?.animateTo(GeoPoint(lat, lon), 18.0, 1000L)
                                        isLocating = false
                                    }
                                }
                            } else {
                                isLocating = true
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        },
                        modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                    ) {
                        if (isLocating) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.MyLocation, contentDescription = "My Location")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lat", style = MaterialTheme.typography.labelSmall)
                            Text("%.5f".format(currentLatitude), style = MaterialTheme.typography.bodyMedium)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lng", style = MaterialTheme.typography.labelSmall)
                            Text("%.5f".format(currentLongitude), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onLocationSelected(SelectedLocation(currentLatitude, currentLongitude)) }) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

