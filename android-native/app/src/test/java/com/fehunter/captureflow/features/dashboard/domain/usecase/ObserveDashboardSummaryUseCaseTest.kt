package com.fehunter.captureflow.features.dashboard.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveDashboardSummaryUseCaseTest {
    @Test
    fun calculatesCompletedAndTotalCountsWithinEachOriginalCategory() = runBlocking {
        val repository = FakeCaptureRepository()
        val summary = ObserveDashboardSummaryUseCase(CaptureUseCases(repository))().first()

        assertEquals(2, summary.completedCount)
        assertEquals(
            listOf(
                Triple("Ler depois", 2, 1),
                Triple("Salvar", 2, 1),
            ),
            summary.categoryCounts.map { Triple(it.category, it.totalCount, it.completedCount) },
        )
    }

    private class FakeCaptureRepository : CaptureRepository {
        private val captures = MutableStateFlow(
            listOf(
                capture("1", "Ler depois", completed = true),
                capture("2", "Ler depois", completed = false),
                capture("3", "Salvar", completed = true),
                capture("4", "Salvar", completed = false),
            ),
        )
        private val categories = MutableStateFlow(
            listOf(
                CaptureCategory("Ler depois", 0),
                CaptureCategory("Salvar", 1),
            ),
        )

        override fun observeCaptures(): Flow<List<Capture>> = captures
        override suspend fun findById(id: String): Capture? = captures.value.firstOrNull { it.id == id }
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
        override suspend fun addCategory(name: String, sortOrder: Int) = Unit
        override suspend fun renameCategory(oldName: String, newName: String) = Unit
        override suspend fun deleteCategory(name: String, fallbackCategory: String) = Unit
        override suspend fun addSubtask(subtask: CaptureSubtask) = Unit
        override suspend fun setSubtaskCompleted(id: String, completed: Boolean) = Unit
        override suspend fun deleteSubtask(id: String) = Unit

        private fun capture(id: String, category: String, completed: Boolean) = Capture(
            id = id,
            title = "Task $id",
            content = "",
            kind = "text",
            category = category,
            sourceUrl = null,
            imageUri = null,
            createdAt = id.toLong(),
            isCompleted = completed,
        )
    }
}
