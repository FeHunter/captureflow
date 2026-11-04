package com.fehunter.captureflow.features.fullscreen.presentation.screens

import com.fehunter.captureflow.core.ui.loadCoverBitmap
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
            withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, raw) }.getOrNull() }
        }
    }
    Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (image != null) {
            Image(
                bitmap = image!!,
                contentDescription = "Imagem completa da referência",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.linearGradient(listOf(Color(0xFF2E6BB0), Color(0xFF12335A), Color(0xFF0A1B33))),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color.White.copy(alpha = 0.45f), modifier = Modifier.size(44.dp))
            }
        }

        Box(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(360.dp)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.72f), Color.Black))),
        )

        if (capture.isCompleted) {
            CompletedBadge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 76.dp, end = 16.dp))
        }

        if (showNavigationHint) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 34.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("‹", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.78f))
                Text("Deslize para navegar", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.78f))
                Text("›", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.78f))
            }
        }
    }
}
