package com.fehunter.captureflow.features.captures.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.fehunter.captureflow.core.ui.loadCoverBitmap
import com.fehunter.captureflow.core.ui.components.TaskPaperCover
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.presentation.model.CaptureImageProcessingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun CategoryThumbnail(capture: Capture, ocrState: CaptureImageProcessingStatus, onClick: () -> Unit, onDelete: (() -> Unit)? = null) {
    val context = LocalContext.current
    val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, capture.imageUri) {
        value = capture.imageUri?.let { raw -> withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, raw) }.getOrNull() } }
    }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(180)).clickable(onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column {
            Box(Modifier.fillMaxWidth().aspectRatio(0.92f).background(MaterialTheme.colorScheme.surfaceVariant)) {
                Crossfade(targetState = bitmap, animationSpec = tween(180), label = "task-cover") { loadedBitmap ->
                    if (loadedBitmap != null) {
                        Image(bitmap = loadedBitmap, contentDescription = capture.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else TaskPaperCover(modifier = Modifier.fillMaxSize())
                }
                CaptureStatusBadge(isCompleted = capture.isCompleted, modifier = Modifier.align(Alignment.TopStart).padding(6.dp))
                Crossfade(
                    targetState = ocrState.isProcessing || ocrState.isError,
                    animationSpec = tween(140),
                    label = "ocr-status",
                ) { showStatus ->
                    if (showStatus) {
                        Surface(
                            modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                            shape = RoundedCornerShape(7.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Text(if (ocrState.isError) "⚠" else "◌", modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = if (ocrState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                if (capture.isCompleted && onDelete != null) {
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(5.dp),
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shadowElevation = 2.dp,
                    ) {
                        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Delete, contentDescription = "Excluir tarefa concluída", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Text(capture.title, modifier = Modifier.fillMaxWidth().padding(horizontal = 9.dp, vertical = 9.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
        }
    }
}

@Composable
fun CategoryCard(
    title: String,
    captures: List<Capture>,
    totalCount: Int,
    completedCount: Int,
    ocrStates: Map<String, CaptureImageProcessingStatus>,
    onExpand: () -> Unit,
    onOpenCapture: (Capture) -> Unit,
) {
    val context = LocalContext.current
    val progress = if (totalCount > 0) completedCount.toFloat() / totalCount else 0f
    val animatedProgress by animateFloatAsState(progress, animationSpec = tween(420), label = "category-progress")
    val accent = when (title.lowercase()) {
        "ler depois" -> MaterialTheme.colorScheme.primary
        "verificar" -> MaterialTheme.colorScheme.secondary
        "salvar" -> MaterialTheme.colorScheme.tertiary
        "importante" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val accentContainer = when (title.lowercase()) {
        "ler depois" -> MaterialTheme.colorScheme.primaryContainer
        "verificar" -> MaterialTheme.colorScheme.secondaryContainer
        "salvar" -> MaterialTheme.colorScheme.tertiaryContainer
        "importante" -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().animateContentSize(tween(220)),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().background(accentContainer.copy(alpha = 0.72f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                    CircularProgressIndicator(
                        progress = animatedProgress,
                        modifier = Modifier.fillMaxSize(),
                        color = accent,
                        trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                        strokeWidth = 4.dp,
                    )
                    Icon(Icons.Default.Image, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(totalCount.toString() + " referências nesta seção", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onExpand, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text("Ver todas", color = accent, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        Text("abrir seção", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text((totalCount - completedCount).coerceAtLeast(0).toString() + " pendentes", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                }
                Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(completedCount.toString() + " concluídas", modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Medium)
                }
                Spacer(Modifier.weight(1f))
                Text((animatedProgress * 100).toInt().toString() + "%", style = MaterialTheme.typography.labelMedium, color = accent, fontWeight = FontWeight.Bold)
            }
            androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(horizontal = 14.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
            if (captures.isEmpty()) {
                Text("Nenhuma referência pendente nesta categoria.", modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 22.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    captures.take(3).forEach { capture ->
                        val bitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, capture.imageUri) {
                            value = capture.imageUri?.let { raw -> withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, raw) }.getOrNull() } }
                        }
                        val loadedBitmap = bitmap
                        Card(
                            modifier = Modifier.weight(1f).clickable { onOpenCapture(capture) },
                            shape = RoundedCornerShape(13.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.85f)),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1.12f)
                                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Crossfade(targetState = loadedBitmap, animationSpec = tween(180), label = "category-preview-cover") { preview ->
                                        if (preview != null) Image(bitmap = preview, contentDescription = capture.title, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                        else TaskPaperCover(modifier = Modifier.fillMaxSize())
                                    }
                                    if (capture.isCompleted) {
                                        CompletedBadge(modifier = Modifier.align(Alignment.TopStart).padding(4.dp))
                                    }
                                    val ocrState = ocrStates[capture.id]
                                    if (ocrState?.isProcessing == true || ocrState?.isError == true) {
                                        Surface(
                                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                                            shape = MaterialTheme.shapes.small,
                                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                        ) {
                                            Text(
                                                if (ocrState.isError) "⚠" else "◌",
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (ocrState.isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }
                                Text(
                                    capture.title,
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 7.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 2,
                                    minLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    repeat((3 - minOf(captures.size, 3)).coerceAtLeast(0)) {
                        Spacer(Modifier.weight(1f).aspectRatio(0.92f))
                    }
                }
            }
        }
    }
}
