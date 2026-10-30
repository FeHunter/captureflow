package com.fehunter.captureflow.features.imageimport.data

import android.content.Context
import android.net.Uri
import com.fehunter.captureflow.features.imageimport.data.storage.ImageImportStorageService
import com.fehunter.captureflow.features.imageimport.data.metadata.LinkMetadataService
import com.fehunter.captureflow.features.imageimport.data.ocr.OcrService
import com.fehunter.captureflow.features.imageimport.domain.gateway.ImageImportGateway
import com.fehunter.captureflow.features.imageimport.domain.gateway.ImportedImageAnalysis
import com.fehunter.captureflow.features.imageimport.domain.gateway.ImportedLinkMetadata

class ImageImportGatewayImpl(
    context: Context,
    private val metadataService: LinkMetadataService,
    private val ocrService: OcrService,
    private val storageService: ImageImportStorageService,
) : ImageImportGateway {
    private val applicationContext = context.applicationContext

    override suspend fun fetchLinkMetadata(url: String): ImportedLinkMetadata? =
        metadataService.fetch(url)?.let {
            ImportedLinkMetadata(it.title, it.description, it.imageUrl)
        }

    override suspend fun analyzeImage(uri: String): ImportedImageAnalysis {
        val result = ocrService.extractStructuredText(applicationContext, Uri.parse(uri))
        return ImportedImageAnalysis(result.fullText, result.title, result.description)
    }

    override fun copyImageToPrivateStorage(uri: String): String =
        storageService.copyToPrivateStorage(Uri.parse(uri)).toString()

    override fun downloadPreviewToPrivateStorage(url: String): String? =
        storageService.downloadToPrivateStorage(url)?.toString()

    override fun deleteSourceScreenshot(uri: String): Boolean =
        storageService.deleteSourceScreenshot(Uri.parse(uri))
}
