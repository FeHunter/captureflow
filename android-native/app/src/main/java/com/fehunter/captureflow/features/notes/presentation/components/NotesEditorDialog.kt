package com.fehunter.captureflow.features.notes.presentation.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Alignment
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.fehunter.captureflow.core.ui.loadCoverBitmap
import com.fehunter.captureflow.features.captures.domain.model.CaptureNote
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MAX_NOTE_LENGTH = 10_000

@Composable
fun NotesEditorDialog(
    captureTitle: String,
    initialNotes: String,
    initialEditingNoteId: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onCompressImage: (String, (String?) -> Unit) -> Unit,
    onDeleteTemporaryImage: (String) -> Unit,
) {
    var notes by remember(captureTitle, initialNotes) { mutableStateOf(NotesCodec.decode(initialNotes)) }
    var editingId by remember(captureTitle, initialEditingNoteId) { mutableStateOf(initialEditingNoteId) }
    var draftText by remember(captureTitle, initialEditingNoteId, initialNotes) {
        mutableStateOf(notes.firstOrNull { it.id == initialEditingNoteId }?.text.orEmpty())
    }
    var draftImageUri by remember(captureTitle, initialEditingNoteId, initialNotes) {
        mutableStateOf(notes.firstOrNull { it.id == initialEditingNoteId }?.imageUri)
    }
    var isCompressingImage by remember { mutableStateOf(false) }
    var generatedImageUris by remember { mutableStateOf(emptySet<String>()) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { selected ->
            isCompressingImage = true
            onCompressImage(selected.toString()) { compressed ->
                draftImageUri = compressed ?: draftImageUri
                if (compressed != null) generatedImageUris = generatedImageUris + compressed
                isCompressingImage = false
            }
        }
    }

    fun beginCreate() {
        editingId = "new"
        draftText = ""
        draftImageUri = null
    }
    fun beginEdit(note: CaptureNote) {
        editingId = note.id
        draftText = note.text
        draftImageUri = note.imageUri
    }
    fun commitDraft() {
        val text = draftText.trim().take(MAX_NOTE_LENGTH)
        if (text.isBlank() && draftImageUri.isNullOrBlank()) {
            if (editingId != "new") notes = notes.filterNot { it.id == editingId }
        } else {
            val existing = notes.firstOrNull { it.id == editingId }
            val item = CaptureNote(
                id = existing?.id ?: UUID.randomUUID().toString(),
                text = text,
                imageUri = draftImageUri,
                createdAt = existing?.createdAt ?: System.currentTimeMillis(),
            )
            notes = if (existing == null) notes + item else notes.map { if (it.id == item.id) item else it }
        }
        editingId = null
    }

    Dialog(
        onDismissRequest = {
            generatedImageUris.forEach(onDeleteTemporaryImage)
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Linha de pensamento", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                Text(captureTitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)

                if (editingId == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Anotações (${notes.size})", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = ::beginCreate) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Nova")
                        }
                    }
                    if (notes.isEmpty()) {
                        Text("Adicione blocos de texto e imagens para construir sua linha de pensamento.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 450.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(notes, key = { it.id }) { note ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth().combinedClickable(
                                        onClick = { },
                                        onLongClick = { beginEdit(note) },
                                    ),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (note.text.isNotBlank()) Text(note.text)
                                        note.imageUri?.let { NoteImagePreview(it) }
                                        Text("Toque e segure para editar", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Text(if (editingId == "new") "Nova anotação" else "Editar anotação", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it.take(MAX_NOTE_LENGTH) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp, max = 280.dp),
                        placeholder = { Text("Registre uma ideia, contexto ou próximo passo…") },
                        minLines = 5,
                        maxLines = 10,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                        supportingText = { Text("${draftText.length}/${MAX_NOTE_LENGTH}") },
                    )
                    draftImageUri?.let { NoteImagePreview(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !isCompressingImage,
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isCompressingImage) "Comprimindo…" else if (draftImageUri == null) "Adicionar imagem" else "Trocar imagem")
                        }
                        if (draftImageUri != null) TextButton(onClick = { draftImageUri = null }) { Text("Remover imagem") }
                    }
                    if (editingId != "new") {
                        TextButton(onClick = {
                            notes = notes.filterNot { it.id == editingId }
                            editingId = null
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Excluir anotação")
                        }
                    }
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { editingId = null }) { Text("Voltar") }
                        Button(onClick = ::commitDraft, enabled = !isCompressingImage) { Text("Aplicar") }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = {
                        generatedImageUris.forEach(onDeleteTemporaryImage)
                        onDismiss()
                    }) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = {
                        val encoded = NotesCodec.encode(notes).orEmpty()
                        val retainedImages = notes.mapNotNull { it.imageUri }.toSet()
                        (generatedImageUris - retainedImages).forEach(onDeleteTemporaryImage)
                        onSave(encoded)
                    }, enabled = editingId == null && !isCompressingImage) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar linha")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteImagePreview(rawUri: String) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(null, rawUri) {
        value = withContext(Dispatchers.IO) { runCatching { loadCoverBitmap(context, rawUri) }.getOrNull() }
    }
    image?.let {
        Image(bitmap = it, contentDescription = "Imagem da anotação", modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp), contentScale = ContentScale.Fit)
    }
}
