package com.fehunter.captureflow.features.categories.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManageCategoriesUseCaseTest {
    @Test
    fun addTrimsNameAndRejectsReservedOrDuplicateNames() = runBlocking {
        val repository = FakeCaptureRepository(
            listOf(CaptureCategory("Ler depois", 0), CaptureCategory("Concluído", 999)),
        )
        val useCase = ManageCategoriesUseCase(repository)

        useCase.add("  Favoritos  ")
        useCase.add("favoritos")
        useCase.add("Todas")
        useCase.add("Concluído")

        assertEquals(
            listOf(
                CaptureCategory("Ler depois", 0),
                CaptureCategory("Concluído", 999),
                CaptureCategory("Favoritos", 1),
            ),
            repository.categories.value,
        )
    }

    @Test
    fun deleteReassignsToAnActiveFallbackAndProtectsCompletedCategory() = runBlocking {
        val repository = FakeCaptureRepository(
            listOf(
                CaptureCategory("Ler depois", 0),
                CaptureCategory("Salvar", 1),
                CaptureCategory("Concluído", 999),
            ),
        )
        val useCase = ManageCategoriesUseCase(repository)

        useCase.delete("Salvar")
        useCase.delete("Concluído")

        assertEquals("Salvar", repository.lastDeletedCategory)
        assertEquals("Ler depois", repository.lastFallbackCategory)
        assertFalse(repository.categories.value.any { it.name == "Salvar" })
        assertTrue(repository.categories.value.any { it.name == "Concluído" })
    }

    private class FakeCaptureRepository(
        initialCategories: List<CaptureCategory>,
    ) : CaptureRepository {
        val categories = MutableStateFlow(initialCategories)
        var lastDeletedCategory: String? = null
        var lastFallbackCategory: String? = null

        override fun observeCaptures(): Flow<List<Capture>> = MutableStateFlow(emptyList())
        override suspend fun findById(id: String): Capture? = null
        override fun observeCategories(): Flow<List<CaptureCategory>> = categories
        override fun observeSubtasks(): Flow<List<CaptureSubtask>> = MutableStateFlow(emptyList())
        override suspend fun save(capture: Capture) = Unit
        override suspend fun findByContent(content: String) = false
        override suspend fun findBySourceUrl(url: String) = false
        override suspend fun updateImage(id: String, imageUri: String?) = Unit
        override suspend fun updateExtractedContent(id: String, content: String, title: String) = Unit
        override suspend fun updateTitle(id: String, title: String) = Unit
        override suspend fun updateNotes(id: String, notes: String?) = Unit
        override suspend fun updateCategory(id: String, category: String) = Unit
        override suspend fun setCompleted(id: String, completed: Boolean) = Unit
        override suspend fun delete(id: String) = Unit

        override suspend fun addCategory(name: String, sortOrder: Int) {
            categories.value = categories.value + CaptureCategory(name, sortOrder)
        }

        override suspend fun renameCategory(oldName: String, newName: String) {
            categories.value = categories.value.map {
                if (it.name == oldName) it.copy(name = newName) else it
            }
        }

        override suspend fun deleteCategory(name: String, fallbackCategory: String) {
            lastDeletedCategory = name
            lastFallbackCategory = fallbackCategory
            categories.value = categories.value.filterNot { it.name == name }
        }

        override suspend fun addSubtask(subtask: CaptureSubtask) = Unit
        override suspend fun setSubtaskCompleted(id: String, completed: Boolean) = Unit
        override suspend fun deleteSubtask(id: String) = Unit
    }
}
