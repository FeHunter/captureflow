package com.fehunter.captureflow.features.search.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterCapturesUseCaseTest {
    private val captures = listOf(
        Capture("1", "Kotlin guide", "Android", "text", "Ler depois", null, null, 1L),
        Capture("2", "Compose tips", "UI", "text", "Salvar", null, null, 2L),
        Capture("3", "Done item", "Kotlin", "text", "Concluído", null, null, 3L, true, "Salvar"),
    )

    @Test
    fun filtersByTitleAndContentCaseInsensitively() {
        val result = FilterCapturesUseCase()(captures, "kOtLiN", "Todas", true)
        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun hidesCompletedItemsUnlessCompletedCategorySelectedOrEnabled() {
        val hidden = FilterCapturesUseCase()(captures, "", "Todas", false)
        assertEquals(listOf("1", "2"), hidden.map { it.id })
        val completed = FilterCapturesUseCase()(captures, "", "Concluído", false)
        assertEquals(listOf("3"), completed.map { it.id })
    }

    @Test
    fun filtersBySelectedCategory() {
        val result = FilterCapturesUseCase()(captures, "", "Salvar", true)
        assertEquals(listOf("2"), result.map { it.id })
    }
}
