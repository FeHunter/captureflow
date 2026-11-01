package com.fehunter.captureflow.features.imageimport.domain.gateway

import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile

data class ImportedLinkMetadata(
    val title: String,
    val description: String?,
    val imageUrl: String?,
)

data class ImportedImageAnalysis(
    val fullText: String,
    val title: String,
    val description: String,
)

/** Platform-independent import boundary. Android URI and Context details stay in Data. */
interface ImageImportGateway {
    suspend fun fetchLinkMetadata(url: String): ImportedLinkMetadata?
    suspend fun analyzeImage(uri: String): ImportedImageAnalysis
    fun copyImageToPrivateStorage(uri: String, profile: ImageCompressionProfile): String
    fun compressEncodedImageToPrivateStorage(encodedImage: ByteArray, profile: ImageCompressionProfile): String
    fun downloadPreviewToPrivateStorage(url: String): String?
    fun deleteTemporaryImage(uri: String): Boolean
    fun deleteSourceScreenshot(uri: String): Boolean
}
