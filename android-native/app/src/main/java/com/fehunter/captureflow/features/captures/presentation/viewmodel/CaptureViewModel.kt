package com.fehunter.captureflow.features.captures.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.core.storage.PrivateImageStorage
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/** Owns Capture and CaptureSubtask state and actions. */
class CaptureViewModel(
    private val captureUseCases: CaptureUseCases,
    private val privateImageStorage: PrivateImageStorage,
) : ViewModel() {
    val captures = captureUseCases.observeCaptures().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    val captureSubtasks = captureUseCases.observeSubtasks().stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList(),
    )

    fun addCaptureSubtask(captureId: String, rawTitle: String) {
        val title = rawTitle.trim().take(160)
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.addSubtask(
                CaptureSubtask(
                    id = UUID.randomUUID().toString(),
                    captureId = captureId,
                    title = title,
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun setCaptureSubtaskCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.setSubtaskCompleted(id, completed)
        }
    }

    fun deleteCaptureSubtask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.deleteSubtask(id)
        }
    }

    fun setCaptureCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.setCompleted(id, completed)
        }
    }

    fun updateCaptureTitle(id: String, rawTitle: String) {
        val title = rawTitle.trim().take(160)
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.updateTitle(id, title)
        }
    }

    fun updateCaptureNotes(id: String, rawNotes: String) {
        val notes = rawNotes.trim().take(10_000).takeIf(String::isNotBlank)
        viewModelScope.launch(Dispatchers.IO) {
            captureUseCases.updateNotes(id, notes)
        }
    }

    fun deleteCapture(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val capture = captureUseCases.findById(id)
            captureUseCases.delete(id)
            privateImageStorage.deletePrivateImage(capture?.imageUri)
        }
    }
}
