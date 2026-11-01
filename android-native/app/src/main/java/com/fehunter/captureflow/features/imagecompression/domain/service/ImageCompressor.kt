package com.fehunter.captureflow.features.imagecompression.domain.service

import android.graphics.Bitmap
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile

/**
 * Converts either an Android URI or an already-decoded/cropped bitmap into a JPEG
 * stored in app-private storage. The implementation owns only bitmaps it creates.
 */
interface ImageCompressor {
    fun compressToPrivateStorage(sourceUri: String, profile: ImageCompressionProfile): String

    fun compressBitmapToPrivateStorage(
        bitmap: Bitmap,
        profile: ImageCompressionProfile,
    ): String
}
