package com.fehunter.captureflow.features.captures.data.repository

import androidx.room.withTransaction
import com.fehunter.captureflow.features.captures.data.local.CaptureDatabase
import com.fehunter.captureflow.features.captures.data.mapper.toDomain
import com.fehunter.captureflow.features.captures.data.mapper.toEntity
import com.fehunter.captureflow.features.captures.data.mapper.toDomain as mediaToDomain
import com.fehunter.captureflow.features.captures.data.mapper.toEntity as mediaToEntity
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.features.captures.domain.model.CaptureImportBatch
import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class RoomCaptureRepository(private val database: CaptureDatabase) : CaptureRepository {
    private val captures get() = database.captureDao()
    private val categories get() = database.categoryDao()
    private val subtasks get() = database.captureSubtaskDao()
    private val media get() = database.captureMediaDao()
    private val legacyTasks get() = database.checklistDao()

    override fun observeCaptures(): Flow<List<Capture>> =
        captures.observeAll().map { items -> items.map { it.toDomain() } }
    override suspend fun findById(id: String): Capture? = captures.findById(id)?.toDomain()
    override fun observeCategories(): Flow<List<CaptureCategory>> =
        categories.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeSubtasks(): Flow<List<CaptureSubtask>> =
        subtasks.observeAll().map { items -> items.map { it.toDomain() } }
    override fun observeMedia(): Flow<List<CaptureMedia>> =
        media.observeAll().map { items -> items.map { it.mediaToDomain() } }

    override suspend fun addMedia(items: List<CaptureMedia>) {
        if (items.isEmpty()) return
        database.withTransaction {
            val captureId = items.first().captureId
            require(items.all { it.captureId == captureId }) { "All media must belong to one capture" }
            require(captures.findById(captureId) != null) { "Capture does not exist" }
            val existing = media.findForCapture(captureId)
            val merged = (existing.map { it.mediaToDomain() } + items)
                .distinctBy { it.uri }
                .take(MAX_MEDIA_PER_CAPTURE)
            val preferredCoverId = items.firstOrNull { it.isCover }?.id
                ?: existing.firstOrNull { it.isCover }?.id
                ?: merged.firstOrNull()?.id
            val normalized = merged.mapIndexed { index, item ->
                item.copy(position = index, isCover = item.id == preferredCoverId)
            }
            media.deleteForCapture(captureId)
            media.insertAll(normalized.map { it.mediaToEntity() })
            normalized.firstOrNull { it.isCover }?.let { captures.updateImageUri(captureId, it.uri) }
        }
    }
    override suspend fun setCover(captureId: String, mediaId: String) {
        database.withTransaction {
            media.clearCover(captureId)
            media.setCover(captureId, mediaId)
            media.findForCapture(captureId).firstOrNull { it.id == mediaId }?.let {
                captures.updateImageUri(captureId, it.uri)
            }
        }
    }

    override suspend fun replaceCoverImage(captureId: String, uri: String) {
        database.withTransaction {
            require(captures.findById(captureId) != null) { "Capture does not exist" }
            val existing = media.findForCapture(captureId)
            val cover = existing.firstOrNull { it.isCover } ?: existing.minByOrNull { it.position }
            if (cover != null) {
                media.clearCover(captureId)
                val extension = uri.substringBefore('?').substringAfterLast('.', "").lowercase()
                val mediaType = when (extension) {
                    "webp" -> "image/webp"
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    else -> "image/*"
                }
                media.updateUriAndType(captureId, cover.id, uri, mediaType)
                media.setCover(captureId, cover.id)
            } else {
                media.insertAll(listOf(
                    com.fehunter.captureflow.features.captures.data.local.CaptureMediaEntity(
                        id = UUID.randomUUID().toString(),
                        captureId = captureId,
                        uri = uri,
                        mediaType = "image/*",
                        position = 0,
                        isCover = true,
                        sourceUrl = null,
                        createdAt = System.currentTimeMillis(),
                    ),
                ))
            }
            captures.updateImageUri(captureId, uri)
        }
    }

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
            media.deleteForCapture(id)
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
            if (captures.findById(subtask.captureId) != null) subtasks.insert(subtask.toEntity())
        }
    }
    override suspend fun setSubtaskCompleted(id: String, completed: Boolean) = subtasks.setCompleted(id, completed)

    override suspend fun importBatch(batch: CaptureImportBatch) {
        database.withTransaction {
            batch.categories.forEach { categories.insert(it.toEntity()) }
            captures.insertImported(batch.captures.map { it.toEntity() })
            val availableCaptureIds = batch.captures.map { it.id }.toSet()
            val safeSubtasks = batch.subtasks.filter { it.captureId in availableCaptureIds }
            val safeMedia = batch.media.filter { it.captureId in availableCaptureIds }
            subtasks.insertImported(safeSubtasks.map { it.toEntity() })
            media.insertImported(safeMedia.map { it.mediaToEntity() })
        }
    }
    override suspend fun deleteSubtask(id: String) = subtasks.delete(id)

    private companion object { const val MAX_MEDIA_PER_CAPTURE = 10 }
}
