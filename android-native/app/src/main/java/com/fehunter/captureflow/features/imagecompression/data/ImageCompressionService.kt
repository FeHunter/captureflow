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
import java.io.InputStream
import java.io.FileInputStream
import android.os.ParcelFileDescriptor
import java.util.UUID

/** Decodes, normalizes orientation, bounds dimensions and recompresses imported images locally. */
class ImageCompressionService(context: Context) : ImageCompressor {
    private val appContext = context.applicationContext

    override fun compressToPrivateStorage(sourceUri: String, profile: ImageCompressionProfile): String {
        val source = Uri.parse(sourceUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openSourceStream(source)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            ?: throw IOException("Não foi possível abrir a imagem selecionada.")
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException("O arquivo não contém uma imagem válida.")
        }

        val sample = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decoded = openSourceStream(source)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        } ?: throw IOException("O conteúdo da URI não possui um formato de imagem decodificável.")

        var oriented: Bitmap? = null
        var normalized: Bitmap? = null
        val directory = File(appContext.filesDir, "captures")
        if (!directory.exists() && !directory.mkdirs()) {
            decoded.recycle()
            throw IOException("Não foi possível preparar o armazenamento da imagem.")
        }
        val destination = File(directory, "capture-${UUID.randomUUID()}.jpg")
        try {
            val orientedBitmap = applyExifOrientation(source, decoded)
            oriented = orientedBitmap
            val normalizedBitmap = if (orientedBitmap.config != Bitmap.Config.ARGB_8888) {
                orientedBitmap.copy(Bitmap.Config.ARGB_8888, false)
                    ?: throw IOException("Não foi possível preparar a imagem para compressão.")
            } else orientedBitmap
            normalized = normalizedBitmap

            FileOutputStream(destination).use { output ->
                if (!normalizedBitmap.compress(Bitmap.CompressFormat.JPEG, profile.quality, output)) {
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

    /**
     * File URIs are app-owned temporary files (for example, the lossless crop PNG).
     * Read them directly instead of routing them through a ContentProvider. For provider
     * URIs, keep the normal stream API and fall back to a file descriptor if a provider
     * unexpectedly returns null.
     */
    private fun openSourceStream(source: Uri): InputStream? {
        if (source.scheme == "file") {
            val path = source.path ?: throw IOException("A URI do arquivo não contém um caminho válido.")
            val file = File(path)
            if (!file.isFile || !file.canRead() || file.length() == 0L) {
                throw IOException("O arquivo temporário da imagem não existe ou não pode ser lido.")
            }
            return FileInputStream(file)
        }
        val resolver = appContext.contentResolver
        return resolver.openInputStream(source) ?: resolver.openFileDescriptor(source, "r")
            ?.let { descriptor -> ParcelFileDescriptor.AutoCloseInputStream(descriptor) }
    }

    private fun applyExifOrientation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            openSourceStream(source)?.use { ExifInterface(it).getAttributeInt(
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
