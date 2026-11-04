package com.fehunter.captureflow.features.categories.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

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

    val dialogBackground = MaterialTheme.colorScheme.surface
    val dialogContent = MaterialTheme.colorScheme.onSurface
    val mutedContent = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outlineVariant

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).widthIn(max = 420.dp),
            shape = RoundedCornerShape(28.dp), color = dialogBackground, contentColor = dialogContent,
            border = BorderStroke(1.dp, outline.copy(alpha = 0.65f)), tonalElevation = 8.dp, shadowElevation = 18.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp).verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Gerenciar categorias", style = MaterialTheme.typography.titleLarge, color = dialogContent)
                    Text("Adicione, renomeie ou remova categorias. As referências existentes acompanham as alterações.",
                        style = MaterialTheme.typography.bodySmall, color = mutedContent)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = newCategoryName, onValueChange = { newCategoryName = it },
                        modifier = Modifier.weight(1f), placeholder = { Text("Nova categoria") }, singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = dialogContent, unfocusedTextColor = dialogContent,
                            focusedBorderColor = accent, unfocusedBorderColor = outline,
                            focusedPlaceholderColor = mutedContent, unfocusedPlaceholderColor = mutedContent, cursorColor = accent,
                        ),
                    )
                    Surface(
                        onClick = { if (newCategoryName.isNotBlank()) { onAdd(newCategoryName.trim()); newCategoryName = "" } },
                        enabled = newCategoryName.isNotBlank(), shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = if (newCategoryName.isNotBlank()) 0.18f else 0.08f), contentColor = accent,
                    ) { Icon(Icons.Default.Add, contentDescription = "Adicionar categoria", modifier = Modifier.padding(13.dp)) }
                }
                HorizontalDivider(color = outline.copy(alpha = 0.8f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    categories.forEach { name ->
                        if (editingCategory == name) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(
                                    value = editedCategoryName, onValueChange = { editedCategoryName = it },
                                    modifier = Modifier.weight(1f), singleLine = true, placeholder = { Text("Nome da categoria") },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = dialogContent, unfocusedTextColor = dialogContent,
                                        focusedBorderColor = accent, unfocusedBorderColor = outline, cursorColor = accent,
                                    ),
                                )
                                IconButton(onClick = { onRename(name, editedCategoryName.trim()); editingCategory = null },
                                    enabled = editedCategoryName.isNotBlank()) {
                                    Icon(Icons.Default.Check, contentDescription = "Salvar nome", tint = accent)
                                }
                                IconButton(onClick = { editingCategory = null }) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancelar edição", tint = mutedContent)
                                }
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge, color = dialogContent)
                                if (name in fixedCategories) {
                                    Surface(shape = RoundedCornerShape(99.dp), color = accent.copy(alpha = 0.14f)) {
                                        Text("Padrão", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            style = MaterialTheme.typography.labelSmall, color = accent)
                                    }
                                } else {
                                    IconButton(onClick = { editingCategory = name; editedCategoryName = name }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Renomear $name", tint = accent)
                                    }
                                    IconButton(onClick = { deleteTarget = name }, enabled = categories.size > 1) {
                                        Icon(Icons.Default.Delete, contentDescription = "Excluir $name",
                                            tint = if (categories.size > 1) MaterialTheme.colorScheme.error else mutedContent.copy(alpha = 0.45f))
                                    }
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Concluir") }
                }
            }
        }
    }
    deleteTarget?.let { name ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null }, containerColor = dialogBackground,
            titleContentColor = dialogContent, textContentColor = mutedContent,
            title = { Text("Excluir categoria?") },
            text = { Text("As referências e os registros históricos em \"$name\" serão realocados para outra categoria.") },
            confirmButton = {
                Button(onClick = { onDelete(name); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }, colors = ButtonDefaults.textButtonColors(contentColor = accent)) { Text("Cancelar") }
            },
        )
    }
}