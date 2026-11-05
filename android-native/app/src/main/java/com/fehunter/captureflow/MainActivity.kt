package com.fehunter.captureflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.fehunter.captureflow.app.di.AppContainer
import com.fehunter.captureflow.app.navigation.CaptureFlowApp
import com.fehunter.captureflow.core.ui.theme.CaptureFlowTheme
import com.fehunter.captureflow.features.backup.presentation.viewmodel.BackupViewModel
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModel
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.imageimport.data.android.ScreenshotNotificationService
import com.fehunter.captureflow.features.imageimport.data.android.SharedContentIntentParser
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel
import com.fehunter.captureflow.features.settings.presentation.viewmodel.SettingsViewModel
import com.fehunter.captureflow.features.share.presentation.viewmodel.CaptureShareViewModel

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
    private var pendingScreenshotImportUri by mutableStateOf<Uri?>(null)
    private var detectedScreenshotUri by mutableStateOf<Uri?>(null)
    private var hasScreenshotAccess by mutableStateOf(false)
    private var screenshotMonitoringEnabled by mutableStateOf(false)

    private val screenshotPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        hasScreenshotAccess = hasScreenshotPermission()
        if (hasScreenshotAccess) {
            requestNotificationPermission()
        } else {
            Toast.makeText(
                this,
                "A detecção contínua exige acesso completo às imagens. Você ainda pode adicionar imagens pela Galeria.",
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) startScreenshotMonitoring()
        else {
            screenshotMonitoringEnabled = false
            screenshotPreferences().edit().putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false).apply()
            Toast.makeText(this, "A permissão de notificações é necessária para manter o monitoramento visível.", Toast.LENGTH_LONG).show()
        }
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
                            imageImportViewModel.receiveSharedContent(
                                incoming.content,
                                incoming.imageUri,
                                ImageCompressionProfile.fromPreference(imageCompressionProfile),
                            )
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
                    onRequestScreenshotAccess = ::requestScreenshotAccess,
                    onStopScreenshotMonitoring = ::stopScreenshotMonitoring,
                    onScreenshotHandled = ::handleScreenshotHandled,
                    onScreenshotImportHandled = { pendingScreenshotImportUri = null },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        hasScreenshotAccess = hasScreenshotPermission()
        screenshotMonitoringEnabled = screenshotPreferences()
            .getBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false)
        if (screenshotMonitoringEnabled && (!hasScreenshotAccess || !hasNotificationPermission())) {
            stopScreenshotMonitoring()
        }
    }

    override fun onStart() {
        super.onStart()
        hasScreenshotAccess = hasScreenshotPermission()
        screenshotMonitoringEnabled = screenshotPreferences()
            .getBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false)

        if (screenshotMonitoringEnabled) {
            if (hasScreenshotAccess && hasNotificationPermission()) {
                startScreenshotService()
            } else {
                stopScreenshotMonitoring()
            }
        }
    }

    private fun screenshotPreferences() =
        getSharedPreferences(ScreenshotNotificationService.PREFERENCES, MODE_PRIVATE)

    private fun screenshotPermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private fun hasScreenshotPermission(): Boolean =
        checkSelfPermission(screenshotPermission()) == PackageManager.PERMISSION_GRANTED

    private fun hasNotificationPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestScreenshotAccess() {
        if (!hasScreenshotPermission()) {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                arrayOf(
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
            } else {
                arrayOf(screenshotPermission())
            }
            screenshotPermissionLauncher.launch(permissions)
            return
        }
        hasScreenshotAccess = true
        requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startScreenshotMonitoring()
        }
    }

    private fun startScreenshotMonitoring() {
        if (!hasScreenshotPermission() || !hasNotificationPermission()) return
        screenshotPreferences().edit()
            .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, true)
            .putLong(ScreenshotNotificationService.PREF_LAST_SCAN_SECONDS, System.currentTimeMillis() / 1_000L - 3L)
            .apply()
        screenshotMonitoringEnabled = true
        startScreenshotService()
    }

    private fun startScreenshotService() {
        if (!hasScreenshotPermission() || !hasNotificationPermission()) {
            stopScreenshotMonitoring()
            return
        }
        runCatching {
            ContextCompat.startForegroundService(
                this,
                Intent(this, ScreenshotNotificationService::class.java),
            )
        }.onFailure {
            screenshotPreferences().edit()
                .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false)
                .apply()
            screenshotMonitoringEnabled = false
            Toast.makeText(this, "Não foi possível iniciar a detecção de screenshots.", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopScreenshotMonitoring() {
        screenshotPreferences().edit()
            .putBoolean(ScreenshotNotificationService.PREF_MONITOR_ENABLED, false)
            .apply()
        screenshotMonitoringEnabled = false
        stopService(Intent(this, ScreenshotNotificationService::class.java))
    }

    private fun handleScreenshotAction(source: Intent?) {
        when (source?.action) {
            ScreenshotNotificationService.ACTION_STOP -> stopScreenshotMonitoring()
            ScreenshotNotificationService.ACTION_IMPORT_SCREENSHOT -> {
                source.getStringExtra(ScreenshotNotificationService.EXTRA_SCREENSHOT_URI)
                    ?.let(Uri::parse)
                    ?.let { pendingScreenshotImportUri = it }
            }
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
