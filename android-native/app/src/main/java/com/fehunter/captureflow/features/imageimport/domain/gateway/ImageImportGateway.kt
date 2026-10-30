package com.fehunter.captureflow.features.imageimport.domain.gateway

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
    fun copyImageToPrivateStorage(uri: String): String
    fun downloadPreviewToPrivateStorage(url: String): String?
    fun deleteSourceScreenshot(uri: String): Boolean
}
