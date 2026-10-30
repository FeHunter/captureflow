package com.fehunter.captureflow.features.imageimport.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.imageimport.domain.gateway.ImageImportGateway

class ImageImportViewModelFactory(
    private val captureUseCases: CaptureUseCases,
    private val gateway: ImageImportGateway,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(ImageImportViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return ImageImportViewModel(captureUseCases, gateway) as T
    }
}
