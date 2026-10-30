package com.fehunter.captureflow.ui

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.snapshotFlow
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.ui.text.style.TextOverflow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.io.File
import com.fehunter.captureflow.BuildConfig
import com.fehunter.captureflow.data.CaptureEntity
import com.fehunter.captureflow.data.CaptureSubtaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowApp(viewModel: CaptureViewModel, themeMode: String, onThemeModeChange: (String) -> Unit, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, detectedScreenshotUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, pendingTaskScreenshotUri: Uri?, onTaskScreenshotHandled: () -> Unit) {
    var showThemeMenu by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }
    var showCompletedCaptures by remember { mutableStateOf(true) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Capture Flow", fontWeight = FontWeight.Bold) }, actions = {
                IconButton(onClick = { showOptionsMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Opções da biblioteca")
                }
                DropdownMenu(expanded = showOptionsMenu, onDismissRequest = { showOptionsMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(if (showCompletedCaptures) "Ocultar concluídos" else "Mostrar concluídos") },
                        onClick = {
                            showCompletedCaptures = !showCompletedCaptures
                            showOptionsMenu = false
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Alterar tema") },
                        onClick = { showOptionsMenu = false; showThemeMenu = true },
                    )
                }
                DropdownMenu(expanded = showThemeMenu, onDismissRequest = { showThemeMenu = false }) {
                    listOf("system" to "Seguir dispositivo", "light" to "Claro", "dark" to "Escuro").forEach { (mode, label) ->
                        DropdownMenuItem(text = { Text(if (themeMode == mode) "✓ $label" else label) }, onClick = { onThemeModeChange(mode); showThemeMenu = false })
                    }
                }
            })
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            CaptureLibrary(
                viewModel, hasScreenshotAccess, screenshotMonitoringEnabled, showCompletedCaptures,
                detectedScreenshotUri, pendingTaskScreenshotUri,
                onRequestScreenshotAccess, onStopScreenshotMonitoring,
                onScreenshotHandled, onTaskScreenshotHandled,
            )
        }
    }
}

@Composable
private fun CaptureLibrary(viewModel: CaptureViewModel, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, showCompletedCaptures: Boolean, detectedScreenshotUri: Uri?, pendingTaskScreenshotUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, onTaskScreenshotHandled: () -> Unit) {
    val context = LocalContext.current
    val captures by viewModel.captures.collectAsState()
    val ocrStates by viewModel.ocrStates.collectAsState()
    val pendingSharedLink by viewModel.pendingSharedLink.collectAsState()
    val categoryEntities by viewModel.categories.collectAsState()
    val allSubtasks by viewModel.captureSubtasks.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCategory by remember { mutableStateOf("Ler depois") }
    var expandedCategory by remember { mutableStateOf<String?>(null) }
    var selectedCapture by remember { mutableStateOf<CaptureEntity?>(null) }
    var sharedLinkCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }
    LaunchedEffect(pendingSharedLink?.id) { sharedLinkCategory = null }
    val categories = listOf("Todas") + categoryEntities.map { it.name }
    LaunchedEffect(categoryEntities) {
        if (category != "Todas" && categoryEntities.none { it.name == category }) category = "Todas"
        if (sharedLinkCategory != null && categoryEntities.none { it.name == sharedLinkCategory }) sharedLinkCategory = null
        if (categoryEntities.isNotEmpty() && categoryEntities.none { it.name == imageCategory }) imageCategory = categoryEntities.first().name
    }
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri },
    )
    LaunchedEffect(detectedScreenshotUri) {
        detectedScreenshotUri?.let { selectedImageUri = it }
    }
    var taskScreenshotToConfirm by remember { mutableStateOf<Uri?>(null) }
    LaunchedEffect(pendingTaskScreenshotUri) {
        if (pendingTaskScreenshotUri != null) taskScreenshotToConfirm = pendingTaskScreenshotUri
    }
    val taskScreenshotPreview by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, taskScreenshotToConfirm,
    ) {
        value = taskScreenshotToConfirm?.let { uri ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, uri.toString()) }.getOrNull()
            }
        }
    }
    val filtered = captures.filter {
        (query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)) &&
            (showCompletedCaptures || !it.isCompleted) &&
            (category == "Todas" || it.category == category)
    }

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
                    if (!screenshotMonitoringEnabled) {
                        OutlinedButton(onClick = onRequestScreenshotAccess, modifier = Modifier.fillMaxWidth()) {
                            Text(if (hasScreenshotAccess) "Ativar avisos de screenshot" else "Configurar avisos de screenshot")
                        }
                        Text(
                            "Requisitos: permitir acesso a fotos/imagens e notificações. O Android mantém uma notificação fixa enquanto a detecção estiver ativa. A detecção depende do nome/pasta da captura e pode variar por fabricante.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    } else {
                        Text("Detecção ativa em segundo plano. Uma notificação fixa indica que o monitoramento está ligado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        OutlinedButton(onClick = onStopScreenshotMonitoring, modifier = Modifier.fillMaxWidth()) {
                            Text("Desativar avisos de screenshot")
                        }
                    }
                }
            }
        }
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Pesquisar") },
                singleLine = true,
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpar busca")
                        }
                    }
                },
            )
        }
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
            val visibleCategories = if (category == "Todas") categories.drop(1) else listOf(category)
            items(visibleCategories, key = { "category-$it" }) { tag ->
                val categoryCaptures = filtered.filter { it.category == tag }
                if (categoryCaptures.isNotEmpty()) {
                    CategoryCard(
                        title = tag,
                        captures = categoryCaptures,
                        ocrStates = ocrStates,
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
            onAdd = viewModel::addCategory,
            onRename = viewModel::renameCategory,
            onDelete = viewModel::deleteCategory,
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
                                    ocrState = ocrStates[capture.id] ?: OcrUiState(),
                                    onClick = { selectedCapture = capture },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    selectedCapture?.let { capture ->
        val viewerCaptures = filtered.filter { it.category == capture.category }
        val initialPage = viewerCaptures.indexOfFirst { it.id == capture.id }.coerceAtLeast(0)
        val sourceUrl = capture.sourceUrl?.takeIf { url ->
            Uri.parse(url).scheme?.lowercase()?.let { it == "http" || it == "https" } == true
        }
        Dialog(
            onDismissRequest = { selectedCapture = null },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
        ) {
            BackHandler { selectedCapture = null }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { viewerCaptures.size },
            )
            LaunchedEffect(capture.id, viewerCaptures) {
                val targetPage = viewerCaptures.indexOfFirst { it.id == capture.id }
                if (targetPage in viewerCaptures.indices && pagerState.currentPage != targetPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }
            LaunchedEffect(pagerState, viewerCaptures) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    viewerCaptures.getOrNull(page)?.let { pageCapture ->
                        if (selectedCapture?.id != pageCapture.id) selectedCapture = pageCapture
                    }
                }
            }
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(capture.createdAt)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(capture.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                        }
                        sourceUrl?.let { url ->
                            IconButton(
                                onClick = {
                                    runCatching {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                    }
                                },
                                modifier = Modifier.size(48.dp),
                            ) {
                                Icon(
                                    Icons.Default.Link,
                                    contentDescription = "Abrir link original",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                        Checkbox(checked = capture.isCompleted, onCheckedChange = { completed ->
                            viewModel.setCaptureCompleted(capture.id, completed)
                            selectedCapture = capture.copy(isCompleted = completed)
                        }, modifier = Modifier.size(48.dp))
                        IconButton(onClick = { viewModel.deleteCapture(capture.id); selectedCapture = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir captura", tint = MaterialTheme.colorScheme.onSurface)
                        }
                        IconButton(onClick = { selectedCapture = null }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar visualizador", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        key = { page -> viewerCaptures[page].id },
                    ) { page ->
                        FullscreenCapturePage(
                            capture = viewerCaptures[page],
                            showNavigationHint = page == pagerState.currentPage,
                        )
                    }
                }
            }
        }
    }
    taskScreenshotToConfirm?.let { uri ->
        AlertDialog(
            onDismissRequest = {
                taskScreenshotToConfirm = null
                onTaskScreenshotHandled()
            },
            title = { Text("Adicionar screenshot às referências?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("A captura foi aberta pela notificação. Confirme para anexá-la a uma nova tarefa.")
                    taskScreenshotPreview?.let { preview ->
                        Image(
                            bitmap = preview,
                            contentDescription = "Prévia da screenshot que será anexada à tarefa",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                            contentScale = ContentScale.Fit,
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addImageCapture(uri, category = "Salvar", deleteSourceScreenshotAfterImport = true)
                    taskScreenshotToConfirm = null
                    onTaskScreenshotHandled()
                }) { Text("Adicionar às referências") }
            },
            dismissButton = {
                TextButton(onClick = {
                    taskScreenshotToConfirm = null
                    onTaskScreenshotHandled()
                }) { Text("Cancelar") }
            },
        )
    }

    pendingSharedLink?.let { pending ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelPendingSharedLink(); sharedLinkCategory = null },
            title = { Text("Salvar link como referência") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 430.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    CategorySelectionSection(
                        categories = categories.drop(1),
                        selectedCategory = sharedLinkCategory,
                        onCategorySelected = { sharedLinkCategory = it },
                    )
                    OutlinedTextField(
                        value = pending.title,
                        onValueChange = viewModel::updatePendingSharedLinkTitle,
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
                        sharedLinkCategory?.let(viewModel::confirmPendingSharedLink)
                        sharedLinkCategory = null
                    },
                    enabled = !pending.isLoading && sharedLinkCategory != null && pending.title.isNotBlank(),
                ) { Text("Salvar referência") }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.cancelPendingSharedLink()
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
                    categories = categories.drop(1),
                    selectedCategory = imageCategory,
                    onCategorySelected = { imageCategory = it },
                )
            }
        }, confirmButton = {
            Button(onClick = {
                viewModel.addImageCapture(uri, imageCategory)
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

@Composable
private fun CategoryThumbnail(
    capture: CaptureEntity,
    ocrState: OcrUiState,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, capture.imageUri,
    ) {
        value = capture.imageUri?.let { raw ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, raw) }.getOrNull()
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap!!, contentDescription = capture.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Column(
                Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Sem imagem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (ocrState.isProcessing || ocrState.isError) {
            Surface(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
            ) {
                Text(if (ocrState.isError) "⚠" else "◌", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                .padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            Text(capture.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CategoryCard(
    title: String,
    captures: List<CaptureEntity>,
    ocrStates: Map<String, OcrUiState>,
    onExpand: () -> Unit,
    onOpenCapture: (CaptureEntity) -> Unit,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text("${captures.size} itens", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                captures.take(3).forEach { capture ->
                    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
                        initialValue = null, capture.imageUri,
                    ) {
                        value = capture.imageUri?.let { raw ->
                            withContext(Dispatchers.IO) {
                                runCatching { loadCoverBitmap(context, raw) }.getOrNull()
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.weight(1f).aspectRatio(1f)
                            .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.medium)
                            .clickable { onOpenCapture(capture) },
                    ) {
                        if (bitmap != null) {
                            Image(bitmap = bitmap!!, contentDescription = capture.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Column(
                                Modifier.fillMaxSize().padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Sem imagem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (ocrStates[capture.id]?.isProcessing == true || ocrStates[capture.id]?.isError == true) {
                            Surface(
                                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                            ) {
                                Text(if (ocrStates[capture.id]?.isError == true) "⚠" else "◌", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f))))
                                .padding(horizontal = 6.dp, vertical = 5.dp),
                        ) {
                            Text(capture.title, style = MaterialTheme.typography.labelSmall, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                repeat((3 - minOf(captures.size, 3)).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f).aspectRatio(1f))
                }
            }
            Surface(
                onClick = onExpand,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                color = Color.Transparent,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Default.ExpandMore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Ver todas · ${captures.size}", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureCard(
    capture: CaptureEntity,
    ocrState: OcrUiState,
    onCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCategoryChange: (String) -> Unit,
    categories: List<String>,
    allCaptures: List<CaptureEntity>,
    subtasks: List<CaptureSubtaskEntity>,
    allSubtasks: List<CaptureSubtaskEntity>,
    onAddSubtask: (String, String) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onTitleChange: (String, String) -> Unit,
) {
    var showFullscreen by remember(capture.id) { mutableStateOf(false) }
    var fullscreenCapture by remember(capture.id) { mutableStateOf(capture) }
    var newSubtask by remember(capture.id) { mutableStateOf("") }
    var showSubtasks by remember(capture.id) { mutableStateOf(false) }
    var showEditTitle by remember(capture.id) { mutableStateOf(false) }
    var editedTitle by remember(capture.id) { mutableStateOf(capture.title) }
    val context = LocalContext.current
    val cover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, capture.imageUri,
    ) {
        value = capture.imageUri?.let { raw ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, raw) }.getOrNull()
            }
        }
    }

    val modalSubtasks = allSubtasks.filter { it.captureId == fullscreenCapture.id }

    Card(
        modifier = Modifier.fillMaxWidth().height(272.dp),
        shape = MaterialTheme.shapes.large,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(104.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { fullscreenCapture = capture; showFullscreen = true },
            ) {
                if (cover != null) {
                    Image(
                        bitmap = cover!!,
                        contentDescription = "Abrir detalhes em tela cheia",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                    IconButton(
                        onClick = { fullscreenCapture = capture; showFullscreen = true },
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .background(Color.Black.copy(alpha = 0.58f), MaterialTheme.shapes.medium)
                            .size(40.dp),
                    ) {
                        Icon(Icons.Default.OpenInFull, contentDescription = "Expandir detalhes", tint = Color.White)
                    }
                } else {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Sem imagem", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                val status = when {
                    ocrState.isError -> ocrState.message ?: "Erro ao analisar"
                    ocrState.isProcessing -> "Analisando imagem…"
                    else -> null
                }
                if (status != null) {
                    Text(
                        status,
                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.94f)).padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (ocrState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                Text(
                    text = capture.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (capture.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (capture.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    lineHeight = MaterialTheme.typography.titleSmall.lineHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp, max = 42.dp)
                        .clickable { fullscreenCapture = capture; showFullscreen = true },
                )
                Text(
                    text = capture.title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = MaterialTheme.typography.labelSmall.lineHeight,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 38.dp, max = 44.dp)
                        .clickable { showFullscreen = true },
                )
                Spacer(Modifier.height(4.dp))
                Box {
                    var showCategoryMenu by remember(capture.id) { mutableStateOf(false) }
                    Surface(
                        onClick = { showCategoryMenu = true },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.height(24.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(capture.category, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Icon(Icons.Default.ExpandMore, contentDescription = "Alterar categoria", modifier = Modifier.size(16.dp))
                        }
                    }
                    DropdownMenu(expanded = showCategoryMenu, onDismissRequest = { showCategoryMenu = false }) {
                        categories.forEach { tag ->
                            DropdownMenuItem(text = { Text(tag) }, onClick = {
                                onCategoryChange(tag)
                                showCategoryMenu = false
                            })
                        }
                    }
                }
                Text(
                    text = SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(capture.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Checkbox(
                        checked = capture.isCompleted,
                        onCheckedChange = onCompleted,
                        modifier = Modifier.size(48.dp),
                    )
                }
                TextButton(
                    onClick = { showSubtasks = true },
                    modifier = Modifier.height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Text("☰ ${subtasks.count { it.isCompleted }}/${subtasks.size}", maxLines = 1, style = MaterialTheme.typography.labelSmall)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir referência")
                }
            }
        }
    }

    if (showSubtasks) {
        ModalBottomSheet(onDismissRequest = { showSubtasks = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sub-checklist · ${modalSubtasks.count { it.isCompleted }}/${modalSubtasks.size} concluídas",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { showSubtasks = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar sub-checklist")
                    }
                }
                modalSubtasks.forEach { subtask ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Checkbox(checked = subtask.isCompleted, onCheckedChange = { onSubtaskCompleted(subtask.id, it) })
                        Text(
                            subtask.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir subtarefa")
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newSubtask,
                        onValueChange = { newSubtask = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nova subtarefa") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            onAddSubtask(fullscreenCapture.id, newSubtask)
                            newSubtask = ""
                        }),
                    )
                    IconButton(
                        onClick = {
                            onAddSubtask(fullscreenCapture.id, newSubtask)
                            newSubtask = ""
                        },
                        enabled = newSubtask.isNotBlank(),
                    ) { Icon(Icons.Default.Add, contentDescription = "Adicionar subtarefa") }
                }
            }
        }
    }

    if (showFullscreen) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
        ) {
            BackHandler { showFullscreen = false }
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(0.20f).padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                fullscreenCapture.title,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                            IconButton(
                                onClick = {
                                    editedTitle = fullscreenCapture.title
                                    showEditTitle = true
                                },
                                modifier = Modifier.size(36.dp),
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Editar título",
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(fullscreenCapture.createdAt)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(fullscreenCapture.category, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    IconButton(onClick = { showFullscreen = false }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar detalhes")
                    }
                }
                val initialPage = allCaptures.indexOfFirst { it.id == fullscreenCapture.id }.coerceAtLeast(0)
                val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { allCaptures.size })
                LaunchedEffect(fullscreenCapture.id, allCaptures) {
                    val targetPage = allCaptures.indexOfFirst { it.id == fullscreenCapture.id }
                    if (targetPage in allCaptures.indices && pagerState.currentPage != targetPage) {
                        pagerState.scrollToPage(targetPage)
                    }
                }
                LaunchedEffect(pagerState, allCaptures) {
                    snapshotFlow { pagerState.settledPage }.collect { page ->
                        allCaptures.getOrNull(page)?.let { pageCapture ->
                            if (fullscreenCapture.id != pageCapture.id) fullscreenCapture = pageCapture
                        }
                    }
                }
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth().weight(0.80f).background(MaterialTheme.colorScheme.surfaceVariant),
                    key = { page -> allCaptures[page].id },
                ) { page ->
                    FullscreenCapturePage(
                        capture = allCaptures[page],
                        showNavigationHint = page == pagerState.currentPage,
                    )
                }
            }
        }
    }
    
    if (showEditTitle) {
        AlertDialog(
            onDismissRequest = { showEditTitle = false },
            title = { Text("Editar título") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("Título principal") },
                    placeholder = { Text("Digite um título") },
                    singleLine = false,
                    maxLines = 3,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        val title = editedTitle.trim().take(160)
                        if (title.isNotBlank()) {
                            onTitleChange(fullscreenCapture.id, title)
                            fullscreenCapture = fullscreenCapture.copy(title = title)
                            showEditTitle = false
                        }
                    }),
                )
            },
            confirmButton = {
                Button(onClick = {
                    val title = editedTitle.trim().take(160)
                    if (title.isNotBlank()) {
                        onTitleChange(fullscreenCapture.id, title)
                        fullscreenCapture = fullscreenCapture.copy(title = title)
                        showEditTitle = false
                    }
                }, enabled = editedTitle.isNotBlank()) { Text("Salvar") }
            },
            dismissButton = {
                TextButton(onClick = { showEditTitle = false }) { Text("Cancelar") }
            },
        )
    }
}


@Composable
private fun CategorySelectionSection(
    categories: List<String>,
    selectedCategory: String?,
    onCategorySelected: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Categoria",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (categories.size > 3) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        "Role para ver todas",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Icon(
                        Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 156.dp)
                .verticalScroll(rememberScrollState())
                .background(
                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    MaterialTheme.shapes.medium,
                )
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            categories.forEach { tag ->
                FilterChip(
                    selected = selectedCategory == tag,
                    onClick = { onCategorySelected(tag) },
                    label = { Text(tag) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (categories.size > 3) {
            Text(
                "Deslize dentro da lista para acessar as demais categorias.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CategoryManagementDialog(
    categories: List<String>,
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedCategoryName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerenciar categorias") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Adicione, renomeie ou remova categorias. As referências existentes acompanham as alterações.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Nova categoria") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            onAdd(newCategoryName)
                            newCategoryName = ""
                        },
                        enabled = newCategoryName.isNotBlank(),
                    ) { Icon(Icons.Default.Add, contentDescription = "Adicionar categoria") }
                }
                Divider()
                categories.forEach { name ->
                    if (editingCategory == name) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editedCategoryName,
                                onValueChange = { editedCategoryName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Nome da categoria") },
                            )
                            IconButton(onClick = {
                                onRename(name, editedCategoryName)
                                editingCategory = null
                            }, enabled = editedCategoryName.isNotBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Salvar nome")
                            }
                            IconButton(onClick = { editingCategory = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar edição")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = {
                                editingCategory = name
                                editedCategoryName = name
                            }) { Icon(Icons.Default.Edit, contentDescription = "Renomear $name") }
                            IconButton(onClick = { deleteTarget = name }, enabled = categories.size > 1) {
                                Icon(Icons.Default.Delete, contentDescription = "Excluir $name")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Concluir") } },
    )

    deleteTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir categoria?") },
            text = { Text("As referências e tarefas em \"$name\" serão movidas para outra categoria.") },
            confirmButton = {
                Button(onClick = {
                    onDelete(name)
                    deleteTarget = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun FullscreenCapturePage(capture: CaptureEntity, showNavigationHint: Boolean) {
    val context = LocalContext.current
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, capture.imageUri,
    ) {
        value = capture.imageUri?.let { raw ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, raw) }.getOrNull()
            }
        }
    }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = "Imagem completa da referência",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Esta referência não possui imagem.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // Keep the complete title readable over the image, regardless of its length.
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.99f), Color.Black.copy(alpha = 0.94f), Color.Black.copy(alpha = 0.78f), Color.Black.copy(alpha = 0.42f), Color.Transparent)))
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 38.dp),
        ) {
            Text(text = capture.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (showNavigationHint) {
            // Overlay does not reserve layout space or reduce the image's Fit area.
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 2.dp,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("‹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Deslize para navegar", style = MaterialTheme.typography.labelMedium)
                    Text("›", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun loadCoverBitmap(context: Context, rawUri: String): androidx.compose.ui.graphics.ImageBitmap? {
    val uri = Uri.parse(rawUri)
    fun openImage() = if (uri.scheme == "file") {
        uri.path?.let(::File)?.takeIf(File::isFile)?.inputStream()
    } else {
        context.contentResolver.openInputStream(uri)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = openImage() ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 1200 || bounds.outHeight / sampleSize > 1200) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return openImage()?.use {
        BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
    }
}
