package com.fehunter.captureflow.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fehunter.captureflow.data.CaptureEntity
import com.fehunter.captureflow.data.ChecklistTaskEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureFlowApp(viewModel: CaptureViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    Scaffold(
        topBar = {
            TopAppBar(title = {
                Column {
                    Text("CaptureFlow", fontWeight = FontWeight.Bold)
                    Text(if (tab == 0) "Suas referências, organizadas" else "Transforme ideias em ação",
                        style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            })
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(selected = tab == 0, onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Link, null) }, label = { Text("Referências") })
                NavigationBarItem(selected = tab == 1, onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.CheckCircle, null) }, label = { Text("Checklist") })
            }
        },
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            if (tab == 0) CaptureLibrary(viewModel) else ChecklistScreen(viewModel)
        }
    }
}

@Composable
private fun CaptureLibrary(viewModel: CaptureViewModel) {
    val captures by viewModel.captures.collectAsState()
    val ocrStates by viewModel.ocrStates.collectAsState()
    var content by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Todas") }
    val categories = listOf("Todas", "Ler depois", "Ideias", "Tarefas", "Referência")
    val filtered = captures.filter {
        (query.isBlank() || it.title.contains(query, true) || it.content.contains(query, true)) &&
            (category == "Todas" || it.category == category)
    }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = content, onValueChange = { content = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Salvar texto ou link") }, placeholder = { Text("Cole um link ou escreva uma referência") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.addCapture(content); content = "" }),
            minLines = 1, maxLines = 3)
        Button(onClick = { viewModel.addCapture(content); content = "" }, enabled = content.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Salvar referência")
        }
        OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Buscar referências") }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            categories.forEach { item ->
                FilterChip(selected = category == item, onClick = { category = item }, label = { Text(item) })
            }
        }
        if (filtered.isEmpty()) {
            Text(if (captures.isEmpty()) "Compartilhe um link, texto ou imagem de outro app para começar."
                else "Nenhuma referência encontrada.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { capture ->
                    CaptureCard(
                        capture = capture,
                        ocrState = ocrStates[capture.id] ?: OcrUiState(),
                        onCompleted = { viewModel.setCaptureCompleted(capture.id, it) },
                        onDelete = { viewModel.deleteCapture(capture.id) },
                        onOcr = { viewModel.extractText(capture) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CaptureCard(
    capture: CaptureEntity,
    ocrState: OcrUiState,
    onCompleted: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onOcr: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = capture.isCompleted, onCheckedChange = onCompleted)
                Column(Modifier.weight(1f)) {
                    Text(capture.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(capture.category, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir referência") }
            }
            Text(capture.content, style = MaterialTheme.typography.bodyMedium, maxLines = 5)
            if (capture.kind == "image" && capture.imageUri != null) {
                OutlinedButton(onClick = onOcr, enabled = !ocrState.isProcessing) {
                    if (ocrState.isProcessing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Image, null)
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(if (ocrState.isProcessing) "Lendo imagem…" else "Extrair texto da imagem")
                }
                ocrState.message?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (ocrState.isError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChecklistScreen(viewModel: CaptureViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    var title by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(value = title, onValueChange = { title = it }, modifier = Modifier.fillMaxWidth(),
            label = { Text("Nova tarefa") }, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { viewModel.addTask(title); title = "" }), singleLine = true)
        Button(onClick = { viewModel.addTask(title); title = "" }, enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(8.dp)); Text("Adicionar tarefa")
        }
        if (tasks.isEmpty()) Text("Suas tarefas aparecerão aqui.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(tasks, key = { it.id }) { task ->
                TaskCard(task, { viewModel.setTaskCompleted(task.id, it) }, { viewModel.deleteTask(task.id) })
            }
        }
    }
}

@Composable
private fun TaskCard(task: ChecklistTaskEntity, onCompleted: (Boolean) -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.isCompleted, onCheckedChange = onCompleted)
            Text(task.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Excluir tarefa") }
        }
    }
}
