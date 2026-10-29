package com.fehunter.captureflow.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import com.fehunter.captureflow.BuildConfig
import com.fehunter.captureflow.data.CaptureEntity
import com.fehunter.captureflow.data.CaptureSubtaskEntity
import com.fehunter.captureflow.data.ChecklistTaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowApp(viewModel: CaptureViewModel, themeMode: String, onThemeModeChange: (String) -> Unit) {
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
            CaptureLibrary(viewModel)
        }
    }
}

@Composable
private fun CaptureLibrary(viewModel: CaptureViewModel) {
    val captures by viewModel.captures.collectAsState()
    val ocrStates by viewModel.ocrStates.collectAsState()
    val allSubtasks by viewModel.captureSubtasks.collectAsState()
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var imageCategory by remember { mutableStateOf("Referência") }
    val categories = listOf("Todas", "Ler depois", "Ideias", "Tarefas", "Referência")
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> selectedImageUri = uri },
    )
    val filtered = captures.filter {
        (query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)) &&
            (category == "Todas" || it.category == category)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
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
        item(key = "search") {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Buscar referências") },
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
            items(filtered, key = { it.id }) { capture ->
                CaptureCard(
                    capture = capture,
                    ocrState = ocrStates[capture.id] ?: OcrUiState(),
                    onCompleted = { viewModel.setCaptureCompleted(capture.id, it) },
                    onDelete = { viewModel.deleteCapture(capture.id) },
                    subtasks = allSubtasks.filter { it.captureId == capture.id },
                    onAddSubtask = { viewModel.addCaptureSubtask(capture.id, it) },
                    onSubtaskCompleted = viewModel::setCaptureSubtaskCompleted,
                    onDeleteSubtask = viewModel::deleteCaptureSubtask,
                )
            }
        }
    }
    selectedImageUri?.let { uri ->
        AlertDialog(onDismissRequest = { selectedImageUri = null }, title = { Text("Vincular imagem a uma tag") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Escolha a categoria para organizar esta imagem.")
                categories.drop(1).forEach { tag ->
                    FilterChip(selected = imageCategory == tag, onClick = { imageCategory = tag }, label = { Text(tag) })
                }
            }
        }, confirmButton = { Button(onClick = { viewModel.addImageCapture(uri, imageCategory); selectedImageUri = null }) { Text("Salvar imagem") } }, dismissButton = { TextButton(onClick = { selectedImageUri = null }) { Text("Cancelar") } })
    }
}

@Composable
private fun CaptureCard(
    capture: CaptureEntity,
    ocrState: OcrUiState,
    onCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    subtasks: List<CaptureSubtaskEntity>,
    onAddSubtask: (String) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
) {
    var showFullscreen by remember(capture.id) { mutableStateOf(false) }
    var newSubtask by remember(capture.id) { mutableStateOf("") }
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

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = capture.isCompleted, onCheckedChange = onCompleted)
                Text(
                    text = capture.title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                if (ocrState.isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Excluir referência")
                }
            }

            cover?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Abrir capa em tela cheia",
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clickable { showFullscreen = true },
                    contentScale = ContentScale.Crop,
                )
            }

            if (ocrState.isError) {
                Text(
                    text = ocrState.message ?: "Não foi possível analisar esta imagem.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else if (ocrState.isProcessing) {
                Text(
                    text = "Analisando imagem…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (subtasks.isNotEmpty()) {
                Text("Sub-checklist", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                subtasks.forEach { subtask ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onSubtaskCompleted(subtask.id, it) },
                        )
                        Text(
                            text = subtask.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                            Icon(Icons.Default.Close, contentDescription = "Excluir subtarefa")
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedTextField(
                    value = newSubtask,
                    onValueChange = { newSubtask = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Adicionar sub-tarefa") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        onAddSubtask(newSubtask)
                        newSubtask = ""
                    }),
                )
                IconButton(
                    onClick = {
                        onAddSubtask(newSubtask)
                        newSubtask = ""
                    },
                    enabled = newSubtask.isNotBlank(),
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Adicionar sub-tarefa")
                }
            }
        }
    }

    if (showFullscreen && cover != null) {
        Dialog(
            onDismissRequest = { showFullscreen = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    bitmap = cover!!,
                    contentDescription = "Imagem em tela cheia",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit,
                )
                IconButton(
                    onClick = { showFullscreen = false },
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                        .background(Color.DarkGray.copy(alpha = 0.75f), MaterialTheme.shapes.large),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Fechar imagem", tint = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ChecklistScreen(viewModel: CaptureViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var title by remember { mutableStateOf("") }
    val screenshotPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> uri?.let(viewModel::addImageTask) },
    )
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Nova tarefa") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.addTask(title); title = "" }), singleLine = true)
        Button(onClick = { viewModel.addTask(title); title = "" }, enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar tarefa")
        }
        OutlinedButton(
            onClick = {
                screenshotPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Salvar screenshot como tarefa")
        }
        if (tasks.isEmpty()) Text("Suas tarefas aparecerão aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(
                    task = task,
                    onCompleted = { viewModel.setTaskCompleted(task.id, it) },
                    onDelete = { viewModel.deleteTask(task.id) },
                    onAnalyze = { viewModel.analyzeTaskScreenshot(task) },
                )
            }
        }
    }
}

@Composable
private fun TaskCard(
    task: ChecklistTaskEntity,
    onCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit,
) {
    val context = LocalContext.current
    val cover by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, task.imageUri,
    ) {
        value = task.imageUri?.let { raw ->
            withContext(Dispatchers.IO) {
                runCatching { loadCoverBitmap(context, raw) }.getOrNull()
            }
        }
    }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.isCompleted, onCheckedChange = onCompleted)
                Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold)
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir tarefa") }
            }
            cover?.let { bitmap ->
                Image(
                    bitmap = bitmap,
                    contentDescription = "Screenshot anexado à tarefa",
                    modifier = Modifier.fillMaxWidth().heightIn(max = 220.dp),
                    contentScale = ContentScale.Crop,
                )
            }
            task.description?.takeIf(String::isNotBlank)?.let { description ->
                Text(description, style = MaterialTheme.typography.bodyMedium, maxLines = 6)
            }
            if (task.imageUri != null) {
                OutlinedButton(onClick = onAnalyze, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Image, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Analisar screenshot")
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
