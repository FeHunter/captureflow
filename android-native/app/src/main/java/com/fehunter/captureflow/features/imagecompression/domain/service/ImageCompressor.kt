package com.fehunter.captureflow.features.imagecompression.domain.service

import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile

/** Platform-neutral boundary for compressing an incoming image URI into app-private storage. */
interface ImageCompressor {
    fun compressToPrivateStorage(sourceUri: String, profile: ImageCompressionProfile): String
}
