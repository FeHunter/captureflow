package com.fehunter.captureflow.features.imagecompression.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.exifinterface.media.ExifInterface
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.imagecompression.domain.service.ImageCompressor
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Single local image pipeline for provider URIs, app files and cropped bitmaps.
 *
 * URI input is decoded with bounds checking and EXIF normalization. Encoded byte input
 * (Notes' lossless PNG crop) is decoded without a URI/provider hop. Both paths share
 * the same dimension limit, alpha handling and JPEG encoder.
 */
class ImageCompressionService(context: Context) : ImageCompressor {
    private val appContext = context.applicationContext

    override fun compressToPrivateStorage(
        sourceUri: String,
        profile: ImageCompressionProfile,
    ): String {
        val source = Uri.parse(sourceUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openSourceStream(source).use { BitmapFactory.decodeStream(it, null, bounds) }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException(
                "A origem não contém uma imagem raster válida " +
                    "(scheme=${source.scheme ?: "desconhecido"}, mime=${bounds.outMimeType ?: "desconhecido"})."
            )
        }

        val sampleSize = calculateSampleSize(
            width = bounds.outWidth,
            height = bounds.outHeight,
            maxDimension = MAX_DIMENSION,
        )
        val decoded = openSourceStream(source).use { input ->
            BitmapFactory.decodeStream(
                input,
                null,
                BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            )
        } ?: throw IOException(
            "O Android reconheceu as dimensões, mas não conseguiu decodificar a imagem " +
                "(scheme=${source.scheme ?: "desconhecido"}, mime=${bounds.outMimeType ?: "desconhecido"})."
        )

        var oriented: Bitmap? = null
        try {
            val orientedBitmap = applyExifOrientation(source, decoded)
            oriented = orientedBitmap
            return encodeJpegToPrivateStorage(orientedBitmap, profile)
        } finally {
            if (oriented != null && oriented !== decoded && !oriented.isRecycled) oriented.recycle()
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    override fun compressEncodedImageToPrivateStorage(
        encodedImage: ByteArray,
        profile: ImageCompressionProfile,
    ): String {
        if (encodedImage.isEmpty()) {
            throw IOException("O recorte recebido está vazio.")
        }
        if (encodedImage.size > MAX_ENCODED_INPUT_BYTES) {
            throw IOException("O recorte excede o limite de tamanho permitido para processamento.")
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(encodedImage, 0, encodedImage.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw IOException(
                "Os bytes recebidos não representam uma imagem raster válida " +
                    "(mime=${bounds.outMimeType ?: "desconhecido"})."
            )
        }

        val sampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        val decoded = BitmapFactory.decodeByteArray(
            encodedImage,
            0,
            encodedImage.size,
            BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: throw IOException("O Android não conseguiu decodificar o recorte recebido.")

        return try {
            // Notes decodes and applies EXIF before cropping; its encoded PNG has no pending EXIF transform.
            encodeJpegToPrivateStorage(decoded, profile)
        } finally {
            if (!decoded.isRecycled) decoded.recycle()
        }
    }

    /**
     * Opens app-owned file URIs directly. Provider URIs use ContentResolver first and
     * a ParcelFileDescriptor fallback, including providers that throw from openInputStream.
     */
    private fun openSourceStream(source: Uri): InputStream {
        when (source.scheme?.lowercase()) {
            "file" -> {
                val path = source.path
                    ?: throw IOException("A URI file:// não contém um caminho válido.")
                val file = File(path)
                if (!file.isFile || !file.canRead() || file.length() <= 0L) {
                    throw IOException("O arquivo da imagem não existe, está vazio ou não pode ser lido.")
                }
                return FileInputStream(file)
            }
            "content" -> {
                val resolver = appContext.contentResolver
                val streamAttempt = runCatching { resolver.openInputStream(source) }
                streamAttempt.getOrNull()?.let { return it }

                val descriptorAttempt = runCatching {
                    resolver.openFileDescriptor(source, "r")?.let { descriptor ->
                        ParcelFileDescriptor.AutoCloseInputStream(descriptor)
                    }
                }
                descriptorAttempt.getOrNull()?.let { return it }

                val details = listOfNotNull(
                    streamAttempt.exceptionOrNull()?.localizedMessage,
                    descriptorAttempt.exceptionOrNull()?.localizedMessage,
                ).joinToString("; ").ifBlank { "o provedor não disponibilizou um fluxo de leitura" }
                throw IOException(
                    "Não foi possível ler a imagem do provedor " +
                        "(authority=${source.authority ?: "desconhecida"}): $details"
                )
            }
            else -> throw IOException(
                "Tipo de URI não suportado para compressão: ${source.scheme ?: "sem esquema"}. " +
                    "São aceitos file:// e content://."
            )
        }
    }

    private fun applyExifOrientation(source: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            openSourceStream(source).use { stream ->
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
                ExifInterface.ORIENTATION_TRANSPOSE -> {
                    postRotate(90f)
                    postScale(-1f, 1f)
                }
                ExifInterface.ORIENTATION_TRANSVERSE -> {
                    postRotate(270f)
                    postScale(-1f, 1f)
                }
            }
        }
        return if (matrix.isIdentity) bitmap else Bitmap.createBitmap(
            bitmap,
            0,
            0,
            bitmap.width,
            bitmap.height,
            matrix,
            true,
        )
    }

    /**
     * Shared encoder for URI-decoded images and Notes' decoded PNG crop.
     * Transparent pixels are composited over white because JPEG has no alpha channel.
     * The caller's bitmap is never recycled or mutated.
     */
    private fun encodeJpegToPrivateStorage(
        source: Bitmap,
        profile: ImageCompressionProfile,
    ): String {
        var working = source
        var ownsWorking = false
        try {
            val largestDimension = max(source.width, source.height)
            if (largestDimension > MAX_DIMENSION) {
                val scale = MAX_DIMENSION.toFloat() / largestDimension.toFloat()
                val targetWidth = (source.width * scale).roundToInt().coerceAtLeast(1)
                val targetHeight = (source.height * scale).roundToInt().coerceAtLeast(1)
                working = Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
                ownsWorking = working !== source
            }

            if (working.config != Bitmap.Config.ARGB_8888 || working.hasAlpha()) {
                val normalized = Bitmap.createBitmap(
                    working.width,
                    working.height,
                    Bitmap.Config.ARGB_8888,
                )
                Canvas(normalized).apply {
                    drawColor(Color.WHITE)
                    drawBitmap(working, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                }
                if (ownsWorking && !working.isRecycled) working.recycle()
                working = normalized
                ownsWorking = true
            }

            val directory = File(appContext.filesDir, CAPTURES_DIRECTORY)
            if (!directory.exists() && !directory.mkdirs()) {
                throw IOException("Não foi possível preparar o armazenamento privado das imagens.")
            }

            val destination = File(directory, "capture-${UUID.randomUUID()}.jpg")
            try {
                FileOutputStream(destination).use { output ->
                    if (!working.compress(Bitmap.CompressFormat.JPEG, profile.quality, output)) {
                        throw IOException("O Android não conseguiu codificar a imagem em JPEG.")
                    }
                    output.flush()
                }
                if (!destination.isFile || destination.length() <= 0L) {
                    throw IOException("O arquivo JPEG gerado está vazio ou inválido.")
                }
                return Uri.fromFile(destination).toString()
            } catch (error: Exception) {
                destination.delete()
                throw error
            }
        } finally {
            if (ownsWorking && !working.isRecycled) working.recycle()
        }
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sample = 1
        while (width / sample > maxDimension || height / sample > maxDimension) {
            sample *= 2
        }
        return sample
    }

    private companion object {
        const val MAX_DIMENSION = 2560
        const val MAX_ENCODED_INPUT_BYTES = 48 * 1024 * 1024
        const val CAPTURES_DIRECTORY = "captures"
    }
}
