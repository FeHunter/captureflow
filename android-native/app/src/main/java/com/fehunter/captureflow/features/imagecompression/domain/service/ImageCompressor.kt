package com.fehunter.captureflow.features.imagecompression.domain.service

import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile

/** Compresses provider URIs or encoded raster bytes into app-private JPEG files. */
interface ImageCompressor {
    fun compressToPrivateStorage(sourceUri: String, profile: ImageCompressionProfile): String

    /** Accepts encoded image bytes (for example, a lossless PNG crop from Notes). */
    fun compressEncodedImageToPrivateStorage(
        encodedImage: ByteArray,
        profile: ImageCompressionProfile,
    ): String
}
