package com.example.automationapp.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.example.automationapp.receiver.GeofenceBroadcastReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Manager for handling geofence operations
 * Requires FINE_LOCATION and BACKGROUND_LOCATION permissions
 */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val geofencingClient: GeofencingClient =
        LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            GEOFENCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    /**
     * Add a single geofence
     */
    suspend fun addGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float = DEFAULT_RADIUS,
        expirationDuration: Long = Geofence.NEVER_EXPIRE,
        loiteringDelay: Int = DEFAULT_LOITERING_DELAY
    ): Result<Unit> {
        return try {
            // Validate inputs
            if (!isValidGeofenceId(id)) {
                return Result.failure(IllegalArgumentException("Invalid geofence ID"))
            }
            if (!isValidCoordinates(latitude, longitude)) {
                return Result.failure(IllegalArgumentException("Invalid coordinates"))
            }
            if (!isValidRadius(radius)) {
                return Result.failure(IllegalArgumentException("Radius must be between $MIN_RADIUS and $MAX_RADIUS meters"))
            }

            // Check permissions
            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }

            if (!hasBackgroundLocationPermission()) {
                Log.w(TAG, "Background location permission not granted - geofence may not work when app is closed")
            }

            val geofence = buildGeofence(
                id = id,
                latitude = latitude,
                longitude = longitude,
                radius = radius,
                expirationDuration = expirationDuration,
                loiteringDelay = loiteringDelay
            )

            val geofencingRequest = buildGeofencingRequest(listOf(geofence))

            addGeofencesInternal(geofencingRequest)

            Log.d(TAG, "Geofence added successfully: $id")
            Result.success(Unit)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception adding geofence", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding geofence", e)
            Result.failure(e)
        }
    }

    /**
     * Add multiple geofences at once
     */
    suspend fun addGeofences(
        geofences: List<GeofenceData>
    ): Result<BatchGeofenceResult> {
        return try {
            if (geofences.isEmpty()) {
                return Result.failure(IllegalArgumentException("Geofence list cannot be empty"))
            }

            if (geofences.size > MAX_GEOFENCES) {
                return Result.failure(
                    IllegalArgumentException("Cannot add more than $MAX_GEOFENCES geofences at once")
                )
            }

            if (!hasLocationPermission()) {
                return Result.failure(SecurityException("Location permission not granted"))
            }

            val validGeofences = mutableListOf<Geofence>()
            val failedIds = mutableListOf<String>()

            geofences.forEach { data ->
                try {
                    if (isValidGeofenceData(data)) {
                        validGeofences.add(
                            buildGeofence(
                                id = data.id,
                                latitude = data.latitude,
                                longitude = data.longitude,
                                radius = data.radius,
                                expirationDuration = data.expirationDuration,
                                loiteringDelay = data.loiteringDelay
                            )
                        )
                    } else {
                        failedIds.add(data.id)
                    }
                } catch (e: Exception) {
                    failedIds.add(data.id)
                    Log.e(TAG, "Failed to create geofence: ${data.id}", e)
                }
            }

            if (validGeofences.isEmpty()) {
                return Result.failure(IllegalArgumentException("No valid geofences to add"))
            }

            val geofencingRequest = buildGeofencingRequest(validGeofences)
            addGeofencesInternal(geofencingRequest)

            Log.d(TAG, "Added ${validGeofences.size} geofences, ${failedIds.size} failed")

            Result.success(
                BatchGeofenceResult(
                    successCount = validGeofences.size,
                    failedIds = failedIds
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error adding geofences", e)
            Result.failure(e)
        }
    }

    /**
     * Remove a single geofence by ID
     */
    suspend fun removeGeofence(id: String): Result<Unit> {
        return try {
            removeGeofencesInternal(listOf(id))
            Log.d(TAG, "Geofence removed: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofence", e)
            Result.failure(e)
        }
    }

    /**
     * Remove multiple geofences by IDs
     */
    suspend fun removeGeofences(ids: List<String>): Result<Unit> {
        return try {
            if (ids.isEmpty()) {
                return Result.failure(IllegalArgumentException("ID list cannot be empty"))
            }

            removeGeofencesInternal(ids)
            Log.d(TAG, "Removed ${ids.size} geofences")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing geofences", e)
            Result.failure(e)
        }
    }

    /**
     * Remove all geofences
     */
    suspend fun removeAllGeofences(): Result<Unit> {
        return try {
            suspendCancellableCoroutine { continuation ->
                geofencingClient.removeGeofences(geofencePendingIntent)
                    .addOnSuccessListener {
                        Log.d(TAG, "All geofences removed")
                        continuation.resume(Result.success(Unit))
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to remove all geofences", exception)
                        continuation.resume(Result.failure(exception))
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error removing all geofences", e)
            Result.failure(e)
        }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if background location permission is granted (Android 10+)
     */
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Background location is included in fine location for Android 9 and below
            true
        }
    }

    /**
     * Get required permissions for geofencing
     */
    fun getRequiredPermissions(): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        } else {
            listOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    // Private helper methods

    private fun buildGeofence(
        id: String,
        latitude: Double,
        longitude: Double,
        radius: Float,
        expirationDuration: Long,
        loiteringDelay: Int
    ): Geofence {
        return Geofence.Builder()
            .setRequestId(id)
            .setCircularRegion(latitude, longitude, radius)
            .setExpirationDuration(expirationDuration)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or
                        Geofence.GEOFENCE_TRANSITION_EXIT or
                        Geofence.GEOFENCE_TRANSITION_DWELL
            )
            .setLoiteringDelay(loiteringDelay)
            .build()
    }

    private fun buildGeofencingRequest(geofences: List<Geofence>): GeofencingRequest {
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofences(geofences)
            .build()
    }

    private suspend fun addGeofencesInternal(request: GeofencingRequest): Unit =
        suspendCancellableCoroutine { continuation ->
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                continuation.resume(Unit)
                return@suspendCancellableCoroutine
            }

            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    throw exception
                }
        }

    private suspend fun removeGeofencesInternal(ids: List<String>): Unit =
        suspendCancellableCoroutine { continuation ->
            geofencingClient.removeGeofences(ids)
                .addOnSuccessListener {
                    continuation.resume(Unit)
                }
                .addOnFailureListener { exception ->
                    throw exception
                }
        }

    // Validation methods

    private fun isValidGeofenceId(id: String): Boolean {
        return id.isNotBlank() && id.length <= MAX_ID_LENGTH
    }

    private fun isValidCoordinates(latitude: Double, longitude: Double): Boolean {
        return latitude in -90.0..90.0 && longitude in -180.0..180.0
    }

    private fun isValidRadius(radius: Float): Boolean {
        return radius in MIN_RADIUS..MAX_RADIUS
    }

    private fun isValidGeofenceData(data: GeofenceData): Boolean {
        return isValidGeofenceId(data.id) &&
                isValidCoordinates(data.latitude, data.longitude) &&
                isValidRadius(data.radius)
    }

    companion object {
        private const val TAG = "GeofenceManager"
        private const val GEOFENCE_REQUEST_CODE = 1001
        private const val DEFAULT_RADIUS = 200f
        private const val MIN_RADIUS = 100f
        private const val MAX_RADIUS = 10000f
        private const val DEFAULT_LOITERING_DELAY = 60000 // 1 minute
        private const val MAX_GEOFENCES = 100 // Google Play Services limit
        private const val MAX_ID_LENGTH = 100
    }
}

/**
 * Data class for geofence configuration
 */
data class GeofenceData(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float = 200f,
    val expirationDuration: Long = Geofence.NEVER_EXPIRE,
    val loiteringDelay: Int = 60000
)

/**
 * Result of batch geofence operation
 */
data class BatchGeofenceResult(
    val successCount: Int,
    val failedIds: List<String>
) {
    val failureCount: Int get() = failedIds.size
    val allSuccessful: Boolean get() = failedIds.isEmpty()

    val summary: String
        get() = "Added $successCount geofences successfully" +
                if (failureCount > 0) ", $failureCount failed" else ""
}
