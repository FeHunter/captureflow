package com.fehunter.captureflow.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.fehunter.captureflow.features.dashboard.domain.usecase.ObserveDashboardSummaryUseCase

class DashboardViewModelFactory(
    private val observeSummary: ObserveDashboardSummaryUseCase,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (!modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        @Suppress("UNCHECKED_CAST")
        return DashboardViewModel(observeSummary) as T
    }
}
