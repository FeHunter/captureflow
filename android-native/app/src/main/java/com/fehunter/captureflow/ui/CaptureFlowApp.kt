package com.fehunter.captureflow.ui

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
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("CaptureFlow v${BuildConfig.VERSION_NAME}", fontWeight = FontWeight.Bold)
                    Text("Suas referências, organizadas", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }, actions = {
                IconButton(onClick = { showThemeMenu = true }) { Icon(Icons.Default.Palette, contentDescription = "Alterar tema") }
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
                viewModel, hasScreenshotAccess, screenshotMonitoringEnabled,
                detectedScreenshotUri, pendingTaskScreenshotUri,
                onRequestScreenshotAccess, onStopScreenshotMonitoring,
                onScreenshotHandled, onTaskScreenshotHandled,
            )
        }
    }
}

@Composable
private fun CaptureLibrary(viewModel: CaptureViewModel, hasScreenshotAccess: Boolean, screenshotMonitoringEnabled: Boolean, detectedScreenshotUri: Uri?, pendingTaskScreenshotUri: Uri?, onRequestScreenshotAccess: () -> Unit, onStopScreenshotMonitoring: () -> Unit, onScreenshotHandled: () -> Unit, onTaskScreenshotHandled: () -> Unit) {
    val context = LocalContext.current
    val captures by viewModel.captures.collectAsState()
    val ocrStates by viewModel.ocrStates.collectAsState()
    val allSubtasks by viewModel.captureSubtasks.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCategory by remember { mutableStateOf("Ler depois") }
    val categories = listOf("Todas", "Ler depois", "Ideias", "Tarefa", "Importante", "Mais tarde")
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
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                categories.forEach { item ->
                    FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
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
            items(filtered.chunked(2), key = { row -> row.first().id }) { captureRow ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    captureRow.forEach { capture ->
                        Box(Modifier.weight(1f)) {
                CaptureCard(
                    capture = capture,
                    ocrState = ocrStates[capture.id] ?: OcrUiState(),
                    onCompleted = { viewModel.setCaptureCompleted(capture.id, it) },
                    onDelete = { viewModel.deleteCapture(capture.id) },
                    onCategoryChange = { viewModel.setCaptureCategory(capture.id, it) },
                    allCaptures = filtered,
                    subtasks = allSubtasks.filter { it.captureId == capture.id },
                    allSubtasks = allSubtasks,
                    onAddSubtask = { captureId, title -> viewModel.addCaptureSubtask(captureId, title) },
                    onSubtaskCompleted = viewModel::setCaptureSubtaskCompleted,
                    onDeleteSubtask = viewModel::deleteCaptureSubtask,
                    onTitleChange = { id, title -> viewModel.updateCaptureTitle(id, title) },
                )
                        }
                    }
                    if (captureRow.size == 1) Spacer(Modifier.weight(1f))
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
                    viewModel.addImageCapture(uri, category = "Referência", deleteSourceScreenshotAfterImport = true)
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

    selectedImageUri?.let { uri ->
        AlertDialog(onDismissRequest = {
            selectedImageUri = null
            if (uri == detectedScreenshotUri) onScreenshotHandled()
        }, title = { Text(if (uri == detectedScreenshotUri) "Screenshot detectada" else "Vincular imagem a uma tag") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (uri == detectedScreenshotUri) {
    "Uma nova captura de tela foi encontrada. Deseja enviá-la ao CaptureFlow?"
} else {
    "Escolha a categoria para organizar esta imagem."
})
                categories.drop(1).forEach { tag ->
                    FilterChip(selected = imageCategory == tag, onClick = { imageCategory = tag }, label = { Text(tag) })
                }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CaptureCard(
    capture: CaptureEntity,
    ocrState: OcrUiState,
    onCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onCategoryChange: (String) -> Unit,
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
        modifier = Modifier.fillMaxWidth().height(320.dp),
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
                        listOf("Ler depois", "Ideias", "Tarefa", "Importante", "Mais tarde").forEach { tag ->
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
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Column(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().weight(0.30f).padding(start = 20.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(fullscreenCapture.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, maxLines = 3, overflow = TextOverflow.Ellipsis)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault()).format(Date(fullscreenCapture.createdAt)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Text(fullscreenCapture.category, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                    IconButton(onClick = {
                        editedTitle = fullscreenCapture.title
                        showEditTitle = true
                    }, modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.large)) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar título da referência")
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
                    modifier = Modifier.fillMaxWidth().weight(0.70f).background(MaterialTheme.colorScheme.surfaceVariant),
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
                    val title = editedTitle.trim()
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
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
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
        // Long titles overlay the image with a dark-to-transparent fade.
        if (capture.title.length > 72) {
            Box(
                modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.88f), Color.Black.copy(alpha = 0.48f), Color.Transparent)))
                    .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 38.dp),
            ) {
                Text(text = capture.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
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
