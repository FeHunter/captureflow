package com.fehunter.captureflow.features.search.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilterCapturesUseCaseTest {
    private val captures = listOf(
        Capture("1", "Kotlin guide", "Android", "text", "Ler depois", null, null, 1L),
        Capture("2", "Compose tips", "UI", "text", "Salvar", null, null, 2L),
        Capture("3", "Done item", "Kotlin", "text", "Salvar", null, null, 3L, true),
    )

    @Test
    fun filtersByTitleAndContentCaseInsensitively() {
        val result = FilterCapturesUseCase()(captures, "kOtLiN", "Todas", true)
        assertEquals(listOf("1", "3"), result.map { it.id })
    }

    @Test
    fun hidesCompletedItemsUnlessShowCompletedIsEnabled() {
        val hidden = FilterCapturesUseCase()(captures, "", "Todas", false)
        assertEquals(listOf("1", "2"), hidden.map { it.id })

        val visible = FilterCapturesUseCase()(captures, "", "Todas", true)
        assertEquals(listOf("1", "2", "3"), visible.map { it.id })
    }

    @Test
    fun completedCaptureRemainsInItsOriginalCategory() {
        val result = FilterCapturesUseCase()(captures, "", "Salvar", true)
        assertEquals(listOf("2", "3"), result.map { it.id })
    }

    @Test
    fun categoryFilterDoesNotTreatCompletionAsACategory() {
        val result = FilterCapturesUseCase()(captures, "", "Concluído", true)
        assertTrue(result.isEmpty())
    }
}
