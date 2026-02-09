package com.example.automationapp

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.automationapp.service.RuleSchedulingManager
import com.example.automationapp.service.TriggerMonitorService
import com.example.automationapp.ui.navigation.AppNavigation
import com.example.automationapp.ui.theme.AutomationAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var ruleSchedulingManager: RuleSchedulingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Force start the TriggerMonitorService for reliable time trigger detection
        forceStartTriggerMonitorService()

        // Initialize scheduling for all rules when app starts
        lifecycleScope.launch {
            ruleSchedulingManager.initializeScheduling()
        }

        enableEdgeToEdge()
        setContent {
            AutomationAppTheme {
                    AppNavigation()

            }
        }
    }

    /**
     * Force start the TriggerMonitorService to ensure time triggers work reliably.
     * This is especially important on Android 14+ where background service restrictions are stricter.
     */
    private fun forceStartTriggerMonitorService() {
        try {
            Log.d(TAG, "Force starting TriggerMonitorService...")
            val serviceIntent = Intent(this, TriggerMonitorService::class.java).apply {
                action = TriggerMonitorService.ACTION_START
            }

            // Use startForegroundService for Android O+ (API 26+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(this, serviceIntent)
            } else {
                startService(serviceIntent)
            }
            Log.d(TAG, "TriggerMonitorService start requested successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start TriggerMonitorService", e)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}

