package com.fehunter.captureflow.features.search.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture

class FilterCapturesUseCase {
    operator fun invoke(
        captures: List<Capture>,
        query: String,
        category: String,
        showCompleted: Boolean,
    ): List<Capture> = captures.filter { capture ->
        (query.isBlank() || capture.title.contains(query, true) || capture.content.contains(query, true)) &&
            (category == "Concluído" || showCompleted || !capture.isCompleted) &&
            (category == "Todas" || capture.category == category)
    }
}
