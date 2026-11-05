package com.fehunter.captureflow.features.fullscreen.presentation.screens

import com.fehunter.captureflow.core.ui.loadCoverBitmap
import com.fehunter.captureflow.core.ui.components.TaskPaperCover
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.presentation.components.CompletedBadge
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun FullscreenCapturePage(capture: Capture, showNavigationHint: Boolean) {
    val context = LocalContext.current
    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(initialValue = null, capture.imageUri) {
        value = capture.imageUri?.let { raw ->
            withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, raw, maxDimension = 1600) }.getOrNull() }
        }
    }
    Box(
        Modifier.fillMaxSize().background(
            if (image != null) Color.Black else MaterialTheme.colorScheme.background,
        ),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = image, animationSpec = tween(200), label = "fullscreen-cover") { loadedImage ->
            if (loadedImage != null) {
                Image(bitmap = loadedImage, contentDescription = "Imagem completa da referência", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            } else {
                TaskPaperCover(modifier = Modifier.fillMaxSize(), prominent = true)
            }
        }

        if (image != null) {
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(360.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black))),
            )
        }

        if (capture.isCompleted) {
            CompletedBadge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 16.dp))
        }

        AnimatedVisibility(
            visible = showNavigationHint,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
            enter = fadeIn(tween(160)),
            exit = fadeOut(tween(160)),
        ) {
            val hintColor = if (image != null) Color.White.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("‹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hintColor)
                Text("Deslize para navegar", style = MaterialTheme.typography.labelMedium, color = hintColor)
                Text("›", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = hintColor)
            }
        }
    }
}
