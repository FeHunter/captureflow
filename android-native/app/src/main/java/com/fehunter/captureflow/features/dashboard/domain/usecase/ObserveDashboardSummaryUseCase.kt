package com.fehunter.captureflow.features.dashboard.domain.usecase

import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.dashboard.domain.model.DashboardCategoryCount
import com.fehunter.captureflow.features.dashboard.domain.model.DashboardSummary
import kotlinx.coroutines.flow.combine

class ObserveDashboardSummaryUseCase(
    private val captureUseCases: CaptureUseCases,
) {
    operator fun invoke() = combine(
        captureUseCases.observeCaptures(),
        captureUseCases.observeCategories(),
    ) { captures, categories ->
        val counts = categories.map { category ->
            val categoryCaptures = captures.filter { it.category == category.name }
            DashboardCategoryCount(
                category = category.name,
                totalCount = categoryCaptures.size,
                completedCount = categoryCaptures.count { it.isCompleted },
            )
        }.filter { it.totalCount > 0 }

        DashboardSummary(
            completedCount = captures.count { it.isCompleted },
            categoryCounts = counts,
        )
    }
}
