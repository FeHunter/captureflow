package com.fehunter.captureflow.features.imageimport.presentation.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import androidx.exifinterface.media.ExifInterface
import kotlin.math.roundToInt

/**
 * Lets the user choose a crop ratio before the image enters OCR/review.
 * The preview and exported bitmap use the same centered crop.
 */
@Composable
fun ImageCropReviewScreen(
    imageUri: String,
    onConfirm: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bitmap by remember(imageUri) { mutableStateOf<Bitmap?>(null) }
    var selectedRatio by remember(imageUri) { mutableStateOf(0f) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(imageUri) {
        bitmap?.let { if (!it.isRecycled) it.recycle() }
        bitmap = withContext(Dispatchers.IO) {
            runCatching { decodeOrientedBitmap(context, imageUri) }.getOrNull()
        }
        if (bitmap == null) error = "Não foi possível abrir a foto."
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                shape = RoundedCornerShape(14.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("CAPTURA DO CADERNO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.72f))
                    Text("Ajustar foto", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(
                "Escolha o enquadramento",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val currentBitmap = bitmap
            if (currentBitmap == null) {
                CircularProgressIndicator()
            } else {
                Image(
                    bitmap = currentBitmap.asImageBitmap(),
                    contentDescription = "Prévia do recorte da foto",
                    contentScale = if (selectedRatio == 0f) ContentScale.Fit else ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .then(if (selectedRatio > 0f) Modifier.aspectRatio(selectedRatio) else Modifier)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf("Original" to 0f, "1:1" to 1f, "4:5" to 0.8f, "16:9" to (16f / 9f))
                        .forEach { (label, ratio) ->
                            FilterChip(
                                selected = selectedRatio == ratio,
                                onClick = { selectedRatio = ratio },
                                modifier = Modifier.weight(1f),
                                label = { Text(label, maxLines = 1) },
                            )
                        }
                }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f),
                ) { Text("Cancelar") }
                Button(
                    onClick = {
                        val source = bitmap ?: return@Button
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            val outputUri = withContext(Dispatchers.IO) {
                                runCatching {
                                    val cropped = cropCenter(source, selectedRatio)
                                    val output = File(context.cacheDir, "captureflow-cropped-${System.currentTimeMillis()}.jpg")
                                    FileOutputStream(output).use { cropped.compress(Bitmap.CompressFormat.JPEG, 94, it) }
                                    if (cropped !== source) cropped.recycle()
                                    Uri.fromFile(output).toString()
                                }.getOrNull()
                            }
                            isSaving = false
                            if (outputUri != null) onConfirm(outputUri)
                            else error = "Não foi possível salvar o recorte. Tente novamente."
                        }
                    },
                    enabled = bitmap != null && !isSaving,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSaving) CircularProgressIndicator()
                    else Text("Usar foto")
                }
            }
        }
    }
}

private fun decodeOrientedBitmap(context: android.content.Context, rawUri: String): Bitmap {
    val uri = Uri.parse(rawUri)
    val stagedFile = File(context.cacheDir, "captureflow-crop-source-${UUID.randomUUID()}.image")
    try {
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Não foi possível acessar a imagem selecionada.")
        input.use { source -> FileOutputStream(stagedFile).use { target -> source.copyTo(target) } }
        if (!stagedFile.isFile || stagedFile.length() <= 0L) {
            throw IOException("A imagem selecionada está vazia ou indisponível.")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(stagedFile.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("O arquivo selecionado não contém uma imagem compatível.")
        }
        var sample = 1
        while (bounds.outWidth / sample > 2800 || bounds.outHeight / sample > 2800) sample *= 2
        val decoded = BitmapFactory.decodeFile(
            stagedFile.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: throw IOException("Não foi possível decodificar a imagem selecionada.")
        val orientation = runCatching {
            ExifInterface(stagedFile.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
            }
        }
        if (matrix.isIdentity) return decoded
        return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
            if (it !== decoded && !decoded.isRecycled) decoded.recycle()
        }
    } finally {
        stagedFile.delete()
    }
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
