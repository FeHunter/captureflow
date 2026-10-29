package com.fehunter.captureflow.services

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * Copies shared image content into app-private storage so OCR does not depend on a
 * temporary URI grant from the app that shared the image.
 */
class SharedImageStorageService {
    fun copyToPrivateStorage(context: Context, source: Uri): Uri {
        val resolver = context.contentResolver
        val input = resolver.openInputStream(source)
            ?: throw IOException("Não foi possível abrir a imagem compartilhada.")

        val extension = when (resolver.getType(source)?.lowercase()) {
            "image/png" -> "png"
            "image/webp" -> "webp"
            "image/gif" -> "gif"
            "image/bmp" -> "bmp"
            else -> "jpg"
        }
        val directory = File(context.filesDir, "captures").apply { mkdirs() }
        val destination = File(directory, "capture-${UUID.randomUUID()}.$extension")

        try {
            input.use { stream ->
                destination.outputStream().use { output -> stream.copyTo(output) }
            }
            if (destination.length() == 0L) {
                destination.delete()
                throw IOException("A imagem compartilhada está vazia.")
            }
            return Uri.fromFile(destination)
        } catch (error: Exception) {
            destination.delete()
            throw error
        }
    }

    /**
     * Best-effort removal of an imported screenshot from shared MediaStore storage.
     * Android may require explicit user consent to delete media owned by another app;
     * when blocked, the operation fails safely and the original remains in the gallery.
     */
    fun deleteSourceScreenshot(context: Context, source: Uri): Boolean {
        if (source.scheme != "content") return false
        return runCatching { context.contentResolver.delete(source, null, null) > 0 }.getOrDefault(false)
    }

    fun deletePrivateImage(context: Context, imageUri: String?) {
        if (imageUri.isNullOrBlank()) return
        val uri = runCatching { Uri.parse(imageUri) }.getOrNull() ?: return
        if (uri.scheme != "file") return

        val capturesDirectory = File(context.filesDir, "captures").canonicalFile
        val imageFile = File(uri.path.orEmpty()).canonicalFile
        if (imageFile.toPath().startsWith(capturesDirectory.toPath())) {
            imageFile.delete()
        }
    }
}
