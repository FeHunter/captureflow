package com.fehunter.captureflow.features.categories.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Label
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun CategoryFilterSection(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
) {
    var showAllCategories by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            categories.chunked(2).forEach { categoryPair ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    categoryPair.forEach { item ->
                        Surface(
                            onClick = { onCategorySelected(item) },
                            shape = RoundedCornerShape(99.dp),
                            color = if (selectedCategory == item) MaterialTheme.colorScheme.onBackground.copy(alpha = 0.88f)
                            else MaterialTheme.colorScheme.surface.copy(alpha = 0.58f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        ) {
                            Text(
                                item,
                                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                                color = if (selectedCategory == item) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (selectedCategory == item) FontWeight.SemiBold else FontWeight.Normal,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }

        TextButton(onClick = { showAllCategories = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Ver todas as categorias")
        }
    }

    if (showAllCategories) {
        AlertDialog(
            onDismissRequest = { showAllCategories = false },
            title = { Text("Filtrar por categoria") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 380.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    categories.forEach { item ->
                        Surface(
                            onClick = {
                                onCategorySelected(item)
                                showAllCategories = false
                            },
                            shape = MaterialTheme.shapes.medium,
                            color = if (selectedCategory == item) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                                    Icon(Icons.Default.Label, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(9.dp))
                                }
                                Text(item, modifier = Modifier.weight(1f).padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge)
                                if (selectedCategory == item) {
                                    Icon(Icons.Default.Check, contentDescription = "Selecionada", tint = MaterialTheme.colorScheme.primary)
                                } else {
                                    Spacer(Modifier.padding(10.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAllCategories = false }) { Text("Fechar") } },
        )
    }
}
