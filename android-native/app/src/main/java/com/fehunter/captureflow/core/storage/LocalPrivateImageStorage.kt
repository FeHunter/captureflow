package com.fehunter.captureflow.core.storage

import android.content.Context
import android.net.Uri
import java.io.File

/**
 * Generic app-private file cleanup shared by features.
 * It only deletes files inside the app's own captures directory.
 */
class LocalPrivateImageStorage(context: Context) : PrivateImageStorage {
    private val applicationContext = context.applicationContext

    override fun deletePrivateImage(uri: String?) {
        if (uri.isNullOrBlank()) return
        val parsed = runCatching { Uri.parse(uri) }.getOrNull() ?: return
        if (parsed.scheme != "file") return

        runCatching {
            val capturesDirectory = File(applicationContext.filesDir, "captures").canonicalFile
            val imageFile = File(parsed.path.orEmpty()).canonicalFile
            if (imageFile.toPath().startsWith(capturesDirectory.toPath())) {
                imageFile.delete()
            }
        }
    }
}
