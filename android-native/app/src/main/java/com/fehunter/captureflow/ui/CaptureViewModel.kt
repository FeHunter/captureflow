package com.fehunter.captureflow.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.data.CaptureClassifier
import com.fehunter.captureflow.data.CaptureDatabase
import com.fehunter.captureflow.data.CaptureEntity
import com.fehunter.captureflow.data.ChecklistTaskEntity
import com.fehunter.captureflow.services.OcrService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class CaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CaptureDatabase.get(application)
    private val captureDao = database.captureDao()
    private val checklistDao = database.checklistDao()
    private val ocrService = OcrService()

    val captures = captureDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tasks = checklistDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCapture(raw: String) {
        val content = CaptureClassifier.normalize(raw)
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!captureDao.containsContent(content)) captureDao.insert(CaptureClassifier.create(content))
        }
    }

    fun receiveSharedIntent(intent: Intent) {
        val incoming = CaptureClassifier.fromIntent(intent) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val content = CaptureClassifier.normalize(incoming.content)
            if (!captureDao.containsContent(content)) captureDao.insert(CaptureClassifier.create(content, incoming.imageUri))
        }
    }

    fun setCaptureCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.setCompleted(id, completed) }
    }
    fun deleteCapture(id: String) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.delete(id) }
    }

    fun extractText(capture: CaptureEntity) {
        val rawUri = capture.imageUri ?: return
        viewModelScope.launch {
            val extracted = runCatching {
                withContext(Dispatchers.IO) { ocrService.extractText(getApplication(), Uri.parse(rawUri)) }
            }.getOrNull()?.trim().orEmpty()
            if (extracted.isBlank()) return@launch
            val combined = listOf(capture.content, extracted).filter(String::isNotBlank).distinct().joinToString("\n")
            withContext(Dispatchers.IO) {
                captureDao.updateExtractedText(capture.id, combined, extracted.lineSequence().first().take(72))
            }
        }
    }

    fun addTask(rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            checklistDao.insert(ChecklistTaskEntity(UUID.randomUUID().toString(), title, false, System.currentTimeMillis()))
        }
    }
    fun setTaskCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { checklistDao.setCompleted(id, completed) }
    }
    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) { checklistDao.delete(id) }
    }
}
