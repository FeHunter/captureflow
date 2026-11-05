package com.fehunter.captureflow.features.share.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.share.domain.usecase.CaptureShareUseCases

class CaptureShareViewModelFactory(private val useCases: CaptureShareUseCases) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(CaptureShareViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return CaptureShareViewModel(useCases) as T
    }
}
