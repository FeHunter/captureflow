package com.fehunter.captureflow.features.fullscreen.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.OpenInFull
import com.fehunter.captureflow.features.fullscreen.presentation.components.CaptureWebReader
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.core.util.YoutubeVideoIdParser
import com.fehunter.captureflow.features.youtubeplayer.presentation.YoutubeEmbeddedPlayer
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import com.fehunter.captureflow.core.ui.loadCoverBitmap
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
    mediaByCapture: Map<String, List<CaptureMedia>>,
    onSetCover: (String, String) -> Unit,
    onDismiss: () -> Unit,
    onCaptureSelected: (Capture) -> Unit,
    onCompleted: (String, Boolean) -> Unit,
    onTitleChange: (String, String) -> Unit,
    categories: List<String>,
    onCategoryChange: (String, String) -> Unit,
    onOpenNotes: (String) -> Unit,
    onEditNote: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    allSubtasks: List<CaptureSubtask>,
    onAddSubtask: (String, String) -> Unit,
    onSubtaskCompleted: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    var showEditTitle by remember(capture?.id) { mutableStateOf(false) }
    var editedTitle by remember(capture?.id) { mutableStateOf(capture?.title.orEmpty()) }
    var editedCategory by remember(capture?.id) { mutableStateOf(capture?.category.orEmpty()) }
    var showCategoryMenu by remember(capture?.id) { mutableStateOf(false) }
    var showSubtasks by remember(capture?.id) { mutableStateOf(false) }
    var newSubtask by remember(capture?.id) { mutableStateOf("") }
    var expandedNoteImage by remember(capture?.id) { mutableStateOf<ImageBitmap?>(null) }
    var showWebReader by remember(capture?.id) { mutableStateOf(false) }

    capture?.let { selected ->
        val viewerCaptures = captures.filter { it.category == selected.category && it.isCompleted == selected.isCompleted }
        val initialPage = viewerCaptures.indexOfFirst { it.id == selected.id }.coerceAtLeast(0)
        val sourceUrl = selected.sourceUrl?.takeIf { url ->
            runCatching {
                val uri = java.net.URI(url)
                uri.scheme?.lowercase() in setOf("http", "https") && !uri.host.isNullOrBlank() && uri.userInfo == null
            }.getOrDefault(false)
        }
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = false),
        ) {
            BackHandler(enabled = expandedNoteImage == null && !showWebReader) { onDismiss() }
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
            if (showWebReader && sourceUrl != null) {
                CaptureWebReader(url = sourceUrl, title = selected.title, onClose = { showWebReader = false })
            } else Surface(Modifier.fillMaxSize(), color = Color.Black) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 8.dp, vertical = 7.dp),
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
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    selected.category,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
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
                                Icon(Icons.Default.Link, contentDescription = "Abrir link original", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        sourceUrl?.let { url ->
                            if (YoutubeVideoIdParser.extractVideoId(url) == null) {
                                IconButton(
                                    onClick = { showWebReader = true },
                                    modifier = Modifier.size(44.dp),
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = "Visualizar conteúdo no CaptureFlow", tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                        IconButton(onClick = onDismiss, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Fechar visualizador", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                    Text(
                        text = viewerCaptures.getOrNull(pagerState.settledPage)?.title ?: selected.title,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        val activeCapture = viewerCaptures.getOrNull(pagerState.settledPage)
                        val activeYoutubeVideoId = remember(activeCapture?.sourceUrl) {
                            YoutubeVideoIdParser.extractVideoId(activeCapture?.sourceUrl)
                        }
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize(),
                            key = { page -> viewerCaptures[page].id },
                        ) { page ->
                            val pageCapture = viewerCaptures[page]
                            val pageMedia = mediaByCapture[pageCapture.id].orEmpty()
                            var mediaIndex by remember(pageCapture.id) {
                                mutableIntStateOf(pageMedia.indexOfFirst { it.isCover }.coerceAtLeast(0))
                            }
                            LaunchedEffect(pageCapture.id, pageMedia) {
                                mediaIndex = pageMedia.indexOfFirst { it.isCover }.coerceAtLeast(0)
                            }
                            val imageSwipeModifier = if (pageMedia.size > 1) {
                                Modifier.pointerInput(pageCapture.id, pageMedia.size) {
                                    var verticalDistance = 0f
                                    val swipeThreshold = 48.dp.toPx()
                                    detectVerticalDragGestures(
                                        onVerticalDrag = { change, dragAmount ->
                                            verticalDistance += dragAmount
                                            change.consume()
                                        },
                                        onDragEnd = {
                                            when {
                                                verticalDistance <= -swipeThreshold ->
                                                    mediaIndex = (mediaIndex + 1).coerceAtMost(pageMedia.lastIndex)
                                                verticalDistance >= swipeThreshold ->
                                                    mediaIndex = (mediaIndex - 1).coerceAtLeast(0)
                                            }
                                            verticalDistance = 0f
                                        },
                                        onDragCancel = { verticalDistance = 0f },
                                    )
                                }
                            } else Modifier
                            Box(Modifier.fillMaxSize().then(imageSwipeModifier)) {
                                val activeMedia = pageMedia.getOrNull(mediaIndex)
                                FullscreenCapturePage(
                                    capture = pageCapture.copy(imageUri = activeMedia?.uri ?: pageCapture.imageUri),
                                    showNavigationHint = page == pagerState.currentPage,
                                )
                                if (pageMedia.size > 1) {
                                    Row(
                                        modifier = Modifier.align(Alignment.Center).fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 3.dp,
                                        ) {
                                            TextButton(
                                                onClick = { mediaIndex = (mediaIndex - 1).coerceAtLeast(0) },
                                                enabled = mediaIndex > 0,
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                ),
                                            ) { Text("‹", style = MaterialTheme.typography.headlineLarge) }
                                        }
                                        Surface(
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                            shadowElevation = 3.dp,
                                        ) {
                                            TextButton(
                                                onClick = { mediaIndex = (mediaIndex + 1).coerceAtMost(pageMedia.lastIndex) },
                                                enabled = mediaIndex < pageMedia.lastIndex,
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.primary,
                                                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
                                                ),
                                            ) { Text("›", style = MaterialTheme.typography.headlineLarge) }
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 56.dp),
                                        color = Color.Black.copy(alpha = 0.72f),
                                        shape = MaterialTheme.shapes.extraLarge,
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        ) {
                                            Text(
                                                "${mediaIndex + 1} / ${pageMedia.size}",
                                                color = Color.White,
                                                style = MaterialTheme.typography.labelMedium,
                                            )
                                            if (activeMedia != null && !activeMedia.isCover) {
                                                TextButton(onClick = { onSetCover(pageCapture.id, activeMedia.id) }) {
                                                    Text("Definir capa", color = Color.White)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        // Keep Chromium's video surface outside the transformed pager page.
                        if (pagerState.currentPage == pagerState.settledPage) {
                            activeYoutubeVideoId?.let { videoId ->
                                YoutubeEmbeddedPlayer(
                                    videoId = videoId,
                                    modifier = Modifier.align(Alignment.Center)
                                        .fillMaxWidth().padding(horizontal = 8.dp),
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 20.dp)
                                .fillMaxWidth(0.78f),
                            horizontalAlignment = Alignment.Start,
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val thoughtNotes = remember(selected.notes) { CaptureNotesCodec.decode(selected.notes) }
                            if (thoughtNotes.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 248.dp).verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    thoughtNotes.forEach { note ->
                                        Surface(
                                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                                onClick = { },
                                                onLongClick = { onEditNote(selected.id, note.id) },
                                            ),
                                            shape = MaterialTheme.shapes.medium,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                            shadowElevation = 4.dp,
                                        ) {
                                            Column(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                if (note.text.isNotBlank()) {
                                                    Text(
                                                        text = note.text,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface,
                                                    )
                                                }
                                                note.imageUri?.let { rawUri ->
                                                    FullscreenNoteImage(
                                                        rawUri = rawUri,
                                                        onExpand = { image -> expandedNoteImage = image },
                                                        onLongPress = { onEditNote(selected.id, note.id) },
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            Surface(
                                shape = MaterialTheme.shapes.extraLarge,
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                shadowElevation = 8.dp,
                            ) {
                                IconButton(
                                    onClick = { onOpenNotes(selected.id) },
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        Icons.Default.ChatBubbleOutline,
                                        contentDescription = if (thoughtNotes.isEmpty()) "Adicionar anotações" else "Editar anotações",
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                        }
                        Surface(
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 20.dp),
                            shape = MaterialTheme.shapes.extraLarge,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            shadowElevation = 8.dp,
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (selected.isCompleted) {
                                    Checkbox(
                                        checked = true,
                                        onCheckedChange = { onCompleted(selected.id, it) },
                                        modifier = Modifier.size(48.dp),
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                } else {
                                    Checkbox(
                                        checked = false,
                                        onCheckedChange = { onCompleted(selected.id, it) },
                                        modifier = Modifier.size(48.dp),
                                        colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary, checkmarkColor = MaterialTheme.colorScheme.onPrimary, uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant),
                                    )
                                    TextButton(
                                        onClick = { showSubtasks = true },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    ) {
                                        val selectedSubtasks = allSubtasks.filter { it.captureId == selected.id }
                                        Text(
                                            "☰ ${selectedSubtasks.count { it.isCompleted }}/${selectedSubtasks.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                        )
                                    }
                                    sourceUrl?.let { url ->
                                        IconButton(
                                            onClick = { runCatching { uriHandler.openUri(url) } },
                                            modifier = Modifier.size(48.dp),
                                        ) {
                                            Icon(
                                                Icons.Default.Link,
                                                contentDescription = "Abrir link original",
                                                tint = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { editedTitle = selected.title; editedCategory = selected.category; showEditTitle = true },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Editar título", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { onDelete(selected.id); onDismiss() },
                                        modifier = Modifier.size(48.dp),
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir captura", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSubtasks && capture != null) {
        val selectedSubtasks = allSubtasks.filter { it.captureId == capture.id }
        ModalBottomSheet(
            onDismissRequest = { showSubtasks = false },
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.onSurfaceVariant) },
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .fillMaxHeight(0.88f)
                    .imePadding()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Sub-checklist", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${selectedSubtasks.count { it.isCompleted }} de ${selectedSubtasks.size} concluídas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { showSubtasks = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar sub-checklist")
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
                        placeholder = { Text("Adicionar uma subtarefa") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.large,
                    )
                    FilledIconButton(
                        onClick = {
                            onAddSubtask(capture.id, newSubtask)
                            newSubtask = ""
                        },
                        enabled = newSubtask.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Adicionar subtarefa")
                    }
                }
                val completedSubtasks = selectedSubtasks.count { it.isCompleted }
                val progress = if (selectedSubtasks.isNotEmpty()) completedSubtasks.toFloat() / selectedSubtasks.size else 0f
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    selectedSubtasks.forEach { subtask ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
                            ) {
                                Checkbox(checked = subtask.isCompleted, onCheckedChange = { onSubtaskCompleted(subtask.id, it) })
                                Text(
                                    subtask.title,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                                )
                                IconButton(onClick = { onDeleteSubtask(subtask.id) }) {
                                    Icon(Icons.Default.Close, contentDescription = "Excluir item", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showEditTitle && capture != null) {
        AlertDialog(
            onDismissRequest = { showEditTitle = false },
            title = { Text("Editar título") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = editedTitle,
                        onValueChange = { editedTitle = it.take(160) },
                        label = { Text("Título principal") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Categoria", style = MaterialTheme.typography.labelLarge)
                        Box {
                            OutlinedButton(
                                onClick = { showCategoryMenu = true },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(
                                    editedCategory,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Icon(Icons.Default.ExpandMore, contentDescription = "Selecionar categoria")
                            }
                            DropdownMenu(
                                expanded = showCategoryMenu,
                                onDismissRequest = { showCategoryMenu = false },
                            ) {
                                categories.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item) },
                                        onClick = {
                                            editedCategory = item
                                            showCategoryMenu = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val title = editedTitle.trim()
                        if (title.isNotBlank()) {
                            onTitleChange(capture.id, title)
                            if (editedCategory != capture.category) {
                                onCategoryChange(capture.id, editedCategory)
                            }
                            onCaptureSelected(capture.copy(title = title, category = editedCategory))
                            showEditTitle = false
                        }
                    },
                    enabled = editedTitle.isNotBlank(),
                ) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showEditTitle = false }) { Text("Cancelar") } },
        )
    }

    expandedNoteImage?.let { image ->
        FullscreenNoteImageDialog(
            image = image,
            onDismiss = { expandedNoteImage = null },
        )
    }
}


@Composable
private fun FullscreenNoteImage(
    rawUri: String,
    onExpand: (ImageBitmap) -> Unit,
    onLongPress: () -> Unit,
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, rawUri) {
        value = withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, rawUri) }.getOrNull() }
    }
    image?.let { bitmap ->
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 52.dp)
                .combinedClickable(
                    onClick = { onExpand(bitmap) },
                    onLongClick = onLongPress,
                ),
        ) {
            Image(
                bitmap = bitmap,
                contentDescription = "Abrir imagem da anotação",
                modifier = Modifier.fillMaxWidth().heightIn(max = 52.dp),
                contentScale = ContentScale.Fit,
            )
            Icon(
                imageVector = Icons.Default.OpenInFull,
                contentDescription = "Expandir imagem",
                tint = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .background(Color.Black.copy(alpha = 0.72f), MaterialTheme.shapes.small)
                    .padding(5.dp),
            )
        }
    }
}

@Composable
private fun FullscreenNoteImageDialog(image: ImageBitmap, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.96f)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                    bitmap = image,
                    contentDescription = "Imagem da anotação em tela cheia",
                    modifier = Modifier.fillMaxSize().padding(20.dp),
                    contentScale = ContentScale.Fit,
                )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
                    .background(Color.Black.copy(alpha = 0.65f), MaterialTheme.shapes.extraLarge),
            ) {
                Icon(Icons.Default.Close, contentDescription = "Fechar imagem", tint = Color.White)
            }
        }
    }
}
