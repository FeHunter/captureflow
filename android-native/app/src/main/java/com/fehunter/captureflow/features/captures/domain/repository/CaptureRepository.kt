package com.fehunter.captureflow.features.captures.domain.repository

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.features.captures.domain.model.CaptureImportBatch
import kotlinx.coroutines.flow.Flow

interface CaptureRepository {
    fun observeCaptures(): Flow<List<Capture>>
    suspend fun findById(id: String): Capture?
    fun observeCategories(): Flow<List<CaptureCategory>>
    fun observeSubtasks(): Flow<List<CaptureSubtask>>
    fun observeMedia(): Flow<List<CaptureMedia>>
    suspend fun addMedia(items: List<CaptureMedia>)
    suspend fun setCover(captureId: String, mediaId: String)
    suspend fun replaceCoverImage(captureId: String, uri: String)
    suspend fun save(capture: Capture)
    suspend fun findByContent(content: String): Boolean
    suspend fun findBySourceUrl(url: String): Boolean
    suspend fun updateImage(id: String, imageUri: String?)
    suspend fun updateExtractedContent(id: String, content: String, title: String)
    suspend fun updateTitle(id: String, title: String)
    suspend fun updateNotes(id: String, notes: String?)
    suspend fun updateCategory(id: String, category: String)
    suspend fun setCompleted(id: String, completed: Boolean)
    suspend fun delete(id: String)
    suspend fun addCategory(name: String, sortOrder: Int)
    suspend fun renameCategory(oldName: String, newName: String)
    suspend fun deleteCategory(name: String, fallbackCategory: String)
    suspend fun addSubtask(subtask: CaptureSubtask)
    suspend fun setSubtaskCompleted(id: String, completed: Boolean)
    suspend fun deleteSubtask(id: String)
    suspend fun importBatch(batch: CaptureImportBatch)
}
