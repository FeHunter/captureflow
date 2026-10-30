package com.fehunter.captureflow.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.dashboard.domain.model.DashboardSummary
import com.fehunter.captureflow.features.dashboard.domain.usecase.ObserveDashboardSummaryUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class DashboardViewModel(
    observeSummary: ObserveDashboardSummaryUseCase,
) : ViewModel() {
    val summary = observeSummary().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        DashboardSummary(completedCount = 0, categoryCounts = emptyList()),
    )
}
