package com.fehunter.captureflow.app.di

import android.app.Application
import com.fehunter.captureflow.features.captures.data.local.CaptureDatabase
import com.fehunter.captureflow.features.captures.presentation.viewmodel.CaptureViewModelFactory
import com.fehunter.captureflow.features.imageimport.data.metadata.LinkMetadataService
import com.fehunter.captureflow.features.imageimport.data.ocr.OcrService
import com.fehunter.captureflow.core.storage.SharedImageStorageService

/**
 * Manual composition root for the current single-module Android application.
 * Feature implementations are wired here rather than constructed by the UI layer.
 */
class AppContainer(application: Application) {
    private val database = CaptureDatabase.get(application)

    val captureViewModelFactory = CaptureViewModelFactory(
        application = application,
        database = database,
        ocrService = OcrService(),
        linkMetadataService = LinkMetadataService(),
        imageStorageService = SharedImageStorageService(),
    )
}
