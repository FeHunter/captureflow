package com.fehunter.captureflow.app.navigation

import com.fehunter.captureflow.core.ui.loadCoverBitmap

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.categories.presentation.components.CategorySelectionSection
import com.fehunter.captureflow.features.categories.presentation.components.CategoryFilterSection
import com.fehunter.captureflow.features.categories.presentation.components.CategoryManagementDialog
import com.fehunter.captureflow.features.fullscreen.presentation.screens.FullscreenCaptureDialog
import com.fehunter.captureflow.features.notes.presentation.components.NotesEditorDialog
import com.fehunter.captureflow.features.captures.presentation.components.CategoryCard
import com.fehunter.captureflow.features.captures.presentation.components.CategoryThumbnail
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.captures.presentation.model.CaptureImageProcessingStatus
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.imageimport.presentation.components.ImageCaptureScreen
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
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CameraAlt
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
fun CaptureLibrary(viewModel: CaptureViewModel, imageImportViewModel: ImageImportViewModel, categoriesViewModel: CategoriesViewModel, dashboardViewModel: DashboardViewModel, categorySortOrder: String, onCategorySortOrderChange: (String) -> Unit, captureSortOrder: String, imageCompressionProfile: String, onImageCompressionProfileChange: (String) -> Unit, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, showCompletedCaptures: Boolean, detectedScreenshotUri: Uri?, pendingScreenshotImportUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, onScreenshotImportHandled: () -> Unit) {
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
    var showCameraCapture by remember { mutableStateOf(false) }
    var imageCategory by remember { mutableStateOf("Ler depois") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var showCompletedInExpandedCategory by remember { mutableStateOf(false) }
    var selectedCapture by remember { mutableStateOf<Capture?>(null) }
    var notesCaptureId by remember { mutableStateOf<String?>(null) }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var sharedLinkCategory by remember { mutableStateOf<String?>(null) }
    var screenshotImportCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSharedLink?.id) { sharedLinkCategory = null }
    val categories = listOf("Todas") + categoryEntities.map { it.name }
    fun sortCaptures(items: List<Capture>): List<Capture> = when (captureSortOrder) {
        "oldest" -> items.sortedBy { it.createdAt }
        "title_asc" -> items.sortedBy { it.title.lowercase() }
        "title_desc" -> items.sortedByDescending { it.title.lowercase() }
        else -> items.sortedByDescending { it.createdAt }
    }
    val activeCategories = categoryEntities.map { it.name }
    LaunchedEffect(categoryEntities) {
        if (category != "Todas" && categoryEntities.none { it.name == category }) category = "Todas"
        if (sharedLinkCategory != null && categoryEntities.none { it.name == sharedLinkCategory }) sharedLinkCategory = null
        if (activeCategories.isNotEmpty() && imageCategory !in activeCategories) imageCategory = activeCategories.first()
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let { imageImportViewModel.prepareImageReview(it.toString(), ImageCompressionProfile.fromPreference(imageCompressionProfile)) } },
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
    val allMatchingCaptures = filterCaptures(
        captures = captures,
        query = query,
        category = category,
        showCompleted = true,
    )
    val sortedCategoryNames = remember(categoryEntities, categorySortOrder, allMatchingCaptures) {
        val names = categoryEntities.map { it.name }
        when (categorySortOrder) {
            "name_asc" -> names.sortedBy { it.lowercase() }
            "name_desc" -> names.sortedByDescending { it.lowercase() }
            "newest" -> names.sortedByDescending { name ->
                allMatchingCaptures.filter { it.category == name }.maxOfOrNull { it.createdAt }
                    ?: Long.MIN_VALUE
            }
            "oldest" -> names.sortedBy { name ->
                allMatchingCaptures.filter { it.category == name }.minOfOrNull { it.createdAt }
                    ?: Long.MAX_VALUE
            }
            else -> names
        }
    }
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
                            Text("Adicionar imagem", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Escolha uma foto ou captura de tela para extrair o texto no próprio aparelho.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showCameraCapture = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Fotografar")
                        }
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Galeria")
                        }
                    }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Compressão", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    FilterChip(
                        selected = imageCompressionProfile == "MEDIUM",
                        onClick = { onImageCompressionProfileChange("MEDIUM") },
                        label = { Text("Médio · 70%") },
                    )
                    FilterChip(
                        selected = imageCompressionProfile == "HIGH",
                        onClick = { onImageCompressionProfileChange("HIGH") },
                        label = { Text("Alto · 50%") },
                    )
                }
                }
            }
        }
        item(key = "search") { CaptureSearchField(query = query, onQueryChange = searchViewModel::updateQuery) }
        item(key = "dashboard") { DashboardSummaryCard(dashboardSummary) }
        item(key = "categories") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                var showSortMenu by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Categorias", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Box {
                        TextButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Ordenar")
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            DropdownMenuItem(text = { Text("Categorias") }, onClick = {}, enabled = false)
                            listOf("manual" to "Ordem padrão", "newest" to "Mais recentes", "oldest" to "Mais antigas", "name_asc" to "Nome · A–Z", "name_desc" to "Nome · Z–A").forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text((if (categorySortOrder == value) "✓ " else "") + label) },
                                    onClick = { onCategorySortOrderChange(value); showSortMenu = false },
                                )
                            }
                        }
                    }
                    TextButton(onClick = { showCategoryManager = true }) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Gerenciar")
                    }
                }
                CategoryFilterSection(
                    categories = categories,
                    selectedCategory = category,
                    onCategorySelected = { category = it },
                )
            }
        }
        if (allMatchingCaptures.isEmpty()) {
            item(key = "empty-state") {
                Text(
                    if (captures.isEmpty()) "Selecione uma imagem acima ou compartilhe um link, texto ou imagem de outro app para começar."
                    else "Nenhuma referência encontrada.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            if (category == "Todas" && allMatchingCaptures.none { !it.isCompleted }) {
                item(key = "completed-only-state") {
                    Text(
                        "Todas as referências encontradas estão concluídas.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val visibleCategories = if (category == "Todas") sortedCategoryNames else listOf(category)
            items(visibleCategories, key = { "category-$it" }) { tag ->
                // Category order controls these section containers only. Keep the
                // preview thumbnails in the library's source order; captureSortOrder
                // is applied inside the expanded category, not to the category preview.
                val categoryCaptures = allMatchingCaptures.filter { it.category == tag }
                if (categoryCaptures.isNotEmpty()) {
                    val previewCaptures = categoryCaptures.filterNot { it.isCompleted }
                    if (category != "Todas" || previewCaptures.isNotEmpty()) {
                        CategoryCard(
                            title = tag,
                            captures = previewCaptures,
                            totalCount = categoryCaptures.size,
                            completedCount = categoryCaptures.count { it.isCompleted },
                            ocrStates = captureImageProcessingStates,
                            onExpand = {
                                expandedCategory = tag
                                showCompletedInExpandedCategory = false
                            },
                            onOpenCapture = { selectedCapture = it },
                        )
                    }
                }
            }
        }
    }
    if (showCategoryManager) {
        CategoryManagementDialog(
            categories = categoryEntities.map { it.name },
            onAdd = categoriesViewModel::addCategory,
            onRename = categoriesViewModel::renameCategory,
            onDelete = categoriesViewModel::deleteCategory,
            onDismiss = { showCategoryManager = false },
        )
    }

    LaunchedEffect(expandedCategory) {
        showCompletedInExpandedCategory = false
    }
    val expandedCategoryCaptures = remember(allMatchingCaptures, expandedCategory, captureSortOrder) {
        expandedCategory?.let { tag -> sortCaptures(allMatchingCaptures.filter { it.category == tag }) }.orEmpty()
    }
    val expandedCategoryVisibleCaptures = remember(expandedCategoryCaptures, showCompletedInExpandedCategory) {
        expandedCategoryCaptures.filter { it.isCompleted == showCompletedInExpandedCategory }
    }
    expandedCategory?.let { tag ->
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
                            Text(
                                "${expandedCategoryCaptures.size} itens · ${expandedCategoryCaptures.count { it.isCompleted }} concluídas",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = !showCompletedInExpandedCategory,
                            onClick = { showCompletedInExpandedCategory = false },
                            label = {
                                Text(
                                    "Pendentes · ${expandedCategoryCaptures.count { !it.isCompleted }}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                        FilterChip(
                            modifier = Modifier.weight(1f),
                            selected = showCompletedInExpandedCategory,
                            onClick = { showCompletedInExpandedCategory = true },
                            label = {
                                Text(
                                    "Concluídas · ${expandedCategoryCaptures.count { it.isCompleted }}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                    if (expandedCategoryVisibleCaptures.isEmpty()) {
                        Text(
                            if (showCompletedInExpandedCategory) "Nenhuma task concluída nesta categoria."
                            else "Nenhuma task pendente nesta categoria.",
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(3),
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(expandedCategoryVisibleCaptures, key = { it.id }) { capture ->
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
    val viewerCandidates = if (expandedCategory != null) expandedCategoryVisibleCaptures else sortCaptures(filtered)
    val fullscreenCaptures = remember(viewerCandidates, selectedCapture) {
        val activeCapture = selectedCapture
        if (activeCapture != null && viewerCandidates.none { it.id == activeCapture.id }) {
            viewerCandidates + activeCapture
        } else {
            viewerCandidates
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
                current.copy(
                    isCompleted = completed,
                    previousCategory = null,
                )
            }
        },
        onTitleChange = viewModel::updateCaptureTitle,
        categories = activeCategories,
        onCategoryChange = { id, newCategory ->
            viewModel.updateCaptureCategory(id, newCategory)
            selectedCapture = selectedCapture?.takeIf { it.id == id }?.copy(category = newCategory)
        },
        onOpenNotes = { notesCaptureId = it; editingNoteId = "new" },
        onEditNote = { id, noteId -> notesCaptureId = id; editingNoteId = noteId },
        onDelete = viewModel::deleteCapture,
        allSubtasks = allSubtasks,
        onAddSubtask = viewModel::addCaptureSubtask,
        onSubtaskCompleted = viewModel::setCaptureSubtaskCompleted,
        onDeleteSubtask = viewModel::deleteCaptureSubtask,
    )


    notesCaptureId?.let { captureId ->
        val notesCapture = captures.firstOrNull { it.id == captureId }
            ?: selectedCapture?.takeIf { it.id == captureId }
        notesCapture?.let { item ->
            NotesEditorDialog(
                captureTitle = item.title,
                initialNotes = item.notes.orEmpty(),
                initialEditingNoteId = editingNoteId,
                onDismiss = { notesCaptureId = null; editingNoteId = null },
                onCompressImage = { uri, callback -> imageImportViewModel.compressNoteImage(uri, callback) },
                onDeleteTemporaryImage = imageImportViewModel::deleteTemporaryImage,
                onSave = { notes ->
                    viewModel.updateCaptureNotes(captureId, notes)
                    selectedCapture = selectedCapture?.takeIf { it.id == captureId }?.copy(
                        notes = notes.trim().takeIf(String::isNotBlank),
                    )
                    notesCaptureId = null
                    editingNoteId = null
                },
            )
        }
    }



    val pendingImageReview by imageImportViewModel.pendingImageReview.collectAsState()
    val pendingImagePreview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, pendingImageReview?.imageUri,
    ) {
        value = pendingImageReview?.imageUri?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, uri) }.getOrNull()
            }
        }
    }
    pendingImageReview?.let { pending ->
        AlertDialog(
            onDismissRequest = imageImportViewModel::cancelPendingImageReview,
            title = { Text("Revisar nova captura") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pendingImagePreview?.let { preview ->
                        Image(
                            bitmap = preview,
                            contentDescription = "Prévia da imagem capturada",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }

                    if (pendingImagePreview == null && pending.isProcessing) {
                        Box(
                            Modifier.fillMaxWidth().height(140.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    OutlinedTextField(
                        value = pending.title,
                        onValueChange = imageImportViewModel::updatePendingImageTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título da tarefa") },
                        placeholder = { Text("Digite um título") },
                        maxLines = 2,
                    )
                    CategorySelectionSection(
                        categories = activeCategories,
                        selectedCategory = pending.category,
                        onCategorySelected = imageImportViewModel::updatePendingImageCategory,
                    )
                    if (pending.isProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Analisando imagem com OCR…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    pending.error?.let {
                        Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = imageImportViewModel::confirmPendingImageReview,
                    enabled = !pending.isProcessing && pending.title.isNotBlank() && pending.category in activeCategories,
                ) { Text("Salvar tarefa") }
            },
            dismissButton = {
                TextButton(onClick = imageImportViewModel::cancelPendingImageReview) { Text("Cancelar") }
            },
        )
    }

    if (showCameraCapture) {
        Dialog(
            onDismissRequest = { showCameraCapture = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            ImageCaptureScreen(
                onImageCaptured = { uri ->
                    showCameraCapture = false
                    imageImportViewModel.prepareImageReview(uri, ImageCompressionProfile.fromPreference(imageCompressionProfile))
                },
                onClose = { showCameraCapture = false },
            )
        }
    }

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
                                compressionProfile = ImageCompressionProfile.fromPreference(imageCompressionProfile),
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
                imageImportViewModel.addImageCapture(uri.toString(), imageCategory, compressionProfile = ImageCompressionProfile.fromPreference(imageCompressionProfile))
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
