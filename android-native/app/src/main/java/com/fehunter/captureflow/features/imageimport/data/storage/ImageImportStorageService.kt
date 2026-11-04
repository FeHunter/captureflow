package com.fehunter.captureflow.features.imageimport.data.storage

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import com.fehunter.captureflow.features.imagecompression.domain.service.ImageCompressor
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile

/**
 * Image Import storage adapter for shared-image copies, preview downloads and
 * best-effort deletion of source screenshots. Generic private-file cleanup lives in Core.
 */
class ImageImportStorageService(
    context: Context,
    private val imageCompressor: ImageCompressor,
) {
    private val applicationContext = context.applicationContext

    /**
     * Copies shared image content into app-private storage so OCR does not depend on a
     * temporary URI grant from the app that shared the image.
     */
    fun copyToPrivateStorage(
        source: Uri,
        profile: ImageCompressionProfile = ImageCompressionProfile.MEDIUM,
    ): Uri = Uri.parse(imageCompressor.compressToPrivateStorage(source.toString(), profile))

    /** Compresses encoded image bytes (for example, a lossless in-memory Notes crop). */
    fun compressEncodedImageToPrivateStorage(
        encodedImage: ByteArray,
        profile: ImageCompressionProfile = ImageCompressionProfile.HIGH,
    ): Uri = Uri.parse(imageCompressor.compressEncodedImageToPrivateStorage(encodedImage, profile))

    /** Downloads a webpage preview image into app-private storage. */
    fun downloadToPrivateStorage(rawUrl: String): Uri? {
        val parsed = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return null
        if (parsed.scheme !in setOf("http", "https") || parsed.host.isNullOrBlank()) return null
        val connection = runCatching { URL(rawUrl).openConnection() as? HttpURLConnection }.getOrNull() ?: return null
        var partialFile: File? = null
        return try {
            connection.connectTimeout = 4_000
            connection.readTimeout = 5_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "CaptureFlow/0.4 (Android)")
            connection.setRequestProperty("Accept", "image/*")
            connection.connect()
            val contentType = connection.contentType.orEmpty().substringBefore(";").lowercase()
            if (connection.responseCode !in 200..299 || !contentType.startsWith("image/")) return null
            val extension = when (contentType) {
                "image/png" -> "png"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                "image/jpeg", "image/jpg" -> "jpg"
                else -> return null
            }
            val directory = File(applicationContext.filesDir, "captures").apply { mkdirs() }
            val destination = File(directory, "link-preview-${UUID.randomUUID()}.$extension")
            partialFile = destination
            connection.inputStream.use { input ->
                destination.outputStream().use { output ->
                    val buffer = ByteArray(8_192)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > 10L * 1024 * 1024) throw IOException("A imagem de prévia excede o limite permitido.")
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(destination.absolutePath, bounds)
            if (destination.length() == 0L || bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                destination.delete()
                partialFile = null
                null
            } else {
                partialFile = null
                Uri.fromFile(destination)
            }
        } catch (_: Exception) {
            partialFile?.delete()
            null
        } finally {
            connection.disconnect()
        }
    }

    /** Deletes only app-owned temporary images; never deletes gallery/content URIs. */
    fun deleteTemporaryImage(source: Uri): Boolean {
        if (source.scheme != "file") return false
        return runCatching {
            val file = File(source.path ?: return false).canonicalFile
            val privateRoots = listOf(
                applicationContext.cacheDir.canonicalFile.path + File.separator,
                File(applicationContext.filesDir, "captures").canonicalFile.path + File.separator,
            )
            if (privateRoots.none { root -> file.path.startsWith(root) }) return false
            file.delete()
        }.getOrDefault(false)
    }

    /**
     * Best-effort removal of an imported screenshot from shared MediaStore storage.
     * Android may require explicit user consent to delete media owned by another app;
     * when blocked, the operation fails safely and the original remains in the gallery.
     */
    fun deleteSourceScreenshot(source: Uri): Boolean {
        if (source.scheme != "content") return false
        return runCatching { applicationContext.contentResolver.delete(source, null, null) > 0 }.getOrDefault(false)
    }

}
