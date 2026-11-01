package com.fehunter.captureflow.features.dashboard.domain.model

data class DashboardCategoryCount(
    val category: String,
    val totalCount: Int,
    val completedCount: Int,
)

data class DashboardSummary(
    val completedCount: Int,
    val categoryCounts: List<DashboardCategoryCount>,
)
