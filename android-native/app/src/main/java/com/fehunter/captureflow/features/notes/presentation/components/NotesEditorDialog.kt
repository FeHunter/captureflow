package com.fehunter.captureflow.features.notes.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.BitmapFactory.Options
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.exifinterface.media.ExifInterface
import com.fehunter.captureflow.features.captures.domain.model.CaptureNote
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

private const val MAX_NOTE_LENGTH = 10_000

@Composable
fun NotesEditorDialog(
    captureTitle: String,
    initialNotes: String,
    initialEditingNoteId: String? = null,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDeleteTemporaryImage: (String) -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val existingNotes = remember(captureTitle, initialNotes) {
        CaptureNotesCodec.decode(initialNotes)
    }
    var editingId by remember(captureTitle, initialEditingNoteId) {
        mutableStateOf(initialEditingNoteId ?: "new")
    }
    val editingNote = existingNotes.firstOrNull { it.id == editingId }
    var draftText by remember(captureTitle, initialEditingNoteId, initialNotes) {
        mutableStateOf(editingNote?.text.orEmpty())
    }
    var draftImageUri by remember(captureTitle, initialEditingNoteId, initialNotes) {
        mutableStateOf(editingNote?.imageUri)
    }
    var isSavingImage by remember { mutableStateOf(false) }
    var generatedImageUris by remember { mutableStateOf(emptySet<String>()) }
    var cropSourceUri by remember { mutableStateOf<String?>(null) }
    var cropBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var selectedRatio by remember { mutableStateOf(0f) }
    var cropError by remember { mutableStateOf<String?>(null) }
    var imageError by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let {
            selectedRatio = 0f
            cropError = null
            cropSourceUri = it.toString()
        }
    }

    LaunchedEffect(cropSourceUri) {
        val sourceUri = cropSourceUri ?: return@LaunchedEffect
        // Clear the previous preview before loading another URI. Otherwise a slow
        // provider could leave the previous bitmap actionable while the new image loads.
        cropBitmap?.let { previous -> if (!previous.isRecycled) previous.recycle() }
        cropBitmap = null
        cropError = null
        imageError = null

        // Photo Picker URIs are temporary ContentProvider grants. Snapshot the selected
        // bytes into app cache while the grant is active, then decode only the app-owned
        // file. This also makes the crop step independent of provider-specific URI behavior.
        val decoded = withContext(Dispatchers.IO) {
            val stagedFile = File(context.cacheDir, "captureflow-note-source-${UUID.randomUUID()}.image")
            try {
                openImageInputStream(context, Uri.parse(sourceUri)).use { input ->
                    FileOutputStream(stagedFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                }
                if (!stagedFile.isFile || stagedFile.length() <= 0L) {
                    throw IOException("O seletor retornou um arquivo vazio ou indisponível.")
                }
                Result.success(decodeOrientedBitmap(context, Uri.fromFile(stagedFile).toString()))
            } catch (error: Exception) {
                Result.failure<Bitmap>(error)
            } finally {
                stagedFile.delete()
            }
        }
        decoded.onSuccess { cropBitmap = it }
            .onFailure { error ->
                cropError = error.localizedMessage?.takeIf(String::isNotBlank)
                    ?: "Não foi possível abrir esta imagem."
            }
    }

    fun closeAndDiscard() {
        generatedImageUris.forEach(onDeleteTemporaryImage)
        cropBitmap?.recycle()
        onDismiss()
    }

    fun saveNote() {
        val text = draftText.trim().take(MAX_NOTE_LENGTH)
        if (text.isBlank() && draftImageUri.isNullOrBlank()) return
        val updated = if (editingNote == null) {
            existingNotes + CaptureNote(
                id = UUID.randomUUID().toString(),
                text = text,
                imageUri = draftImageUri,
            )
        } else {
            existingNotes.map { note ->
                if (note.id == editingNote.id) note.copy(text = text, imageUri = draftImageUri) else note
            }
        }
        val retainedImages = updated.mapNotNull { it.imageUri }.toSet()
        (generatedImageUris - retainedImages).forEach(onDeleteTemporaryImage)
        onSave(CaptureNotesCodec.encode(updated).orEmpty())
    }

    fun deleteNote() {
        val updated = existingNotes.filterNot { it.id == editingId }
        generatedImageUris.forEach(onDeleteTemporaryImage)
        onSave(CaptureNotesCodec.encode(updated).orEmpty())
    }

    fun applyCropAndSaveImage() {
        val source = cropBitmap ?: return
        if (isSavingImage) return
        isSavingImage = true
        imageError = null

        // Diagnostic mode: save the cropped bitmap as PNG directly in app-private storage.
        // ImageCompressionService is intentionally bypassed for this test.
        coroutineScope.launch {
            val savedUri = withContext(Dispatchers.IO) {
                val directory = File(context.filesDir, "captures")
                val destination = File(directory, "capture-note-${UUID.randomUUID()}.png")
                try {
                    if (!directory.exists() && !directory.mkdirs()) {
                        throw IOException("Não foi possível preparar o armazenamento privado.")
                    }
                    val cropped = cropCenter(source, selectedRatio)
                    try {
                        FileOutputStream(destination).use { output ->
                            if (!cropped.compress(Bitmap.CompressFormat.PNG, 100, output)) {
                                throw IOException("Não foi possível gravar a imagem recortada em PNG.")
                            }
                            output.flush()
                        }
                    } finally {
                        if (cropped !== source && !cropped.isRecycled) cropped.recycle()
                    }
                    if (!destination.isFile || destination.length() <= 0L) {
                        throw IOException("A imagem salva ficou vazia.")
                    }
                    Uri.fromFile(destination).toString()
                } catch (error: Exception) {
                    destination.delete()
                    throw error
                }
            }

            savedUri.onSuccess { uri ->
                draftImageUri = uri
                generatedImageUris = generatedImageUris + uri
                imageError = null
                cropBitmap?.let { if (!it.isRecycled) it.recycle() }
                cropBitmap = null
                cropSourceUri = null
            }.onFailure { error ->
                imageError = error.localizedMessage?.takeIf(String::isNotBlank)
                    ?: "Não foi possível salvar a imagem sem compressão."
            }
            isSavingImage = false
        }
    }

    Dialog(
        onDismissRequest = { if (!isCompressingImage) closeAndDiscard() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = !isCompressingImage,
            dismissOnClickOutside = !isCompressingImage,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.94f).heightIn(max = 700.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    if (editingNote == null) "Nova anotação" else "Editar anotação",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    captureTitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                )

                OutlinedTextField(
                    value = draftText,
                    onValueChange = { draftText = it.take(MAX_NOTE_LENGTH) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 130.dp, max = 240.dp),
                    placeholder = { Text("Registre uma ideia, contexto ou próximo passo…") },
                    minLines = 4,
                    maxLines = 9,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText = { Text("${draftText.length}/${MAX_NOTE_LENGTH}") },
                )

                if (cropSourceUri != null) {
                    Text("Ajustar imagem", style = MaterialTheme.typography.titleSmall)
                    cropBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Prévia do recorte da anotação",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 230.dp)
                                .then(if (selectedRatio > 0f) Modifier.aspectRatio(selectedRatio) else Modifier),
                            contentScale = if (selectedRatio == 0f) ContentScale.Fit else ContentScale.Crop,
                        )
                    } ?: if (cropError == null) CircularProgressIndicator() else Text(cropError!!, color = MaterialTheme.colorScheme.error)
                    Text("Formato do recorte", style = MaterialTheme.typography.labelMedium)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Original" to 0f, "1:1" to 1f, "4:5" to 0.8f, "16:9" to (16f / 9f))
                            .forEach { (label, ratio) ->
                                FilterChip(
                                    selected = selectedRatio == ratio,
                                    onClick = { selectedRatio = ratio },
                                    label = { Text(label) },
                                )
                            }
                    }
                    imageError?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                cropBitmap?.recycle()
                                cropBitmap = null
                                cropSourceUri = null
                            },
                            enabled = !isCompressingImage,
                            modifier = Modifier.weight(1f),
                        ) { Text("Voltar") }
                        Button(
                            onClick = ::applyCropAndSaveImage,
                            enabled = cropBitmap != null && !isCompressingImage,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (isCompressingImage) CircularProgressIndicator()
                            else Text("Aplicar recorte")
                        }
                    }
                } else {
                    draftImageUri?.let { NoteImagePreview(it) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = {
                                imagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            enabled = !isCompressingImage,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Image, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (draftImageUri == null) "Selecionar imagem" else "Trocar imagem")
                        }
                        if (draftImageUri != null) {
                            OutlinedButton(
                                onClick = {
                                    selectedRatio = 0f
                                    cropError = null
                                    cropSourceUri = draftImageUri
                                },
                                enabled = !isCompressingImage,
                            ) { Text("Recortar") }
                        }
                    }
                    if (draftImageUri != null) {
                        TextButton(
                            onClick = { draftImageUri = null },
                            enabled = !isCompressingImage,
                        ) { Text("Remover imagem") }
                    }
                    imageError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (editingNote != null && cropSourceUri == null) {
                        TextButton(
                            onClick = ::deleteNote,
                            enabled = !isCompressingImage,
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Excluir")
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(
                        onClick = ::closeAndDiscard,
                        enabled = !isCompressingImage,
                    ) { Text("Cancelar") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = ::saveNote,
                        enabled = !isCompressingImage && cropSourceUri == null &&
                            (draftText.isNotBlank() || !draftImageUri.isNullOrBlank()),
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Salvar anotação")
                    }
                }
            }
        }
    }
}

@Composable
private fun NoteImagePreview(rawUri: String) {
    val context = LocalContext.current
    var bitmap by remember(rawUri) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(rawUri) {
        bitmap = withContext(Dispatchers.IO) {
            runCatching { decodeOrientedBitmap(context, rawUri) }.getOrNull()
        }
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "Imagem da anotação",
            modifier = Modifier.fillMaxWidth().heightIn(max = 180.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

private fun decodeOrientedBitmap(context: android.content.Context, rawUri: String): Bitmap {
    val uri = Uri.parse(rawUri)
    val bounds = Options().apply { inJustDecodeBounds = true }
    openImageInputStream(context, uri).use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("O arquivo selecionado não contém uma imagem raster suportada.")
    }

    var sample = 1
    while (bounds.outWidth / sample > 2560 || bounds.outHeight / sample > 2560) sample *= 2
    val bitmap = openImageInputStream(context, uri).use {
        BitmapFactory.decodeStream(it, null, Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
    } ?: throw IOException("O Android não conseguiu decodificar o conteúdo da imagem.")

    val orientation = runCatching {
        openImageInputStream(context, uri).use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
        }
    }
    if (matrix.isIdentity) return bitmap
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
        bitmap.recycle()
    }
}

/**
 * Photo Picker providers and app-owned cache files expose different URI schemes.
 * Read file:// directly and fall back to a provider file descriptor even when
 * openInputStream throws, not only when it returns null.
 */
private fun openImageInputStream(
    context: android.content.Context,
    uri: Uri,
): InputStream {
    if (uri.scheme.equals("file", ignoreCase = true)) {
        val path = uri.path ?: throw IOException("A URI da imagem não contém um caminho válido.")
        val file = File(path)
        if (!file.isFile || !file.canRead() || file.length() <= 0L) {
            throw IOException("O arquivo da imagem não existe ou não pode ser lido: $path")
        }
        return java.io.FileInputStream(file)
    }

    if (!uri.scheme.equals("content", ignoreCase = true)) {
        throw IOException("Tipo de URI não suportado para imagem: ${uri.scheme ?: "sem esquema"}.")
    }

    val resolver = context.contentResolver
    val streamFailure = runCatching { resolver.openInputStream(uri) }
    streamFailure.getOrNull()?.let { return it }

    val descriptorAttempt = runCatching {
        resolver.openFileDescriptor(uri, "r")?.let {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it)
        }
    }
    descriptorAttempt.getOrNull()?.let { return it }

    val streamMessage = streamFailure.exceptionOrNull()?.localizedMessage
    val descriptorMessage = descriptorAttempt.exceptionOrNull()?.localizedMessage
    throw IOException(
        "O Android não conseguiu ler a imagem do seletor (authority=${uri.authority ?: "desconhecida"}). " +
            listOfNotNull(streamMessage, descriptorMessage).joinToString("; ")
                .ifBlank { "O provedor não disponibilizou um fluxo de leitura." }
    )
}

private fun cropCenter(source: Bitmap, ratio: Float): Bitmap {
    if (ratio <= 0f) return source
    val sourceRatio = source.width.toFloat() / source.height
    val cropWidth: Int
    val cropHeight: Int
    if (sourceRatio > ratio) {
        cropHeight = source.height
        cropWidth = (cropHeight * ratio).roundToInt().coerceIn(1, source.width)
    } else {
        cropWidth = source.width
        cropHeight = (cropWidth / ratio).roundToInt().coerceIn(1, source.height)
    }
    val left = (source.width - cropWidth) / 2
    val top = (source.height - cropHeight) / 2
    return Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)
}
