package com.fehunter.captureflow.features.fullscreen.presentation.screens

import com.fehunter.captureflow.core.ui.loadCoverBitmap
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.presentation.components.CompletedBadge
import com.fehunter.captureflow.core.util.YoutubeVideoIdParser
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FullscreenCapturePage(
    capture: Capture,
    showNavigationHint: Boolean,
) {
    val context = LocalContext.current
    val youtubeVideoId = remember(capture.sourceUrl) {
        YoutubeVideoIdParser.extractVideoId(capture.sourceUrl)
    }
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null, capture.imageUri, youtubeVideoId,
    ) {
        value = if (youtubeVideoId != null) {
            null
        } else {
            capture.imageUri?.let { raw ->
                withContext(Dispatchers.IO) {
                    runCatching { loadCoverBitmap(context, raw) }.getOrNull()
                }
            }
        }
    }

    if (youtubeVideoId != null) {
        // Keep the embedded player visually isolated: title and status are laid out
        // outside the iframe viewport instead of being drawn over its video surface.
        Column(
            modifier = Modifier.fillMaxSize().background(Color.Black)
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = capture.title,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 8.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            if (capture.isCompleted) {
                CompletedBadge(modifier = Modifier.padding(top = 4.dp))
            }
            // The WebView is hosted by FullscreenCaptureDialog outside HorizontalPager.
            // This page reserves the center for the sibling player without transforming it.
            Spacer(Modifier.weight(1f))
            if (showNavigationHint) {
                Text(
                    "‹ Deslize para navegar ›",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }
        }
        return
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    Icons.Default.Image,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Esta referência não possui imagem.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (capture.isCompleted) {
            CompletedBadge(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 16.dp),
            )
        }
        Box(
            modifier = Modifier.align(Alignment.TopCenter).fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.99f),
                            Color.Black.copy(alpha = 0.94f),
                            Color.Black.copy(alpha = 0.78f),
                            Color.Black.copy(alpha = 0.42f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 38.dp),
        ) {
            Text(
                text = capture.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
        if (showNavigationHint) {
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
