package com.fehunter.captureflow.features.captures.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow

/**
 * Feature application facade: keeps ViewModels independent from persistence contracts
 * while exposing cohesive Captures operations.
 */
class CaptureUseCases(private val repository: CaptureRepository) {
    fun observeCaptures(): Flow<List<Capture>> = repository.observeCaptures()
    suspend fun findById(id: String): Capture? = repository.findById(id)
    fun observeCategories(): Flow<List<CaptureCategory>> = repository.observeCategories()
    fun observeSubtasks(): Flow<List<CaptureSubtask>> = repository.observeSubtasks()
    suspend fun save(capture: Capture) = repository.save(capture)
    suspend fun findByContent(content: String) = repository.findByContent(content)
    suspend fun findBySourceUrl(url: String) = repository.findBySourceUrl(url)
    suspend fun updateImage(id: String, uri: String?) = repository.updateImage(id, uri)
    suspend fun updateExtractedContent(id: String, content: String, title: String) =
        repository.updateExtractedContent(id, content, title)
    suspend fun updateTitle(id: String, title: String) = repository.updateTitle(id, title)
    suspend fun updateNotes(id: String, notes: String?) = repository.updateNotes(id, notes)
    suspend fun setCompleted(id: String, completed: Boolean) = repository.setCompleted(id, completed)
    suspend fun delete(id: String) = repository.delete(id)
    suspend fun addSubtask(item: CaptureSubtask) = repository.addSubtask(item)
    suspend fun setSubtaskCompleted(id: String, completed: Boolean) =
        repository.setSubtaskCompleted(id, completed)
    suspend fun deleteSubtask(id: String) = repository.deleteSubtask(id)
}
