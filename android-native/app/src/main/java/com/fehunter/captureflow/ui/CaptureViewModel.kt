package com.fehunter.captureflow.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.data.CaptureClassifier
import com.fehunter.captureflow.data.CaptureDatabase
import com.fehunter.captureflow.data.CaptureEntity
import com.fehunter.captureflow.data.CaptureSubtaskEntity
import com.fehunter.captureflow.data.ChecklistTaskEntity
import com.fehunter.captureflow.services.LinkMetadataService
import com.fehunter.captureflow.services.OcrService
import com.fehunter.captureflow.services.SharedImageStorageService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class OcrUiState(
    val isProcessing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)

class CaptureViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CaptureDatabase.get(application)
    private val captureDao = database.captureDao()
    private val checklistDao = database.checklistDao()
    private val captureSubtaskDao = database.captureSubtaskDao()
    private val ocrService = OcrService()
    private val linkMetadataService = LinkMetadataService()
    private val imageStorageService = SharedImageStorageService()

    private val _ocrStates = MutableStateFlow<Map<String, OcrUiState>>(emptyMap())
    val ocrStates = _ocrStates.asStateFlow()

    val captures = captureDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tasks = checklistDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val captureSubtasks = captureSubtaskDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCapture(raw: String) {
        val content = CaptureClassifier.normalize(raw)
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!captureDao.containsContent(content)) {
                val capture = CaptureClassifier.create(content)
                captureDao.insert(capture)
                enrichLink(capture.id, content, capture.title)
            }
        }
    }

    /**
     * Saves an image chosen with Android's system photo picker as a local capture.
     * Copying it immediately avoids relying on the picker URI grant later during OCR.
     */
    fun addImageCapture(uri: Uri, category: String = "Referência") {
        viewModelScope.launch(Dispatchers.IO) {
            val storedUri = runCatching {
                imageStorageService.copyToPrivateStorage(getApplication(), uri).toString()
            }.getOrElse {
                // Keep the granted URI as a fallback; OCR will show an actionable error if
                // the provider no longer allows access when the user starts analysis.
                uri.toString()
            }
            val capture = CaptureClassifier.create("Imagem compartilhada", storedUri)
                .copy(title = "Imagem selecionada", category = category)
            captureDao.insert(capture)
            extractText(capture)
        }
    }

    fun receiveSharedIntent(intent: Intent) {
        val incoming = CaptureClassifier.fromIntent(intent) ?: return
        if (incoming.imageUri != null) {
            addImageCapture(Uri.parse(incoming.imageUri))
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val imageUri = incoming.imageUri?.let { sharedUri ->
                runCatching {
                    imageStorageService.copyToPrivateStorage(getApplication(), Uri.parse(sharedUri)).toString()
                }.getOrElse {
                    // Keep the original URI as a fallback; OCR will report if the provider
                    // no longer allows access. The normal path uses the durable private copy.
                    sharedUri
                }
            }
            val content = CaptureClassifier.normalize(incoming.content)
            val capture = CaptureClassifier.create(content, imageUri)
            // Images use a generic placeholder as content, so content-only deduplication
            // would incorrectly discard every image after the first one.
            if (imageUri != null || !captureDao.containsContent(content)) {
                captureDao.insert(capture)
                if (imageUri == null) enrichLink(capture.id, content, capture.title)
            }
        }
    }

    private suspend fun enrichLink(id: String, content: String, fallbackTitle: String) {
        val url = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).find(content)?.value ?: return
        val metadata = linkMetadataService.fetch(url) ?: return
        val description = metadata.description?.takeIf(String::isNotBlank)
        val enrichedContent = listOf(url, description).filterNotNull().joinToString("\n")
        captureDao.updateExtractedText(id, enrichedContent, metadata.title.ifBlank { fallbackTitle })
    }

    fun addCaptureSubtask(captureId: String, rawTitle: String) {
        val title = rawTitle.trim()
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            captureSubtaskDao.insert(
                CaptureSubtaskEntity(
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
            captureSubtaskDao.setCompleted(id, completed)
        }
    }

    fun deleteCaptureSubtask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            captureSubtaskDao.delete(id)
        }
    }

    fun setCaptureCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.setCompleted(id, completed) }
    }

    fun deleteCapture(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val capture = captures.value.firstOrNull { it.id == id }
            imageStorageService.deletePrivateImage(getApplication(), capture?.imageUri)
            captureSubtaskDao.deleteForCapture(id)
            captureDao.delete(id)
            _ocrStates.value = _ocrStates.value - id
        }
    }

    fun extractText(capture: CaptureEntity) {
        val rawUri = capture.imageUri ?: return
        if (_ocrStates.value[capture.id]?.isProcessing == true) return

        setOcrState(capture.id, OcrUiState(isProcessing = true, message = "Lendo imagem…"))
        viewModelScope.launch {
            try {
                val analysis = withContext(Dispatchers.IO) {
                    ocrService.extractStructuredText(getApplication(), Uri.parse(rawUri))
                }
                val extracted = analysis.fullText.trim()

                if (extracted.isBlank()) {
                    setOcrState(
                        capture.id,
                        OcrUiState(message = "Nenhum texto foi identificado nesta imagem.", isError = true),
                    )
                    return@launch
                }

                val title = analysis.title.ifBlank {
                    extracted.lineSequence().firstOrNull(String::isNotBlank)?.take(72) ?: "Screenshot analisado"
                }
                withContext(Dispatchers.IO) {
                    // For image references, keep only the title as the useful payload.
                    // The original screenshot remains available through imageUri as the cover.
                    captureDao.updateExtractedText(capture.id, "", title)
                }
                setOcrState(capture.id, OcrUiState())
            } catch (error: Exception) {
                setOcrState(
                    capture.id,
                    OcrUiState(
                        message = error.localizedMessage?.takeIf(String::isNotBlank)
                            ?: "Não foi possível ler esta imagem. Tente compartilhá-la novamente.",
                        isError = true,
                    ),
                )
            }
        }
    }

    private fun setOcrState(id: String, state: OcrUiState) {
        _ocrStates.value = _ocrStates.value.toMutableMap().apply { put(id, state) }
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

    fun analyzeTaskScreenshot(task: ChecklistTaskEntity) {
        val rawUri = task.imageUri ?: return
        viewModelScope.launch {
            try {
                val analysis = withContext(Dispatchers.IO) {
                    ocrService.extractStructuredText(getApplication(), Uri.parse(rawUri))
                }
                if (analysis.fullText.isBlank()) return@launch
                val title = analysis.title.ifBlank { "Screenshot para revisar" }
                val description = analysis.description.ifBlank { analysis.fullText }.take(3_000)
                withContext(Dispatchers.IO) {
                    checklistDao.updateDetails(task.id, title, description)
                }
            } catch (_: Exception) {
                // Keep the original task and screenshot when OCR cannot read the image.
            }
        }
    }

    fun addImageTask(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val storedUri = runCatching {
                imageStorageService.copyToPrivateStorage(getApplication(), uri).toString()
            }.getOrElse {
                uri.toString()
            }
            checklistDao.insert(
                ChecklistTaskEntity(
                    id = UUID.randomUUID().toString(),
                    title = "Revisar screenshot",
                    isCompleted = false,
                    createdAt = System.currentTimeMillis(),
                    imageUri = storedUri,
                ),
            )
        }
    }

    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = tasks.value.firstOrNull { it.id == id }
            imageStorageService.deletePrivateImage(getApplication(), task?.imageUri)
            checklistDao.delete(id)
        }
    }
}
