package com.fehunter.captureflow.features.captures.domain.repository

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import kotlinx.coroutines.flow.Flow

/**
 * Public domain boundary for the Captures feature.
 * Room entities and DAOs must remain behind this contract.
 */
interface CaptureRepository {
    fun observeCaptures(): Flow<List<Capture>>
    suspend fun findById(id: String): Capture?
    fun observeCategories(): Flow<List<CaptureCategory>>
    fun observeSubtasks(): Flow<List<CaptureSubtask>>

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
}
