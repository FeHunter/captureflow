package com.fehunter.captureflow.features.fullscreen.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import androidx.compose.material.icons.filled.Add
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collect

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullscreenCaptureDialog(
    capture: Capture?,
    captures: List<Capture>,
    onDismiss: () -> Unit,
    onCaptureSelected: (Capture) -> Unit,
    onCompleted: (String, Boolean) -> Unit,
    onTitleChange: (String, String) -> Unit,
    onNotesChange: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    allSubtasks: List<CaptureSubtask>,
    onAddSubtask: (String, String) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showEditTitle by remember(capture?.id) { mutableStateOf(false) }
    var showNotes by remember(capture?.id) { mutableStateOf(false) }
    var editedTitle by remember(capture?.id) { mutableStateOf(capture?.title.orEmpty()) }
    var editedNotes by remember(capture?.id) { mutableStateOf(capture?.notes.orEmpty()) }
    var showSubtasks by remember(capture?.id) { mutableStateOf(false) }
    var newSubtask by remember(capture?.id) { mutableStateOf("") }

    capture?.let { selected ->
        val viewerCaptures = captures.filter { it.category == selected.category }
        val initialPage = viewerCaptures.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
        val sourceUrl = selected.sourceUrl?.takeIf { url ->
            runCatching {
                java.net.URI(url).scheme?.lowercase() in setOf("http", "https")
            }.getOrDefault(false)
        }
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
        ) {
            BackHandler { onDismiss() }
            val pagerState = rememberPagerState(
                initialPage = initialPage,
                pageCount = { viewerCaptures.size },
            )
            LaunchedEffect(selected.id, viewerCaptures) {
                val targetPage = viewerCaptures.indexOfFirst { it.id == selected.id }
                if (targetPage in viewerCaptures.indices && pagerState.currentPage != targetPage) {
                    pagerState.scrollToPage(targetPage)
                }
            }
            LaunchedEffect(pagerState, viewerCaptures) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    viewerCaptures.getOrNull(page)?.let(onCaptureSelected)
                }
            }
            Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f).padding(start = 8.dp)) {
                            Text(
                                SimpleDateFormat("dd/MM/yyyy · HH:mm", Locale.getDefault())
                                    .format(Date(selected.createdAt)),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                            ) {
                                Text(
                                    selected.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                )
                            }
                        }
                        sourceUrl?.let { url ->
                            IconButton(
                                onClick = {
                                    runCatching { uriHandler.openUri(url) }
                                },
                                modifier = Modifier.size(44.dp),
                            ) {
                                Icon(Icons.Default.Link, contentDescription = "Abrir link original", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar visualizador", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { page -> viewerCaptures[page].id },
                        ) { page ->
                            val pageCapture = viewerCaptures[page]
                            Box(Modifier.fillMaxSize()) {
                                FullscreenCapturePage(
                                    capture = pageCapture,
                                    showNavigationHint = page == pagerState.currentPage,
                                )
                                pageCapture.notes?.trim()?.takeIf(String::isNotEmpty)?.let { note ->
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomStart)
                                            .padding(start = 16.dp, bottom = 20.dp, end = 88.dp)
                                            .fillMaxWidth(0.78f),
                                        shape = MaterialTheme.shapes.medium,
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shadowElevation = 4.dp,
                                    ) {
                                        Text(
                                            note,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 20.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                            shadowElevation = 8.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                Checkbox(
                                    checked = selected.isCompleted,
                                    onCheckedChange = { onCompleted(selected.id, it) },
                                    modifier = Modifier.size(48.dp),
                                )
                                TextButton(
                                    onClick = { showSubtasks = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                ) {
                                    val selectedSubtasks = allSubtasks.filter { it.captureId == selected.id }
                                    Text(
                                        "☰ ${selectedSubtasks.count { it.isCompleted }}/${selectedSubtasks.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        maxLines = 1,
                                    )
                                }
                                IconButton(
                                    onClick = { editedTitle = selected.title; showEditTitle = true },
                                    modifier = Modifier.size(48.dp),
                                ) { Icon(Icons.Default.Edit, contentDescription = "Editar título", tint = MaterialTheme.colorScheme.onSurface) }
                                IconButton(
                                    onClick = { editedNotes = selected.notes.orEmpty(); showNotes = true },
                                    modifier = Modifier.size(48.dp),
                                ) { Icon(Icons.Default.Description, contentDescription = "Anotações", tint = MaterialTheme.colorScheme.onSurface) }
                                IconButton(
                                    onClick = { onDelete(selected.id); onDismiss() },
                                    modifier = Modifier.size(48.dp),
                                ) { Icon(Icons.Default.Delete, contentDescription = "Excluir captura", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubtasks && capture != null) {
        val selectedSubtasks = allSubtasks.filter { it.captureId == capture.id }
        ModalBottomSheet(onDismissRequest = { showSubtasks = false }) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Sub-checklist · ${selectedSubtasks.count { it.isCompleted }}/${selectedSubtasks.size} concluídas",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(onClick = { showSubtasks = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar sub-checklist")
                    }
                }
                selectedSubtasks.forEach { subtask ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = subtask.isCompleted,
                            onCheckedChange = { onSubtaskCompleted(subtask.id, it) },
                        )
                        Text(
                            subtask.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurface,
                        )
                        IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir subtarefa")
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newSubtask,
                        onValueChange = { newSubtask = it.take(160) },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Nova subtarefa") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            onAddSubtask(capture.id, newSubtask)
                            newSubtask = ""
                        },
                        enabled = newSubtask.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar subtarefa")
                    }
                }
            }
        }
    }

    if (showNotes && capture != null) {
        AlertDialog(
            onDismissRequest = { showNotes = false },
            title = { Text("Anotações") },
            text = {
                OutlinedTextField(
                    value = editedNotes,
                    onValueChange = { editedNotes = it.take(10_000) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Escreva suas anotações sobre esta referência…") },
                    minLines = 5,
                    maxLines = 10,
                    supportingText = { Text("${editedNotes.length}/10000") },
                )
            },
            confirmButton = {
                Button(onClick = {
                    onNotesChange(capture.id, editedNotes)
                    onCaptureSelected(capture.copy(notes = editedNotes.trim().takeIf(String::isNotBlank)))
                    showNotes = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showNotes = false }) { Text("Cancelar") } },
        )
    }

    if (showEditTitle && capture != null) {
        AlertDialog(
            onDismissRequest = { showEditTitle = false },
            title = { Text("Editar título") },
            text = {
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it.take(160) },
                    label = { Text("Título principal") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = editedTitle.trim()
                        if (title.isNotBlank()) {
                            onTitleChange(capture.id, title)
                            onCaptureSelected(capture.copy(title = title))
                            showEditTitle = false
                        }
                    },
                    enabled = editedTitle.isNotBlank(),
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showEditTitle = false }) { Text("Cancelar") } },
        )
    }
}
