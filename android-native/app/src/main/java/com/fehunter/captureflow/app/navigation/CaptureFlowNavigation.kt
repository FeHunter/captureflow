package com.fehunter.captureflow.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModel
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.newsletters.presentation.screens.NewslettersScreen
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowApp(
    viewModel: CaptureViewModel,
    imageImportViewModel: ImageImportViewModel,
    categoriesViewModel: CategoriesViewModel,
    dashboardViewModel: DashboardViewModel,
    newslettersViewModel: NewslettersViewModel,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    categorySortOrder: String,
    onCategorySortOrderChange: (String) -> Unit,
    captureSortOrder: String,
    imageCompressionProfile: String,
    hasScreenshotAccess: Boolean,
    screenshotMonitoringEnabled: Boolean,
    detectedScreenshotUri: Uri?,
    onRequestScreenshotAccess: () -> Unit,
    onStopScreenshotMonitoring: () -> Unit,
    onScreenshotHandled: () -> Unit,
    pendingScreenshotImportUri: Uri?,
    onScreenshotImportHandled: () -> Unit,
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCompletedCaptures by remember { mutableStateOf(true) }
    var showNewsletters by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                title = {
                    Text(
                        if (showNewsletters) "Newsletters" else "Capture Flow",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    if (!showNewsletters) {
                        IconButton(onClick = { showNewsletters = true }) {
                            Icon(Icons.Default.RssFeed, contentDescription = "Abrir Newsletters")
                        }
                        IconButton(
                            onClick = {
                                if (screenshotMonitoringEnabled) onStopScreenshotMonitoring()
                                else onRequestScreenshotAccess()
                            },
                        ) {
                            Icon(
                                imageVector = if (screenshotMonitoringEnabled) {
                                    Icons.Default.Notifications
                                } else {
                                    Icons.Default.NotificationsOff
                                },
                                contentDescription = if (screenshotMonitoringEnabled) {
                                    "Desativar avisos de screenshot"
                                } else if (hasScreenshotAccess) {
                                    "Ativar avisos de screenshot"
                                } else {
                                    "Configurar avisos de screenshot"
                                },
                                tint = if (screenshotMonitoringEnabled) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                        IconButton(onClick = { showOptionsMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Opções da biblioteca")
                        }
                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(if (showCompletedCaptures) "Ocultar concluídos" else "Mostrar concluídos")
                                },
                                onClick = {
                                    showCompletedCaptures = !showCompletedCaptures
                                    showOptionsMenu = false
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Alterar tema") },
                                onClick = {
                                    showOptionsMenu = false
                                    showThemeMenu = true
                                },
                            )
                        }
                        DropdownMenu(
                            expanded = showThemeMenu,
                            onDismissRequest = { showThemeMenu = false },
                        ) {
                            listOf(
                                "system" to "Seguir dispositivo",
                                "light" to "Claro",
                                "dark" to "Escuro",
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = { Text(if (themeMode == mode) "✓ $label" else label) },
                                    onClick = {
                                        onThemeModeChange(mode)
                                        showThemeMenu = false
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding), color = Color.Transparent) {
            if (showNewsletters) {
                NewslettersScreen(
                    viewModel = newslettersViewModel,
                    onBack = { showNewsletters = false },
                )
            } else {
                CaptureLibrary(
                    viewModel = viewModel,
                    imageImportViewModel = imageImportViewModel,
                    categoriesViewModel = categoriesViewModel,
                    dashboardViewModel = dashboardViewModel,
                    categorySortOrder = categorySortOrder,
                    onCategorySortOrderChange = onCategorySortOrderChange,
                    captureSortOrder = captureSortOrder,
                    imageCompressionProfile = imageCompressionProfile,
                    hasScreenshotAccess = hasScreenshotAccess,
                    screenshotMonitoringEnabled = screenshotMonitoringEnabled,
                    showCompletedCaptures = showCompletedCaptures,
                    detectedScreenshotUri = detectedScreenshotUri,
                    pendingScreenshotImportUri = pendingScreenshotImportUri,
                    onRequestScreenshotAccess = onRequestScreenshotAccess,
                    onStopScreenshotMonitoring = onStopScreenshotMonitoring,
                    onScreenshotHandled = onScreenshotHandled,
                    onScreenshotImportHandled = onScreenshotImportHandled,
                )
            }
        }
    }
}
