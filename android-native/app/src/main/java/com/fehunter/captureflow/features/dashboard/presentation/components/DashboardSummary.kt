package com.fehunter.captureflow.features.dashboard.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fehunter.captureflow.features.dashboard.domain.model.DashboardSummary
import com.fehunter.captureflow.core.ui.components.NotebookPaper

private const val COLLAPSED_CATEGORY_LIMIT = 5

@Composable
fun DashboardSummaryCard(summary: DashboardSummary) {
    var showAllCategories by remember { mutableStateOf(false) }
    val visibleCategories = if (showAllCategories) summary.categoryCounts else summary.categoryCounts.take(COLLAPSED_CATEGORY_LIMIT)
    val remainingCategories = (summary.categoryCounts.size - COLLAPSED_CATEGORY_LIMIT).coerceAtLeast(0)
    val totalCount = summary.categoryCounts.sumOf { it.totalCount }
    val overallProgress = if (totalCount > 0) summary.completedCount.toFloat() / totalCount else 0f

    NotebookPaper(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("MEU CADERNO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f), fontWeight = FontWeight.Bold, letterSpacing = 1.2f)
                Text("Meu progresso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(summary.completedCount.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("referências concluídas", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(if (summary.completedCount > 0) "Cada pequena conquista conta." else "Suas conquistas aparecerão aqui.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f))
                    }
                }
                LinearProgressIndicator(
                    progress = overallProgress.coerceIn(0f, 1f),
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                )
            }
            if (summary.categoryCounts.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    visibleCategories.forEachIndexed { index, item ->
                        val pendingCount = (item.totalCount - item.completedCount).coerceAtLeast(0)
                        val progress = if (item.totalCount > 0) (item.completedCount.toFloat() / item.totalCount).coerceIn(0f, 1f) else 0f
                        val rowColor = when (index % 3) {
                            0 -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f)
                            1 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.48f)
                            else -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.48f)
                        }
                        Column(modifier = Modifier.fillMaxWidth().background(rowColor, RoundedCornerShape(12.dp)).padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(item.category, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                                Text(item.completedCount.toString() + " concluídas · " + pendingCount + " pendentes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)), color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f))
                        }
                    }
                    if (remainingCategories > 0) {
                        TextButton(onClick = { showAllCategories = !showAllCategories }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showAllCategories) "Recolher categorias" else "Ver todas as categorias (" + summary.categoryCounts.size + ")")
                            androidx.compose.material3.Icon(imageVector = if (showAllCategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }
                    }
                }
            }
            androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
        }
    }
}
