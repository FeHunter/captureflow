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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fehunter.captureflow.features.dashboard.domain.model.DashboardSummary

private const val COLLAPSED_CATEGORY_LIMIT = 5

@Composable
fun DashboardSummaryCard(summary: DashboardSummary) {
    var showAllCategories by remember { mutableStateOf(false) }
    val visibleCategories = if (showAllCategories) summary.categoryCounts else summary.categoryCounts.take(COLLAPSED_CATEGORY_LIMIT)
    val remainingCategories = (summary.categoryCounts.size - COLLAPSED_CATEGORY_LIMIT).coerceAtLeast(0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color(0xFF1F6F83), Color(0xFF12335A), Color(0xFF0A1B33))))
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Meu progresso", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(summary.completedCount.toString(), style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("referências concluídas", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                    Text(
                        if (summary.completedCount > 0) "Continue assim." else "Suas conquistas aparecerão aqui.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.76f),
                    )
                }
            }
            if (summary.categoryCounts.isNotEmpty()) {
                visibleCategories.forEach { item ->
                    val pendingCount = (item.totalCount - item.completedCount).coerceAtLeast(0)
                    val progress = if (item.totalCount > 0) (item.completedCount.toFloat() / item.totalCount).coerceIn(0f, 1f) else 0f
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(item.category, style = MaterialTheme.typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Medium)
                            Text(
                                "${item.completedCount} concluídas · ${pendingCount} pendentes",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            color = Color(0xFF8EEAF5),
                            trackColor = Color.White.copy(alpha = 0.2f),
                        )
                    }
                }
                if (remainingCategories > 0) {
                    TextButton(onClick = { showAllCategories = !showAllCategories }) {
                        Text(if (showAllCategories) "Recolher categorias" else "Ver todas as categorias (${summary.categoryCounts.size})", color = Color.White)
                        androidx.compose.material3.Icon(
                            imageVector = if (showAllCategories) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color.White,
                        )
                    }
                }
            }
        }
    }
}
