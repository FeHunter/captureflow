package com.fehunter.captureflow.features.captures.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.captures.data.local.CaptureDatabase
import com.fehunter.captureflow.features.imageimport.data.metadata.LinkMetadataService
import com.fehunter.captureflow.features.imageimport.data.ocr.OcrService
import com.fehunter.captureflow.core.storage.SharedImageStorageService

class CaptureViewModelFactory(
    private val application: Application,
    private val database: CaptureDatabase,
    private val ocrService: OcrService,
    private val linkMetadataService: LinkMetadataService,
    private val imageStorageService: SharedImageStorageService,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return CaptureViewModel(
            application = application,
            database = database,
            ocrService = ocrService,
            linkMetadataService = linkMetadataService,
            imageStorageService = imageStorageService,
        ) as T
    }
}
