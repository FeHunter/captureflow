package com.fehunter.captureflow.app.navigation

import com.fehunter.captureflow.core.ui.loadCoverBitmap
import com.fehunter.captureflow.core.ui.theme.CaptureFlowBackdrop

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.categories.presentation.components.CategorySelectionSection
import com.fehunter.captureflow.features.categories.presentation.components.CategoryFilterSection
import com.fehunter.captureflow.features.categories.presentation.components.CategoryManagementDialog
import com.fehunter.captureflow.features.fullscreen.presentation.screens.FullscreenCaptureDialog
import com.fehunter.captureflow.features.notes.presentation.components.NotesEditorDialog
import com.fehunter.captureflow.features.captures.presentation.components.CategoryCard
import com.fehunter.captureflow.features.captures.presentation.components.CategoryThumbnail
import com.fehunter.captureflow.features.captures.presentation.components.CreateTaskDialog
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModel
import com.fehunter.captureflow.features.captures.presentation.model.CaptureImageProcessingStatus
import com.fehunter.captureflow.features.imageimport.presentation.viewmodel.ImageImportViewModel
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.imageimport.presentation.components.ImageCaptureScreen
import com.fehunter.captureflow.features.imageimport.presentation.components.ImageCropReviewScreen
import com.fehunter.captureflow.features.search.presentation.components.CaptureSearchField
import com.fehunter.captureflow.features.search.presentation.viewmodel.SearchViewModel
import com.fehunter.captureflow.features.search.domain.usecase.FilterCapturesUseCase
import com.fehunter.captureflow.features.categories.presentation.viewmodel.CategoriesViewModel
import com.fehunter.captureflow.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.fehunter.captureflow.features.share.presentation.viewmodel.CaptureShareViewModel
import com.fehunter.captureflow.features.share.presentation.screens.CaptureShareDialog
import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import com.fehunter.captureflow.features.share.domain.model.CaptureShareNote
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import com.fehunter.captureflow.features.dashboard.presentation.components.DashboardSummaryCard
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle
import com.fehunter.captureflow.features.newsletters.presentation.components.NewsletterHomeSection
import com.fehunter.captureflow.features.newsletters.presentation.screens.NewsletterArticleReader
import com.fehunter.captureflow.features.newsletters.presentation.viewmodel.NewslettersViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


@Composable
fun CaptureLibrary(viewModel: CaptureViewModel, imageImportViewModel: ImageImportViewModel, categoriesViewModel: CategoriesViewModel, dashboardViewModel: DashboardViewModel, newslettersViewModel: NewslettersViewModel, onOpenNewsletters: () -> Unit, shareViewModel: CaptureShareViewModel, categorySortOrder: String, onCategorySortOrderChange: (String) -> Unit, captureSortOrder: String, imageCompressionProfile: String, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, detectedScreenshotUri: Uri?, pendingScreenshotImportUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, onScreenshotImportHandled: () -> Unit) {
    val context = LocalContext.current
    val captures by viewModel.captures.collectAsStateWithLifecycle()
    val coverUpdateError by viewModel.coverUpdateError.collectAsStateWithLifecycle()
    val libraryListState = rememberLazyListState()
    val showFloatingActions by remember(libraryListState) {
        androidx.compose.runtime.derivedStateOf {
            libraryListState.firstVisibleItemIndex > 0 || libraryListState.firstVisibleItemScrollOffset > 96
        }
    }
    LaunchedEffect(coverUpdateError) {
        coverUpdateError?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            viewModel.clearCoverUpdateError()
        }
    }
    val ocrStates by imageImportViewModel.ocrStates.collectAsStateWithLifecycle()
    val captureImageProcessingStates = remember(ocrStates) {
        ocrStates.mapValues { (_, state) ->
            CaptureImageProcessingStatus(isProcessing = state.isProcessing, isError = state.isError)
        }
    }
    val pendingSharedLink by imageImportViewModel.pendingSharedLink.collectAsStateWithLifecycle()
    val categoryEntities by categoriesViewModel.categories.collectAsStateWithLifecycle()
    val dashboardSummary by dashboardViewModel.summary.collectAsStateWithLifecycle()
    val allSubtasks by viewModel.captureSubtasks.collectAsStateWithLifecycle()
    val captureMedia by viewModel.captureMedia.collectAsStateWithLifecycle()
    val searchViewModel: SearchViewModel = viewModel()
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    var category by remember { mutableStateOf("Todas") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var coverCropSourceUri by remember { mutableStateOf<String?>(null) }
    var coverCropTargetId by remember { mutableStateOf<String?>(null) }
    var showCameraCapture by remember { mutableStateOf(false) }
    var showCreateTaskDialog by remember { mutableStateOf(false) }
    var pendingDeleteCapture by remember { mutableStateOf<Capture?>(null) }
    var deletingCaptureIds by remember { mutableStateOf(emptySet<String>()) }
    val uiScope = rememberCoroutineScope()
    var imageCategory by remember { mutableStateOf("Ler depois") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var showCompletedInExpandedCategory by remember { mutableStateOf(false) }
    var selectedCapture by remember { mutableStateOf<Capture?>(null) }
    var shareCapture by remember { mutableStateOf<Capture?>(null) }
    var selectedNewsletterArticle by remember { mutableStateOf<NewsletterArticle?>(null) }
    var notesCaptureId by remember { mutableStateOf<String?>(null) }
    var editingNoteId by remember { mutableStateOf<String?>(null) }
    var showNotesCameraCapture by remember { mutableStateOf(false) }
    var notesCameraImageUri by remember { mutableStateOf<String?>(null) }
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
    val coverImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null && coverCropTargetId != null) coverCropSourceUri = uri.toString()
        },
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
                runCatching { loadCoverBitmap(context, uri.toString(), maxDimension = 480) }.getOrNull()
            }
        }
    }
    val filterCaptures = remember { FilterCapturesUseCase() }
    val allMatchingCaptures = remember(captures, query, category) {
        filterCaptures(
            captures = captures,
            query = query,
            category = category,
            showCompleted = true,
        )
    }
    val capturesByCategory = remember(allMatchingCaptures) {
        allMatchingCaptures.groupBy { it.category }
    }
    val sortedCategoryNames = remember(categoryEntities, categorySortOrder, allMatchingCaptures) {
        val names = categoryEntities.map { it.name }
        val newestCaptureAt = names.associateWith { name ->
            capturesByCategory[name]?.maxOfOrNull { it.createdAt }
        }
        val oldestCaptureAt = names.associateWith { name ->
            capturesByCategory[name]?.minOfOrNull { it.createdAt }
        }
        when (categorySortOrder) {
            "name_asc" -> names.sortedBy { it.lowercase() }
            "name_desc" -> names.sortedByDescending { it.lowercase() }
            // Compare actual task timestamps, not category insertion order. The second
            // timestamp is a deterministic tie-breaker when categories share the same
            // newest/oldest task time.
            "newest" -> names.sortedWith(
                compareByDescending<String> { newestCaptureAt[it] ?: Long.MIN_VALUE }
                    .thenByDescending { oldestCaptureAt[it] ?: Long.MIN_VALUE }
                    .thenBy { names.indexOf(it) },
            )
            "oldest" -> names.sortedWith(
                compareBy<String> { oldestCaptureAt[it] ?: Long.MAX_VALUE }
                    .thenBy { newestCaptureAt[it] ?: Long.MAX_VALUE }
                    .thenBy { names.indexOf(it) },
            )
            else -> names
        }
    }
    Box(Modifier.fillMaxSize()) {
        CaptureFlowBackdrop(Modifier.matchParentSize())
        LazyColumn(
        state = libraryListState,
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "image-picker") {
            Card(Modifier.fillMaxWidth(), shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
            )) {
                Column(
                    Modifier.fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Adicionar ao caderno", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            Text("Guarde uma imagem ou registre uma tarefa em texto.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { showCameraCapture = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 9.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(19.dp))
                                Text("Fotografar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                        OutlinedButton(
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 9.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(19.dp))
                                Text("Galeria", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                        FilledTonalButton(
                            onClick = { showCreateTaskDialog = true },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 3.dp, vertical = 9.dp),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(19.dp))
                                Text("Nova tarefa", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }
        item(key = "dashboard") { DashboardSummaryCard(dashboardSummary) }
        item(key = "newsletter-preview") {
            NewsletterHomeSection(
                viewModel = newslettersViewModel,
                onOpenArticle = { article ->
                    newslettersViewModel.markRead(article.id, true)
                    selectedNewsletterArticle = article
                },
                onOpenNewsletters = onOpenNewsletters,
            )
        }
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
        item(key = "search") { CaptureSearchField(query = query, onQueryChange = searchViewModel::updateQuery) }
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
            items(visibleCategories, key = { "category-$it" }, contentType = { "category-card" }) { tag ->
                // Category order controls these section containers only. Keep the
                // preview thumbnails in the library's source order; captureSortOrder
                // is applied inside the expanded category, not to the category preview.
                val categoryCaptures = capturesByCategory[tag].orEmpty()
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
        AnimatedVisibility(
            visible = showFloatingActions,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 12.dp),
            enter = fadeIn(tween(160)) + scaleIn(initialScale = 0.96f, animationSpec = tween(160)),
            exit = fadeOut(tween(120)),
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 10.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = { showCameraCapture = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Text("Fotografar", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(
                        onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Text("Galeria", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    TextButton(onClick = { showCreateTaskDialog = true }, contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text("Nova tarefa", style = MaterialTheme.typography.labelSmall)
                        }
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

    if (showCreateTaskDialog) {
        CreateTaskDialog(
            categories = activeCategories,
            onDismiss = { showCreateTaskDialog = false },
            onCreate = { title, selectedCategory ->
                viewModel.createTask(title, selectedCategory)
                showCreateTaskDialog = false
            },
        )
    }

    LaunchedEffect(expandedCategory) {
        showCompletedInExpandedCategory = false
    }
    LaunchedEffect(deletingCaptureIds, captures) {
        val removedIds = deletingCaptureIds.filter { id -> captures.none { it.id == id } }.toSet()
        if (removedIds.isNotEmpty()) deletingCaptureIds = deletingCaptureIds - removedIds
    }
    val expandedCategoryCaptures = remember(capturesByCategory, expandedCategory, captureSortOrder) {
        expandedCategory?.let { tag -> sortCaptures(capturesByCategory[tag].orEmpty()) }.orEmpty()
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
                color = Color.Transparent,
            ) {
                Box(Modifier.fillMaxSize()) {
                    CaptureFlowBackdrop(Modifier.matchParentSize())
                    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { expandedCategory = null }, modifier = Modifier.size(48.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Voltar à biblioteca")
                            }
                            Text(tag, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Spacer(Modifier.size(44.dp))
                        }
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(expandedCategoryCaptures.size.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, lineHeight = MaterialTheme.typography.displayMedium.lineHeight)
                            Column(Modifier.padding(bottom = 6.dp)) {
                                Text("referências", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${expandedCategoryCaptures.count { it.isCompleted }} concluídas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
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
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                                labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
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
                            items(expandedCategoryVisibleCaptures, key = { it.id }, contentType = { "capture-thumbnail" }) { capture ->
                                AnimatedVisibility(
                                    visible = capture.id !in deletingCaptureIds,
                                    enter = fadeIn(tween(150)) + scaleIn(initialScale = 0.97f, animationSpec = tween(150)),
                                    exit = fadeOut(tween(180)) + shrinkVertically(animationSpec = tween(180)),
                                ) {
                                    CategoryThumbnail(
                                        capture = capture,
                                        ocrState = captureImageProcessingStates[capture.id] ?: CaptureImageProcessingStatus(),
                                        onClick = { selectedCapture = capture },
                                        onDelete = { if (capture.isCompleted) pendingDeleteCapture = capture },
                                    )
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }
    pendingDeleteCapture?.let { captureToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteCapture = null },
            title = { Text("Excluir tarefa?") },
            text = { Text("“${captureToDelete.title}” será removida permanentemente do caderno.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = captureToDelete.id
                        pendingDeleteCapture = null
                        deletingCaptureIds = deletingCaptureIds + id
                        uiScope.launch {
                            delay(190)
                            viewModel.deleteCapture(id)
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDeleteCapture = null }) { Text("Cancelar") } },
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    val viewerCandidates = if (expandedCategory != null) expandedCategoryVisibleCaptures else sortCaptures(allMatchingCaptures)
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
        mediaByCapture = remember(captureMedia) { captureMedia.groupBy { it.captureId } },
        onSetCover = viewModel::setCaptureCover,
        onEditCover = { captureId ->
            coverCropTargetId = captureId
            coverImagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
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
        onShare = { shareCapture = it },
    )

    coverCropSourceUri?.let { sourceUri ->
        Dialog(
            onDismissRequest = {
                coverCropSourceUri = null
                coverCropTargetId = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            ImageCropReviewScreen(
                imageUri = sourceUri,
                onConfirm = { croppedUri ->
                    val targetId = coverCropTargetId
                    if (targetId != null) {
                        viewModel.replaceCaptureCover(
                            targetId,
                            croppedUri,
                            ImageCompressionProfile.fromPreference(imageCompressionProfile),
                            onComplete = {
                                Uri.parse(croppedUri).path?.let { java.io.File(it).delete() }
                                coverCropSourceUri = null
                                coverCropTargetId = null
                            },
                        )
                    } else {
                        Uri.parse(croppedUri).path?.let { java.io.File(it).delete() }
                        coverCropSourceUri = null
                    }
                },
                onCancel = {
                    coverCropSourceUri = null
                    coverCropTargetId = null
                },
            )
        }
    }

    selectedNewsletterArticle?.let { article ->
        Dialog(
            onDismissRequest = { selectedNewsletterArticle = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                NewsletterArticleReader(article = article, onBack = { selectedNewsletterArticle = null })
            }
        }
    }

    shareCapture?.let { selectedItem ->
        val item = captures.firstOrNull { it.id == selectedItem.id } ?: selectedItem
        val itemMedia = captureMedia.filter { it.captureId == item.id }
        val decodedNotes = CaptureNotesCodec.decode(item.notes)
        CaptureShareDialog(
            payload = CaptureSharePayload(
                title = item.title,
                category = item.category,
                createdAt = item.createdAt,
                sourceUrl = item.sourceUrl,
                content = item.content,
                coverUri = item.imageUri,
                mediaUris = itemMedia.map { it.uri },
                notes = decodedNotes.map { CaptureShareNote(it.text, it.imageUri) },
            ),
            viewModel = shareViewModel,
            onDismiss = { shareCapture = null },
        )
    }

    notesCaptureId?.let { captureId ->
        val notesCapture = captures.firstOrNull { it.id == captureId }
            ?: selectedCapture?.takeIf { it.id == captureId }
        notesCapture?.let { item ->
            NotesEditorDialog(
                captureTitle = item.title,
                initialNotes = item.notes.orEmpty(),
                initialEditingNoteId = editingNoteId,
                onRequestCamera = { showNotesCameraCapture = true },
                cameraImageUri = notesCameraImageUri,
                onCameraImageConsumed = { notesCameraImageUri = null },
                onDismiss = {
                    showNotesCameraCapture = false
                    notesCameraImageUri = null
                    notesCaptureId = null
                    editingNoteId = null
                },
                onDeleteTemporaryImage = imageImportViewModel::deleteTemporaryImage,
                onCompressImage = imageImportViewModel::compressNoteImage,
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



    if (showNotesCameraCapture) {
        Dialog(
            onDismissRequest = { showNotesCameraCapture = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            ImageCaptureScreen(
                reviewBeforeReturn = false,
                onImageCaptured = { uri ->
                    notesCameraImageUri = uri
                    showNotesCameraCapture = false
                },
                onClose = { showNotesCameraCapture = false },
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
        Dialog(
            onDismissRequest = imageImportViewModel::cancelPendingImageReview,
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Revisar nova captura", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    pendingImagePreview?.let { preview ->
                        Image(
                            bitmap = preview,
                            contentDescription = "Prévia da imagem capturada",
                            modifier = Modifier.fillMaxWidth().height(210.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                            contentScale = ContentScale.Fit,
                        )
                    } ?: if (pending.isProcessing) {
                        Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        Spacer(Modifier.height(8.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Texto analisado no dispositivo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedTextField(
                        value = pending.title,
                        onValueChange = imageImportViewModel::updatePendingImageTitle,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Título") },
                        placeholder = { Text("Digite um título") },
                        maxLines = 2,
                        shape = RoundedCornerShape(15.dp),
                    )
                    Text("Categoria (obrigatória)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        activeCategories.forEach { item ->
                            FilterChip(
                                selected = pending.category == item,
                                onClick = { imageImportViewModel.updatePendingImageCategory(item) },
                                label = { Text(item, maxLines = 1) },
                            )
                        }
                    }
                    if (pending.isProcessing) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Analisando imagem com OCR…", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    pending.error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Spacer(Modifier.weight(1f))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = imageImportViewModel::cancelPendingImageReview,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp),
                        ) { Text("Cancelar") }
                        Button(
                            onClick = imageImportViewModel::confirmPendingImageReview,
                            enabled = !pending.isProcessing && pending.title.isNotBlank() && pending.category in activeCategories,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(15.dp),
                        ) { Text("Salvar referência") }
                    }
                }
            }
        }
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
        Dialog(
            onDismissRequest = {
                screenshotToConfirm = null
                onScreenshotImportHandled()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Adicionar screenshot às referências?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("A captura foi identificada pelo monitoramento ativado por você. A imagem original permanece na galeria; nada é salvo no CaptureFlow até sua confirmação.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        screenshotPreview?.let { preview ->
                            Image(
                                bitmap = preview,
                                contentDescription = "Prévia da screenshot que será salva como referência",
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } ?: Spacer(Modifier.height(12.dp))
                        Text("Categoria (obrigatória)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            activeCategories.forEach { item ->
                                FilterChip(
                                    selected = screenshotImportCategory == item,
                                    onClick = { screenshotImportCategory = item },
                                    label = { Text(item, maxLines = 1) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    screenshotToConfirm = null
                                    onScreenshotImportHandled()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    screenshotImportCategory?.let { selectedCategory ->
                                        imageImportViewModel.addImageCapture(
                                            uri.toString(),
                                            category = selectedCategory,
                                            deleteSourceScreenshotAfterImport = false,
                                            compressionProfile = ImageCompressionProfile.fromPreference(imageCompressionProfile),
                                        )
                                        screenshotToConfirm = null
                                        onScreenshotImportHandled()
                                    }
                                },
                                enabled = screenshotImportCategory != null,
                                modifier = Modifier.weight(1f),
                            ) { Text("Salvar referência") }
                        }
                    }
                }
            }
        }
    }

    pendingSharedLink?.let { pending ->
        Dialog(
            onDismissRequest = { imageImportViewModel.cancelPendingSharedLink(); sharedLinkCategory = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Salvar link como referência", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        OutlinedTextField(
                            value = pending.title,
                            onValueChange = imageImportViewModel::updatePendingSharedLinkTitle,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Título principal") },
                            placeholder = { Text("Digite um título") },
                            maxLines = 2,
                            shape = RoundedCornerShape(15.dp),
                        )
                        Text(pending.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (pending.isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Text("Buscando informações do link…", style = MaterialTheme.typography.bodySmall)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    if (pending.metadataFound) "Informações do link carregadas" else "Não foi possível obter informações automaticamente.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Text("Categoria (obrigatória)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            activeCategories.forEach { item ->
                                FilterChip(
                                    selected = sharedLinkCategory == item,
                                    onClick = { sharedLinkCategory = item },
                                    label = { Text(item, maxLines = 1) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { imageImportViewModel.cancelPendingSharedLink(); sharedLinkCategory = null },
                                modifier = Modifier.weight(1f),
                            ) { Text("Cancelar") }
                            Button(
                                onClick = {
                                    sharedLinkCategory?.let(imageImportViewModel::confirmPendingSharedLink)
                                    sharedLinkCategory = null
                                },
                                enabled = !pending.isLoading && sharedLinkCategory != null && pending.title.isNotBlank(),
                                modifier = Modifier.weight(1f),
                            ) { Text("Salvar referência") }
                        }
                    }
                }
            }
        }
    }

    selectedImageUri?.let { uri ->
        Dialog(
            onDismissRequest = {
                selectedImageUri = null
                if (uri == detectedScreenshotUri) onScreenshotHandled()
            },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.56f)), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            if (uri == detectedScreenshotUri) "Screenshot detectada" else "Adicionar imagem às referências",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            if (uri == detectedScreenshotUri) "Encontramos uma nova captura de tela. O monitoramento é opcional e pode ser desligado no ícone de sino."
                            else "Escolha a categoria para organizar esta imagem.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        val detectedPreview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, uri) {
                            value = withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, uri.toString(), maxDimension = 360) }.getOrNull() }
                        }
                        detectedPreview?.let { preview ->
                            Image(
                                bitmap = preview,
                                contentDescription = "Prévia da screenshot detectada",
                                modifier = Modifier.fillMaxWidth().height(150.dp),
                                contentScale = ContentScale.Fit,
                            )
                        } ?: Spacer(Modifier.height(12.dp))
                        Text("Enviar para a categoria", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            activeCategories.forEach { item ->
                                FilterChip(
                                    selected = imageCategory == item,
                                    onClick = { imageCategory = item },
                                    label = { Text(item, maxLines = 1) },
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    selectedImageUri = null
                                    if (uri == detectedScreenshotUri) onScreenshotHandled()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("Agora não") }
                            Button(
                                onClick = {
                                    imageImportViewModel.addImageCapture(uri.toString(), imageCategory, compressionProfile = ImageCompressionProfile.fromPreference(imageCompressionProfile))
                                    selectedImageUri = null
                                    if (uri == detectedScreenshotUri) onScreenshotHandled()
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text(if (uri == detectedScreenshotUri) "Enviar para o app" else "Salvar referência") }
                        }
                    }
                }
            }
        }
    }
}
