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
        val counts = categories
            .filterNot { it.name == "Concluído" }
            .map { category ->
                DashboardCategoryCount(
                    category = category.name,
                    count = captures.count { it.category == category.name },
                )
            }
            .filter { it.count > 0 }
        DashboardSummary(
            completedCount = captures.count { it.category == "Concluído" },
            categoryCounts = counts,
        )
    }
}
