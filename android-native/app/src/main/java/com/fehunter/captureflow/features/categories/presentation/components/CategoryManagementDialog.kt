package com.fehunter.captureflow.features.categories.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun CategoryManagementDialog(
    categories: List<String>,
    fixedCategories: Set<String> = emptySet(),
    onAdd: (String) -> Unit,
    onRename: (String, String) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var newCategoryName by remember { mutableStateOf("") }
    var editingCategory by remember { mutableStateOf<String?>(null) }
    var editedCategoryName by remember { mutableStateOf("") }
    var deleteTarget by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gerenciar categorias") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Adicione, renomeie ou remova categorias. As referências existentes acompanham as alterações.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Nova categoria") },
                        singleLine = true,
                    )
                    IconButton(
                        onClick = {
                            onAdd(newCategoryName)
                            newCategoryName = ""
                        },
                        enabled = newCategoryName.isNotBlank(),
                    ) { Icon(Icons.Default.Add, contentDescription = "Adicionar categoria") }
                }
                HorizontalDivider()
                categories.forEach { name ->
                    if (editingCategory == name) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = editedCategoryName,
                                onValueChange = { editedCategoryName = it },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Nome da categoria") },
                            )
                            IconButton(onClick = {
                                onRename(name, editedCategoryName)
                                editingCategory = null
                            }, enabled = editedCategoryName.isNotBlank()) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Salvar nome")
                            }
                            IconButton(onClick = { editingCategory = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancelar edição")
                            }
                        }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                            if (name in fixedCategories) {
                                Text("Padrão", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            } else {
                                IconButton(onClick = {
                                    editingCategory = name
                                    editedCategoryName = name
                                }) { Icon(Icons.Default.Edit, contentDescription = "Renomear $name") }
                                IconButton(onClick = { deleteTarget = name }, enabled = categories.size > 1) {
                                    Icon(Icons.Default.Delete, contentDescription = "Excluir $name")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Concluir") } },
    )

    deleteTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Excluir categoria?") },
            text = { Text("As referências e os registros históricos em \"$name\" serão realocados para outra categoria.") },
            confirmButton = {
                Button(onClick = {
                    onDelete(name)
                    deleteTarget = null
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancelar") } },
        )
    }
}
