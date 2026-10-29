package com.fehunter.captureflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.remember
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fehunter.captureflow.ui.CaptureFlowApp
import com.fehunter.captureflow.ui.CaptureViewModel
import com.fehunter.captureflow.services.ScreenshotNotificationService

class MainActivity : ComponentActivity() {
    private val captureViewModel: CaptureViewModel by viewModels()
    private var pendingShareIntent by mutableStateOf<Intent?>(null)
    private var detectedScreenshotUri by mutableStateOf<Uri?>(null)
    private var hasScreenshotAccess by mutableStateOf(false)
    private var screenshotMonitoringEnabled by mutableStateOf(false)

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasScreenshotAccess = granted
        if (granted) requestNotificationPermission()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) startScreenshotNotificationService()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pendingShareIntent = if (intent.action == Intent.ACTION_SEND) intent else null
        handleScreenshotAction(intent)
        setContent {
            val preferences = remember { getSharedPreferences("captureflow_preferences", MODE_PRIVATE) }
            var themeMode by remember {
                mutableStateOf(preferences.getString("theme_mode", "system") ?: "system")
            }
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            val navy = Color(0xFF07182B)
            val deepBlue = Color(0xFF0C2B46)
            val ocean = Color(0xFF164B62)
            val blue = Color(0xFF1F5F8B)
            val cyan = Color(0xFF39A6D6)
            val lightSurface = Color(0xFFF4F8FC)
            val scheme = if (isDark) darkColorScheme(
                primary = Color(0xFF63C4F0), onPrimary = Color(0xFF002033),
                secondary = Color(0xFF58B9D6), onSecondary = Color(0xFF06212C),
                tertiary = Color(0xFF8ABFE8), background = navy,
                onBackground = Color(0xFFF3F7FB), surface = deepBlue,
                onSurface = Color(0xFFF3F7FB), surfaceVariant = ocean,
                onSurfaceVariant = Color(0xFFB7CEDF), outline = Color(0xFF52758E),
            ) else lightColorScheme(
                primary = blue, onPrimary = Color.White,
                secondary = ocean, onSecondary = Color.White,
                tertiary = cyan, background = lightSurface, onBackground = navy,
                surface = Color.White, onSurface = navy,
                surfaceVariant = Color(0xFFE1EDF6),
                onSurfaceVariant = Color(0xFF3D5D73), outline = Color(0xFF7D9AAF),
            )
            SideEffect { preferences.edit().putString("theme_mode", themeMode).apply() }
            MaterialTheme(colorScheme = scheme) {
                val shared = pendingShareIntent
                LaunchedEffect(shared) {
                    if (shared?.action == Intent.ACTION_SEND) {
                        captureViewModel.receiveSharedIntent(shared)
                        pendingShareIntent = null
                    }
                }
                CaptureFlowApp(
                    viewModel = captureViewModel,
                    themeMode = themeMode,
                    onThemeModeChange = { themeMode = it },
                    hasScreenshotAccess = hasScreenshotAccess,
                    screenshotMonitoringEnabled = screenshotMonitoringEnabled,
                    detectedScreenshotUri = detectedScreenshotUri,
                    onRequestScreenshotAccess = ::requestScreenshotAccess,
                    onStopScreenshotMonitoring = ::stopScreenshotNotificationService,
                    onScreenshotHandled = ::handleScreenshotHandled,
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        hasScreenshotAccess = hasScreenshotPermission()
        val preferences = getSharedPreferences(ScreenshotNotificationService.PREFERENCES, MODE_PRIVATE)
        screenshotMonitoringEnabled = preferences.getBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false)
        if (hasScreenshotAccess && screenshotMonitoringEnabled) {
            if (hasNotificationPermission()) startScreenshotNotificationService() else {
                screenshotMonitoringEnabled = false
                preferences.edit().putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false).apply()
            }
        }
    }

    private fun screenshotPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun hasScreenshotPermission(): Boolean =
        checkSelfPermission(screenshotPermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestScreenshotAccess() {
        if (hasScreenshotPermission()) {
            hasScreenshotAccess = true
            requestNotificationPermission()
        } else {
            screenshotPermissionLauncher.launch(screenshotPermission())
        }
    }

    private fun hasNotificationPermission(): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startScreenshotNotificationService()
        }
    }

    private fun startScreenshotNotificationService() {
        if (!hasScreenshotPermission() || !hasNotificationPermission()) return
        getSharedPreferences(ScreenshotNotificationService.PREFERENCES, MODE_PRIVATE).edit()
            .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, true).apply()
        screenshotMonitoringEnabled = true
        val serviceIntent = Intent(this, ScreenshotNotificationService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) startForegroundService(serviceIntent) else startService(serviceIntent)
        } catch (_: Exception) {
            getSharedPreferences(ScreenshotNotificationService.PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false).apply()
            screenshotMonitoringEnabled = false
        }
    }

    private fun stopScreenshotNotificationService() {
        getSharedPreferences(ScreenshotNotificationService.PREFERENCES, MODE_PRIVATE).edit()
            .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false).apply()
        screenshotMonitoringEnabled = false
        startService(Intent(this, ScreenshotNotificationService::class.java).setAction(ScreenshotNotificationService.ACTION_STOP))
    }

    private fun handleScreenshotAction(intent: Intent?) {
        val uri = intent?.getStringExtra(ScreenshotNotificationService.EXTRA_SCREENSHOT_URI)?.let(Uri::parse) ?: return
        when (intent.action) {
            ScreenshotNotificationService.ACTION_CREATE_TASK -> {
                captureViewModel.addImageTask(uri)
                detectedScreenshotUri = null
            }
            ScreenshotNotificationService.ACTION_SAVE_REFERENCE -> detectedScreenshotUri = uri
        }
    }

    private fun handleScreenshotHandled() {
        detectedScreenshotUri = null
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_SEND) pendingShareIntent = intent
        handleScreenshotAction(intent)
    }
}
