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
import com.fehunter.captureflow.core.ui.theme.CaptureFlowTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fehunter.captureflow.app.navigation.CaptureFlowApp
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.settings.presentation.viewmodel.SettingsViewModel
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModel
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel
import com.fehunter.captureflow.features.backup.presentation.viewmodel.BackupViewModel
import com.fehunter.captureflow.features.share.presentation.viewmodel.CaptureShareViewModel
import com.fehunter.captureflow.features.imageimport.data.android.ScreenshotNotificationService
import com.fehunter.captureflow.app.di.AppContainer
import com.fehunter.captureflow.features.imageimport.data.android.SharedContentIntentParser

class MainActivity : ComponentActivity() {
    private val appContainer by lazy { AppContainer(application) }
    private val captureViewModel: CaptureViewModel by viewModels { appContainer.captureViewModelFactory }
    private val imageImportViewModel: ImageImportViewModel by viewModels { appContainer.imageImportViewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { appContainer.settingsViewModelFactory }
    private val categoriesViewModel: CategoriesViewModel by viewModels { appContainer.categoriesViewModelFactory }
    private val dashboardViewModel: DashboardViewModel by viewModels { appContainer.dashboardViewModelFactory }
    private val newslettersViewModel: NewslettersViewModel by viewModels { appContainer.newslettersViewModelFactory }
    private val backupViewModel: BackupViewModel by viewModels { appContainer.backupViewModelFactory }
    private val shareViewModel: CaptureShareViewModel by viewModels { appContainer.shareViewModelFactory }
    private var pendingShareIntent by mutableStateOf<Intent?>(null)
    private var detectedScreenshotUri by mutableStateOf<Uri?>(null)
    private var pendingScreenshotImportUri by mutableStateOf<Uri?>(null)
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
        pendingShareIntent = intent.takeIf {
            it.action == Intent.ACTION_SEND || it.action == Intent.ACTION_SEND_MULTIPLE
        }
        handleScreenshotAction(intent)
        setContent {
            val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
            val categorySortOrder by settingsViewModel.categorySortOrder.collectAsStateWithLifecycle()
            val captureSortOrder by settingsViewModel.captureSortOrder.collectAsStateWithLifecycle()
            val imageCompressionProfile by settingsViewModel.imageCompressionProfile.collectAsStateWithLifecycle()
            val isDark = when (themeMode) {
                "dark" -> true
                "light" -> false
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            CaptureFlowTheme(darkTheme = isDark) {
                val shared = pendingShareIntent
                LaunchedEffect(shared) {
                    if (shared?.action == Intent.ACTION_SEND || shared?.action == Intent.ACTION_SEND_MULTIPLE) {
                        SharedContentIntentParser.parse(shared)?.let { incoming ->
                            imageImportViewModel.receiveSharedContent(incoming.content, incoming.imageUri, ImageCompressionProfile.fromPreference(imageCompressionProfile))
                        }
                        pendingShareIntent = null
                    }
                }
                CaptureFlowApp(
                    viewModel = captureViewModel,
                    imageImportViewModel = imageImportViewModel,
                    categoriesViewModel = categoriesViewModel,
                    dashboardViewModel = dashboardViewModel,
                    newslettersViewModel = newslettersViewModel,
                    backupViewModel = backupViewModel,
                    shareViewModel = shareViewModel,
                    themeMode = themeMode,
                    onThemeModeChange = settingsViewModel::setThemeMode,
                    categorySortOrder = categorySortOrder,
                    onCategorySortOrderChange = settingsViewModel::setCategorySortOrder,
                    captureSortOrder = captureSortOrder,
                    imageCompressionProfile = imageCompressionProfile,
                    hasScreenshotAccess = hasScreenshotAccess,
                    screenshotMonitoringEnabled = screenshotMonitoringEnabled,
                    detectedScreenshotUri = detectedScreenshotUri,
                    pendingScreenshotImportUri = pendingScreenshotImportUri,
                    onScreenshotImportHandled = { pendingScreenshotImportUri = null },
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
            ScreenshotNotificationService.ACTION_IMPORT_SCREENSHOT -> {
                pendingScreenshotImportUri = uri
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
        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            pendingShareIntent = intent
        }
        handleScreenshotAction(intent)
    }
}
