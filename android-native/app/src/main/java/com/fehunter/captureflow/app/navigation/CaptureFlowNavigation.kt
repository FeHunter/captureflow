package com.fehunter.captureflow.app.navigation

import android.net.Uri
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.RssFeed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fehunter.captureflow.features.settings.presentation.screens.PrivacyPolicyScreen
import com.fehunter.captureflow.features.settings.presentation.screens.TutorialsScreen
import com.fehunter.captureflow.features.backup.presentation.screens.BackupScreen
import com.fehunter.captureflow.features.backup.presentation.viewmodel.BackupViewModel
import com.fehunter.captureflow.features.share.presentation.viewmodel.CaptureShareViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowApp(
    viewModel: CaptureViewModel,
    imageImportViewModel: ImageImportViewModel,
    categoriesViewModel: CategoriesViewModel,
    dashboardViewModel: DashboardViewModel,
    newslettersViewModel: NewslettersViewModel,
    backupViewModel: BackupViewModel,
    shareViewModel: CaptureShareViewModel,
    themeMode: String,
    onThemeModeChange: (String) -> Unit,
    categorySortOrder: String,
    onCategorySortOrderChange: (String) -> Unit,
    captureSortOrder: String,
    imageCompressionProfile: String,
    hasScreenshotAccess: Boolean,
    screenshotMonitoringEnabled: Boolean,
    detectedScreenshotUri: Uri?,
    pendingScreenshotImportUri: Uri?,
    onRequestScreenshotAccess: () -> Unit,
    onStopScreenshotMonitoring: () -> Unit,
    onScreenshotHandled: () -> Unit,
    onScreenshotImportHandled: () -> Unit,
) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }
    var showTutorials by remember { mutableStateOf(false) }
    var showNewsletters by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    var showScreenshotRationale by remember { mutableStateOf(false) }

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
                        when { showNewsletters -> "Newsletters"; showBackup -> "Backup e restauração"; else -> "Capture Flow" },
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    if (!showNewsletters && !showBackup) {
                        IconButton(onClick = { showNewsletters = true }) {
                            Icon(Icons.Default.RssFeed, contentDescription = "Abrir Newsletters")
                        }
                        IconButton(
                            onClick = {
                                if (screenshotMonitoringEnabled) onStopScreenshotMonitoring()
                                else showScreenshotRationale = true
                            },
                        ) {
                            Icon(
                                imageVector = if (screenshotMonitoringEnabled) {
                                    Icons.Default.PhotoCamera
                                } else {
                                    Icons.Default.PhotoCamera
                                },
                                contentDescription = if (screenshotMonitoringEnabled) {
                                    "Desativar detecção de screenshots"
                                } else if (hasScreenshotAccess) {
                                    "Ativar detecção de screenshots"
                                } else {
                                    "Configurar detecção de screenshots"
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
                                text = { Text("Privacidade") },
                                onClick = {
                                    showOptionsMenu = false
                                    showPrivacyPolicy = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Tutoriais") },
                                onClick = {
                                    showOptionsMenu = false
                                    showTutorials = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Backup e restauração") },
                                onClick = {
                                    showOptionsMenu = false
                                    showBackup = true
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
            if (showBackup) {
                BackupScreen(viewModel = backupViewModel, onBack = { showBackup = false })
            } else if (showNewsletters) {
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
                    shareViewModel = shareViewModel,
                    categorySortOrder = categorySortOrder,
                    onCategorySortOrderChange = onCategorySortOrderChange,
                    captureSortOrder = captureSortOrder,
                    imageCompressionProfile = imageCompressionProfile,
                    hasScreenshotAccess = hasScreenshotAccess,
                    screenshotMonitoringEnabled = screenshotMonitoringEnabled,
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

    if (showScreenshotRationale) {
        AlertDialog(
            onDismissRequest = { showScreenshotRationale = false },
            title = { Text("Sobre a detecção de screenshots") },
            text = {
                Text("Para identificar novas screenshots, o CaptureFlow consulta metadados recentes do MediaStore (como nome, pasta e data) enquanto o app está aberto e em primeiro plano. Essa função exige acesso completo às imagens e notificações. O acesso limitado a fotos selecionadas não é suficiente para detectar novas screenshots em todos os aplicativos. Enquanto ativada, uma notificação persistente informa que o monitoramento está em execução, inclusive quando você usa outros aplicativos. O CaptureFlow consulta metadados do MediaStore; não captura a tela, não usa Acessibilidade e não envia imagens a servidores. Cada captura gera uma notificação para você revisar. Nada é salvo no caderno até sua confirmação. Você pode interromper o monitoramento pela notificação ou pelo cabeçalho do app. A seleção manual pelo Photo Picker continua disponível.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showScreenshotRationale = false
                        onRequestScreenshotAccess()
                    },
                ) { Text("Continuar") }
            },
            dismissButton = {
                TextButton(onClick = { showScreenshotRationale = false }) { Text("Agora não") }
            },
        )
    }
    if (showPrivacyPolicy) {
        PrivacyPolicyScreen(onBack = { showPrivacyPolicy = false })
    }
    if (showTutorials) {
        TutorialsScreen(onBack = { showTutorials = false })
    }
}
