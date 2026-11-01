package com.fehunter.captureflow.features.categories.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Displays every category in two rows inside one horizontally scrollable container.
 * The dialog remains available as an alternative for selecting from the full list.
 */
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
                        FilterChip(
                            selected = selectedCategory == item,
                            onClick = { onCategorySelected(item) },
                            label = {
                                Text(
                                    item,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }
        }

        TextButton(
            onClick = { showAllCategories = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Ver todas as categorias")
        }
    }

    if (showAllCategories) {
        AlertDialog(
            onDismissRequest = { showAllCategories = false },
            title = { Text("Filtrar por categoria") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    categories.forEach { item ->
                        FilterChip(
                            modifier = Modifier.fillMaxWidth(),
                            selected = selectedCategory == item,
                            onClick = {
                                onCategorySelected(item)
                                showAllCategories = false
                            },
                            label = { Text(item) },
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAllCategories = false }) {
                    Text("Fechar")
                }
            },
        )
    }
}
