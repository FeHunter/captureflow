package com.fehunter.captureflow.features.imagecompression.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.imagecompression.domain.service.ImageCompressor
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

/** Decodes, normalizes orientation, bounds dimensions and recompresses imported images locally. */
class ImageCompressionService(context: Context) : ImageCompressor {
    private val appContext = context.applicationContext

    override fun compressToPrivateStorage(sourceUri: String, profile: ImageCompressionProfile): String {
        val source = Uri.parse(sourceUri)
        val resolver = appContext.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Não foi possível abrir a imagem selecionada.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("O arquivo não contém uma imagem válida.")
        }

        val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decoded = resolver.openInputStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: throw IOException("Não foi possível decodificar a imagem selecionada.")

        var oriented: Bitmap? = null
        var normalized: Bitmap? = null
        val directory = File(appContext.filesDir, "captures")
        if (!directory.exists() && !directory.mkdirs()) {
            decoded.recycle()
            throw IOException("Não foi possível preparar o armazenamento da imagem.")
        }
        val destination = File(directory, "capture-${UUID.randomUUID()}.jpg")
        try {
            oriented = applyExifOrientation(source, decoded)
            normalized = if (oriented.config != Bitmap.Config.ARGB_8888) {
                oriented.copy(Bitmap.Config.ARGB_8888, false)
                    ?: throw IOException("Não foi possível preparar a imagem para compressão.")
            } else oriented

            FileOutputStream(destination).use { output ->
                if (!normalized.compress(Bitmap.CompressFormat.JPEG, profile.quality, output)) {
                    throw IOException("O Android não conseguiu codificar a imagem em JPEG.")
                }
                output.flush()
            }
            if (destination.length() == 0L) {
                throw IOException("A imagem comprimida ficou vazia.")
            }
            return Uri.fromFile(destination).toString()
        } catch (error: Exception) {
            destination.delete()
            throw error
        } finally {
            if (normalized != null && normalized !== oriented) normalized.recycle()
            if (oriented != null && oriented !== decoded) oriented.recycle()
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    private fun applyExifOrientation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            appContext.contentResolver.openInputStream(source)?.use { ExifInterface(it).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            ) } ?: ExifInterface.ORIENTATION_NORMAL
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
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(
            bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true,
        )
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (width / sample > maxDimension || height / sample > maxDimension) sample *= 2
        return sample
    }

    private companion object {
        const val MAX_DIMENSION = 2560
    }
}
