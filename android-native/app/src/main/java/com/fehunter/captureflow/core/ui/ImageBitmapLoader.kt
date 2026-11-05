package com.fehunter.captureflow.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

private const val DEFAULT_MAX_DIMENSION = 1200

/** Small process-wide cache for decoded local covers. Room and private files remain the source of truth. */
private val imageBitmapCache: LruCache<String, ImageBitmap> by lazy {
    val maxSizeKb = (Runtime.getRuntime().maxMemory() / 1024L / 8L)
        .toInt()
        .coerceIn(4 * 1024, 24 * 1024)
    object : LruCache<String, ImageBitmap>(maxSizeKb) {
        override fun sizeOf(key: String, value: ImageBitmap): Int {
            val sizeKb = value.width.toLong() * value.height.toLong() * 4L / 1024L
            return sizeKb.coerceIn(1L, Int.MAX_VALUE.toLong()).toInt()
        }
    }
}

/** Decode local/content images with bounded sampling and cache. Must be called from a worker dispatcher. */
fun loadCoverBitmap(context: Context, rawUri: String, maxDimension: Int = DEFAULT_MAX_DIMENSION): ImageBitmap? {
    val targetDimension = maxDimension.coerceIn(128, 2048)
    val uri = Uri.parse(rawUri)
    val file = if (uri.scheme == "file") uri.path?.let(::File)?.takeIf(File::isFile) else null
    val cacheKey = buildString {
        append(rawUri)
        append('#')
        append(targetDimension)
        if (file != null) {
            append('#')
            append(file.length())
            append('#')
            append(file.lastModified())
        }
    }
    imageBitmapCache.get(cacheKey)?.let { return it }

    fun openImage() = if (uri.scheme == "file") {
        file?.inputStream()
    } else {
        context.applicationContext.contentResolver.openInputStream(uri)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = openImage() ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > targetDimension || bounds.outHeight / sampleSize > targetDimension) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sampleSize
        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
    }
    val decoded = openImage()?.use {
        BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
    } ?: return null
    imageBitmapCache.put(cacheKey, decoded)
    return decoded
}
