package com.fehunter.captureflow.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp

/** Lightweight, theme-aware ruled-paper cover for references created without an image. */
@Composable
fun TaskPaperCover(modifier: Modifier = Modifier, prominent: Boolean = false) {
    val paperColor = MaterialTheme.colorScheme.surfaceVariant
    val ruleColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.72f)
    val marginColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.48f)
    val iconColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    val lineSpacing = if (prominent) 32.dp else 19.dp
    val leftMargin = if (prominent) 30.dp else 16.dp
    Box(
        modifier = modifier.background(paperColor).drawBehind {
            val gap = lineSpacing.toPx()
            var y = gap
            while (y < size.height) {
                drawLine(ruleColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                y += gap
            }
            val marginX = leftMargin.toPx()
            drawLine(marginColor, Offset(marginX, 0f), Offset(marginX, size.height), strokeWidth = if (prominent) 1.5.dp.toPx() else 1.dp.toPx())
        },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = if (prominent) 32.dp else 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (prominent) 10.dp else 4.dp),
        ) {
            Icon(Icons.Default.Edit, contentDescription = null, tint = iconColor)
            Text(
                text = if (prominent) "Tarefa sem imagem" else "Anotação",
                style = if (prominent) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
                color = textColor,
                maxLines = 1,
            )
        }
    }
}
