package com.fehunter.captureflow.features.captures.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.core.storage.PrivateImageStorage
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases

class CaptureViewModelFactory(
    private val captureUseCases: CaptureUseCases,
    private val privateImageStorage: PrivateImageStorage,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CaptureViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }

        @Suppress("UNCHECKED_CAST")
        return CaptureViewModel(
            captureUseCases = captureUseCases,
            privateImageStorage = privateImageStorage,
        ) as T
    }
}
