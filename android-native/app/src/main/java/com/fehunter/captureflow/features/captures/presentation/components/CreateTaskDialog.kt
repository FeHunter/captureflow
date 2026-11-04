package com.fehunter.captureflow.features.captures.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CreateTaskDialog(
    categories: List<String>,
    onDismiss: () -> Unit,
    onCreate: (title: String, category: String) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var selectedCategory by remember(categories) {
        mutableStateOf(categories.firstOrNull() ?: "Ler depois")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nova tarefa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    "Registre uma ideia ou algo para fazer. Você pode adicionar uma imagem depois, se quiser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(160) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("O que você precisa lembrar?") },
                    placeholder = { Text("Escreva sua tarefa…") },
                    maxLines = 3,
                    shape = RoundedCornerShape(14.dp),
                )
                Text("Categoria", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { category ->
                        FilterChip(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            label = { Text(category, maxLines = 1) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title.trim(), selectedCategory) },
                enabled = title.isNotBlank() && selectedCategory in categories,
            ) { Text("Salvar tarefa") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(22.dp),
    )
}
