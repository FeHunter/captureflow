package com.fehunter.captureflow.app.navigation

import com.fehunter.captureflow.core.ui.loadCoverBitmap

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.categories.presentation.components.CategorySelectionSection
import com.fehunter.captureflow.features.categories.presentation.components.CategoryManagementDialog
import com.fehunter.captureflow.features.fullscreen.presentation.screens.FullscreenCaptureDialog
import com.fehunter.captureflow.features.captures.presentation.components.CategoryCard
import com.fehunter.captureflow.features.captures.presentation.components.CategoryThumbnail
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.captures.presentation.model.CaptureImageProcessingStatus
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.search.presentation.components.CaptureSearchField
import com.fehunter.captureflow.features.search.presentation.viewmodel.SearchViewModel
import com.fehunter.captureflow.features.search.domain.usecase.FilterCapturesUseCase
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModel
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.fehunter.captureflow.features.dashboard.presentation.components.DashboardSummaryCard
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun CaptureLibrary(viewModel: CaptureViewModel, imageImportViewModel: ImageImportViewModel, categoriesViewModel: CategoriesViewModel, dashboardViewModel: DashboardViewModel, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, showCompletedCaptures: Boolean, detectedScreenshotUri: Uri?, pendingScreenshotImportUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, onScreenshotImportHandled: () -> Unit) {
    val context = LocalContext.current
    val captures by viewModel.captures.collectAsStateWithLifecycle()
    val ocrStates by imageImportViewModel.ocrStates.collectAsState()
    val captureImageProcessingStates = remember(ocrStates) {
        ocrStates.mapValues { (_, state) ->
            CaptureImageProcessingStatus(isProcessing = state.isProcessing, isError = state.isError)
        }
    }
    val pendingSharedLink by imageImportViewModel.pendingSharedLink.collectAsState()
    val categoryEntities by categoriesViewModel.categories.collectAsState()
    val dashboardSummary by dashboardViewModel.summary.collectAsState()
    val allSubtasks by viewModel.captureSubtasks.collectAsState()
    val searchViewModel: SearchViewModel = viewModel()
    val query by searchViewModel.query.collectAsState()
    var category by remember { mutableStateOf("Todas") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCategory by remember { mutableStateOf("Ler depois") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var selectedCapture by remember { mutableStateOf<Capture?>(null) }
    var sharedLinkCategory by remember { mutableStateOf<String?>(null) }
    var screenshotImportCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSharedLink?.id) { sharedLinkCategory = null }
    val categories = listOf("Todas") + categoryEntities.map { it.name }
    val activeCategories = categoryEntities.map { it.name }.filterNot { it == "Concluído" }
    LaunchedEffect(categoryEntities) {
        if (category != "Todas" && categoryEntities.none { it.name == category }) category = "Todas"
        if (sharedLinkCategory != null && categoryEntities.none { it.name == sharedLinkCategory }) sharedLinkCategory = null
        if (activeCategories.isNotEmpty() && imageCategory !in activeCategories) imageCategory = activeCategories.first()
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri },
    )
    LaunchedEffect(detectedScreenshotUri) {
        detectedScreenshotUri?.let { selectedImageUri = it }
    }
    var screenshotToConfirm by remember { mutableStateOf(pendingScreenshotImportUri) }
    LaunchedEffect(pendingScreenshotImportUri) {
        screenshotToConfirm = pendingScreenshotImportUri
    }
    LaunchedEffect(screenshotToConfirm) { screenshotImportCategory = null }
    val screenshotPreview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, screenshotToConfirm,
    ) {
        value = screenshotToConfirm?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, uri.toString()) }.getOrNull()
            }
        }
    }
    val filterCaptures = remember { FilterCapturesUseCase() }
    val filtered = filterCaptures(
        captures = captures,
        query = query,
        category = category,
        showCompleted = showCompletedCaptures,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "image-picker") {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
            )) {
                Column(
                    Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Analisar imagem do dispositivo", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Escolha uma foto ou captura de tela para extrair o texto no próprio aparelho.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Button(
                        onClick = {
                            imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Selecionar imagem")
                    }
                }
            }
        }
        item(key = "search") { CaptureSearchField(query = query, onQueryChange = searchViewModel::updateQuery) }
        item(key = "dashboard") { DashboardSummaryCard(dashboardSummary) }
        item(key = "categories") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Categorias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showCategoryManager = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gerenciar")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { item ->
                        FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
                    }
                }
            }
        }
        if (filtered.isEmpty()) {
            item(key = "empty-state") {
                Text(
                    if (captures.isEmpty()) "Selecione uma imagem acima ou compartilhe um link, texto ou imagem de outro app para começar."
                    else "Nenhuma referência encontrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            val visibleCategories = if (category == "Todas") categoryEntities.map { it.name } else listOf(category)
            items(visibleCategories, key = { "category-$it" }) { tag ->
                val categoryCaptures = filtered.filter { it.category == tag }
                if (categoryCaptures.isNotEmpty()) {
                    CategoryCard(
                        title = tag,
                        captures = categoryCaptures,
                        ocrStates = captureImageProcessingStates,
                        onExpand = { expandedCategory = tag },
                        onOpenCapture = { selectedCapture = it },
                    )
                }
            }
        }
    }
    if (showCategoryManager) {
        CategoryManagementDialog(
            categories = categoryEntities.map { it.name },
            fixedCategories = setOf("Concluído"),
            onAdd = categoriesViewModel::addCategory,
            onRename = categoriesViewModel::renameCategory,
            onDelete = categoriesViewModel::deleteCategory,
            onDismiss = { showCategoryManager = false },
        )
    }

    expandedCategory?.let { tag ->
        val categoryCaptures = filtered.filter { it.category == tag }
        Dialog(
            onDismissRequest = { expandedCategory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
        ) {
            BackHandler { expandedCategory = null }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = { expandedCategory = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Voltar à biblioteca")
                        }
                        Column(Modifier.weight(1f)) {
                            Text(tag, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                            Text("${categoryCaptures.size} itens", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (categoryCaptures.isEmpty()) {
                        Text("Nenhum item nesta categoria.", modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(categoryCaptures, key = { it.id }) { capture ->
                                CategoryThumbnail(
                                    capture = capture,
                                    ocrState = captureImageProcessingStates[capture.id] ?: CaptureImageProcessingStatus(),
                                    onClick = { selectedCapture = capture },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    val fullscreenCaptures = remember(filtered, selectedCapture) {
        val activeCapture = selectedCapture
        if (activeCapture != null && filtered.none { it.id == activeCapture.id }) {
            filtered + activeCapture
        } else {
            filtered
        }
    }
    FullscreenCaptureDialog(
        capture = selectedCapture,
        captures = fullscreenCaptures,
        onDismiss = { selectedCapture = null },
        onCaptureSelected = { selectedCapture = it },
        onCompleted = { id, completed ->
            viewModel.setCaptureCompleted(id, completed)
            selectedCapture = selectedCapture?.takeIf { it.id == id }?.let { current ->
                if (completed) {
                    current.copy(
                        isCompleted = true,
                        previousCategory = if (current.category != "Concluído") current.category else current.previousCategory,
                        category = "Concluído",
                    )
                } else {
                    current.copy(
                        isCompleted = false,
                        category = current.previousCategory ?: "Salvar",
                        previousCategory = null,
                    )
                }
            }
        },
        onTitleChange = viewModel::updateCaptureTitle,
        onNotesChange = viewModel::updateCaptureNotes,
        onDelete = viewModel::deleteCapture,
        allSubtasks = allSubtasks,
        onAddSubtask = viewModel::addCaptureSubtask,
        onSubtaskCompleted = viewModel::setCaptureSubtaskCompleted,
        onDeleteSubtask = viewModel::deleteCaptureSubtask,
    )


    screenshotToConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                screenshotToConfirm = null
                onScreenshotImportHandled()
            },
            title = { Text("Adicionar screenshot às referências?") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("A captura foi aberta pela notificação. Escolha em qual categoria deseja salvá-la.")
                    screenshotPreview?.let { preview ->
                        Image(
                            bitmap = preview,
                            contentDescription = "Prévia da screenshot que será salva como referência",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                    CategorySelectionSection(
                        categories = activeCategories,
                        selectedCategory = screenshotImportCategory,
                        onCategorySelected = { screenshotImportCategory = it },
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        screenshotImportCategory?.let { selectedCategory ->
                            imageImportViewModel.addImageCapture(
                                uri.toString(),
                                category = selectedCategory,
                                deleteSourceScreenshotAfterImport = true,
                            )
                            screenshotToConfirm = null
                            onScreenshotImportHandled()
                        }
                    },
                    enabled = screenshotImportCategory != null,
                ) { Text("Salvar referência") }
            },
            dismissButton = {
                TextButton(onClick = {
                    screenshotToConfirm = null
                    onScreenshotImportHandled()
                }) { Text("Cancelar") }
            },
        )
    }

    pendingSharedLink?.let { pending ->
        AlertDialog(
            onDismissRequest = { imageImportViewModel.cancelPendingSharedLink(); sharedLinkCategory = null },
            title = { Text("Salvar link como referência") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategorySelectionSection(
                        categories = activeCategories,
                        selectedCategory = sharedLinkCategory,
                        onCategorySelected = { sharedLinkCategory = it },
                    )
                    OutlinedTextField(
                        value = pending.title,
                        onValueChange = imageImportViewModel::updatePendingSharedLinkTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título principal") },
                        placeholder = { Text("Digite um título") },
                        maxLines = 2,
                        singleLine = false,
                    )
                    Text(
                        pending.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (pending.isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Buscando informações do link…", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (!pending.metadataFound) {
                        Text("Não foi possível obter o título automaticamente.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        sharedLinkCategory?.let(imageImportViewModel::confirmPendingSharedLink)
                        sharedLinkCategory = null
                    },
                    enabled = !pending.isLoading && sharedLinkCategory != null && pending.title.isNotBlank(),
                ) { Text("Salvar referência") }
            },
            dismissButton = {
                TextButton(onClick = {
                    imageImportViewModel.cancelPendingSharedLink()
                    sharedLinkCategory = null
                }) { Text("Cancelar") }
            },
        )
    }

    selectedImageUri?.let { uri ->
        AlertDialog(onDismissRequest = {
            selectedImageUri = null
            if (uri == detectedScreenshotUri) onScreenshotHandled()
        }, title = { Text(if (uri == detectedScreenshotUri) "Screenshot detectada" else "Vincular imagem a uma tag") }, text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(if (uri == detectedScreenshotUri) {
                    "Uma nova captura de tela foi encontrada. Deseja enviá-la ao CaptureFlow?"
                } else {
                    "Escolha a categoria para organizar esta imagem."
                })
                CategorySelectionSection(
                    categories = activeCategories,
                    selectedCategory = imageCategory,
                    onCategorySelected = { imageCategory = it },
                )
            }
        }, confirmButton = {
            Button(onClick = {
                imageImportViewModel.addImageCapture(uri.toString(), imageCategory)
                selectedImageUri = null
                if (uri == detectedScreenshotUri) onScreenshotHandled()
            }) { Text("Enviar para o app") }
        }, dismissButton = {
            TextButton(onClick = {
                selectedImageUri = null
                if (uri == detectedScreenshotUri) onScreenshotHandled()
            }) { Text("Agora não") }
        })
    }
}
