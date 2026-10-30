package com.fehunter.captureflow.features.captures.data.repository

import androidx.room.withTransaction
import com.fehunter.captureflow.features.captures.data.local.CaptureDatabase
import com.fehunter.captureflow.features.captures.data.mapper.toDomain
import com.fehunter.captureflow.features.captures.data.mapper.toEntity
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomCaptureRepository(
    private val database: CaptureDatabase,
) : CaptureRepository {
    private val captures get() = database.captureDao()
    private val categories get() = database.categoryDao()
    private val subtasks get() = database.captureSubtaskDao()
    private val legacyTasks get() = database.checklistDao()

    override fun observeCaptures(): Flow<List<Capture>> =
        captures.observeAll().map { items -> items.map { it.toDomain() } }

    override suspend fun findById(id: String): Capture? = captures.findById(id)?.toDomain()

    override fun observeCategories(): Flow<List<CaptureCategory>> =
        categories.observeAll().map { items -> items.map { it.toDomain() } }

    override fun observeSubtasks(): Flow<List<CaptureSubtask>> =
        subtasks.observeAll().map { items -> items.map { it.toDomain() } }

    override suspend fun save(capture: Capture) = captures.insert(capture.toEntity())
    override suspend fun findByContent(content: String) = captures.containsContent(content)
    override suspend fun findBySourceUrl(url: String) = captures.containsSourceUrl(url)
    override suspend fun updateImage(id: String, imageUri: String?) = captures.updateImageUri(id, imageUri)
    override suspend fun updateExtractedContent(id: String, content: String, title: String) =
        captures.updateExtractedText(id, content, title)
    override suspend fun updateTitle(id: String, title: String) = captures.updateTitle(id, title)
    override suspend fun updateNotes(id: String, notes: String?) = captures.updateNotes(id, notes)
    override suspend fun updateCategory(id: String, category: String) = captures.updateCategory(id, category)

    override suspend fun setCompleted(id: String, completed: Boolean) {
        if (completed) captures.markCompleted(id) else captures.markPending(id)
    }

    override suspend fun delete(id: String) {
        database.withTransaction {
            subtasks.deleteForCapture(id)
            captures.delete(id)
        }
    }

    override suspend fun addCategory(name: String, sortOrder: Int) {
        categories.insert(com.fehunter.captureflow.features.captures.data.local.CaptureCategoryEntity(name, sortOrder))
    }

    override suspend fun renameCategory(oldName: String, newName: String) {
        database.withTransaction {
            captures.renameCategoryReferences(oldName, newName)
            captures.renamePreviousCategoryReferences(oldName, newName)
            legacyTasks.renameCategoryReferences(oldName, newName)
            categories.rename(oldName, newName)
        }
    }

    override suspend fun deleteCategory(name: String, fallbackCategory: String) {
        database.withTransaction {
            captures.renameCategoryReferences(name, fallbackCategory)
            captures.renamePreviousCategoryReferences(name, fallbackCategory)
            legacyTasks.renameCategoryReferences(name, fallbackCategory)
            categories.delete(name)
        }
    }

    override suspend fun addSubtask(subtask: CaptureSubtask) {
        database.withTransaction {
            if (captures.findById(subtask.captureId) != null) {
                subtasks.insert(subtask.toEntity())
            }
        }
    }
    override suspend fun setSubtaskCompleted(id: String, completed: Boolean) = subtasks.setCompleted(id, completed)
    override suspend fun deleteSubtask(id: String) = subtasks.delete(id)
}
