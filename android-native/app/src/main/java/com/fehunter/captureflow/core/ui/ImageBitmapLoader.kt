package com.fehunter.captureflow.core.ui

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File

/** Loads a local/content image with bounded decoding to avoid oversized bitmap allocations. */
fun loadCoverBitmap(context: Context, rawUri: String): ImageBitmap? {
    val uri = Uri.parse(rawUri)
    fun openImage() = if (uri.scheme == "file") {
        uri.path?.let(::File)?.takeIf(File::isFile)?.inputStream()
    } else {
        context.contentResolver.openInputStream(uri)
    }

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    val boundsStream = openImage() ?: return null
    boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (bounds.outWidth / sampleSize > 1200 || bounds.outHeight / sampleSize > 1200) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    return openImage()?.use {
        BitmapFactory.decodeStream(it, null, options)?.asImageBitmap()
    }
}
