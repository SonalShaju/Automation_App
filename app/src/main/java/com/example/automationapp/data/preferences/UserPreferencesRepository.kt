package com.example.automationapp.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Extension property for DataStore
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

/**
 * Repository for managing user preferences using DataStore
 */
@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object PreferencesKeys {
        val IS_SETUP_COMPLETE = booleanPreferencesKey("is_setup_complete")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("has_seen_onboarding")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
    }

    /**
     * Flow that emits whether the initial setup/onboarding is complete
     */
    val isSetupComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] ?: false
        }

    /**
     * Flow that emits whether user has seen onboarding
     */
    val hasSeenOnboarding: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] ?: false
        }

    /**
     * Mark the onboarding/setup as complete
     */
    suspend fun completeSetup() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = true
            preferences[PreferencesKeys.HAS_SEEN_ONBOARDING] = true
        }
    }

    /**
     * Reset setup status (for testing or re-onboarding)
     */
    suspend fun resetSetup() {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_SETUP_COMPLETE] = false
        }
    }

    /**
     * Check if notifications are enabled in preferences
     */
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] ?: true
        }

    /**
     * Set notifications preference
     */
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    /**
     * Check if dark mode is enabled
     */
    val darkModeEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] ?: false
        }

    /**
     * Set dark mode preference
     */
    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DARK_MODE_ENABLED] = enabled
        }
    }
}

