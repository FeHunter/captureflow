package com.fehunter.captureflow.features.captures.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.core.storage.PrivateImageStorage
import com.fehunter.captureflow.features.imagecompression.domain.service.ImageCompressor
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureFactory
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia
import com.fehunter.captureflow.features.captures.domain.model.CaptureNotesCodec
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID

class CaptureViewModel(
    private val captureUseCases: CaptureUseCases,
    private val privateImageStorage: PrivateImageStorage,
    private val imageCompressor: ImageCompressor,
) : ViewModel() {
    val captures = captureUseCases.observeCaptures().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    val captureSubtasks = captureUseCases.observeSubtasks().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )
    private val _coverUpdateError = MutableStateFlow<String?>(null)
    val coverUpdateError = _coverUpdateError.asStateFlow()

    val captureMedia = captureUseCases.observeMedia().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList(),
    )

    fun createTask(rawTitle: String, category: String) {
        val title = rawTitle.trim().take(160)
        val targetCategory = category.trim()
        if (title.isBlank() || targetCategory.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val availableCategories = captureUseCases.observeCategories().first().map { it.name }
            if (targetCategory !in availableCategories) return@launch
            captureUseCases.save(CaptureFactory.createTask(title, targetCategory))
        }
    }

    fun setCaptureCover(captureId: String, mediaId: String) {
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.setCover(captureId, mediaId) }
    }
    fun replaceCaptureCover(
        captureId: String,
        sourceUri: String,
        profile: ImageCompressionProfile,
        onComplete: () -> Unit = {},
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var newUri: String? = null
            try {
                val previousUri = captureUseCases.findById(captureId)?.imageUri
                newUri = imageCompressor.compressToPrivateStorage(sourceUri, profile)
                captureUseCases.replaceCoverImage(captureId, newUri)
                if (previousUri != newUri) privateImageStorage.deletePrivateImage(previousUri)
            } catch (error: Exception) {
                privateImageStorage.deletePrivateImage(newUri)
                _coverUpdateError.value = error.localizedMessage?.takeIf(String::isNotBlank)
                    ?: "Não foi possível atualizar a imagem da capa."
            } finally {
                onComplete()
            }
        }
    }

    fun clearCoverUpdateError() { _coverUpdateError.value = null }

    fun addCaptureSubtask(captureId: String, rawTitle: String) {
        val title = rawTitle.trim().take(160)
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.addSubtask(CaptureSubtask(
                id = UUID.randomUUID().toString(), captureId = captureId, title = title,
                isCompleted = false, createdAt = System.currentTimeMillis(),
            ))
        }
    }
    fun setCaptureSubtaskCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.setSubtaskCompleted(id, completed) }
    }
    fun deleteCaptureSubtask(id: String) {
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.deleteSubtask(id) }
    }
    fun setCaptureCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.setCompleted(id, completed) }
    }
    fun updateCaptureTitle(id: String, rawTitle: String) {
        val title = rawTitle.trim().take(160)
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.updateTitle(id, title) }
    }
    fun updateCaptureCategory(id: String, category: String) {
        val normalizedCategory = category.trim()
        if (normalizedCategory.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { captureUseCases.updateCategory(id, normalizedCategory) }
    }
    fun updateCaptureNotes(id: String, rawNotes: String) {
        val notes = rawNotes.takeIf(String::isNotBlank)
        viewModelScope.launch(Dispatchers.IO) {
            val previousImages = captureUseCases.findById(id)?.notes
                ?.let(CaptureNotesCodec::decode).orEmpty().mapNotNull { it.imageUri }.toSet()
            val retainedImages = CaptureNotesCodec.decode(notes).mapNotNull { it.imageUri }.toSet()
            captureUseCases.updateNotes(id, notes)
            (previousImages - retainedImages).forEach(privateImageStorage::deletePrivateImage)
        }
    }
    fun deleteCapture(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val capture = captureUseCases.findById(id)
            val mediaUris = captureMedia.value.filter { it.captureId == id }.map { it.uri }
            captureUseCases.delete(id)
            privateImageStorage.deletePrivateImage(capture?.imageUri)
            mediaUris.forEach(privateImageStorage::deletePrivateImage)
            capture?.notes?.let(CaptureNotesCodec::decode).orEmpty()
                .mapNotNull { it.imageUri }.forEach(privateImageStorage::deletePrivateImage)
        }
    }
}
