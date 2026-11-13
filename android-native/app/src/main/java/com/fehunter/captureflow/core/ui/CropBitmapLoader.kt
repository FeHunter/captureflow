package com.fehunter.captureflow.core.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream

/**
 * Decodes a local image for a crop editor with bounded memory and EXIF orientation.
 * Call from a worker dispatcher. The caller owns and must recycle the returned Bitmap.
 */
fun loadCropBitmap(context: Context, rawUri: String, maxDimension: Int = 2560): Bitmap {
    val uri = Uri.parse(rawUri)
    fun openImage(): InputStream {
        if (uri.scheme.equals("file", ignoreCase = true)) {
            val path = uri.path ?: throw IOException("A URI da imagem não contém um caminho válido.")
            val file = File(path)
            if (!file.isFile || !file.canRead() || file.length() <= 0L) {
                throw IOException("O arquivo da imagem não existe ou não pode ser lido.")
            }
            return FileInputStream(file)
        }
        if (!uri.scheme.equals("content", ignoreCase = true)) {
            throw IOException("Tipo de URI não suportado para imagem.")
        }
        val resolver = context.applicationContext.contentResolver
        val streamAttempt = runCatching { resolver.openInputStream(uri) }
        streamAttempt.getOrNull()?.let { return it }
        val descriptorAttempt = runCatching {
            resolver.openFileDescriptor(uri, "r")?.let { descriptor ->
                android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            }
        }
        descriptorAttempt.getOrNull()?.let { return it }
        throw IOException(
            "O Android não conseguiu ler a imagem selecionada: " +
                listOfNotNull(
                    streamAttempt.exceptionOrNull()?.localizedMessage,
                    descriptorAttempt.exceptionOrNull()?.localizedMessage,
                ).joinToString("; ").ifBlank { "o provedor não disponibilizou um fluxo." },
        )
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openImage().use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw IOException("O arquivo selecionado não contém uma imagem raster compatível.")
    }
    val target = maxDimension.coerceIn(512, 4096)
    var sample = 1
    while (bounds.outWidth / sample > target || bounds.outHeight / sample > target) sample *= 2
    val decoded = openImage().use { input ->
        BitmapFactory.decodeStream(
            input,
            null,
            BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        )
    } ?: throw IOException("Não foi possível decodificar a imagem selecionada.")

    val orientation = runCatching {
        openImage().use { input ->
            ExifInterface(input).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix().apply {
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { postRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { postRotate(270f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> postRotate(270f)
        }
    }
    if (matrix.isIdentity) return decoded
    return Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
        if (it !== decoded && !decoded.isRecycled) decoded.recycle()
    }
}
