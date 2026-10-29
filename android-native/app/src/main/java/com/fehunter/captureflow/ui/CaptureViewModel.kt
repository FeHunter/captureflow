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
    private val ocrService = OcrService()
    private val imageStorageService = SharedImageStorageService()

    private val _ocrStates = MutableStateFlow<Map<String, OcrUiState>>(emptyMap())
    val ocrStates = _ocrStates.asStateFlow()

    val captures = captureDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tasks = checklistDao.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun addCapture(raw: String) {
        val content = CaptureClassifier.normalize(raw)
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            if (!captureDao.containsContent(content)) captureDao.insert(CaptureClassifier.create(content))
        }
    }

    /**
     * Saves an image chosen with Android's system photo picker as a local capture.
     * Copying it immediately avoids relying on the picker URI grant later during OCR.
     */
    fun addImageCapture(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val storedUri = runCatching {
                imageStorageService.copyToPrivateStorage(getApplication(), uri).toString()
            }.getOrElse {
                // Keep the granted URI as a fallback; OCR will show an actionable error if
                // the provider no longer allows access when the user starts analysis.
                uri.toString()
            }
            val capture = CaptureClassifier.create("Imagem compartilhada", storedUri)
                .copy(title = "Imagem selecionada")
            captureDao.insert(capture)
        }
    }

    fun receiveSharedIntent(intent: Intent) {
        val incoming = CaptureClassifier.fromIntent(intent) ?: return
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
            }
        }
    }

    fun setCaptureCompleted(id: String, completed: Boolean) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.setCompleted(id, completed) }
    }

    fun deleteCapture(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val capture = captures.value.firstOrNull { it.id == id }
            imageStorageService.deletePrivateImage(getApplication(), capture?.imageUri)
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
                val extracted = withContext(Dispatchers.IO) {
                    ocrService.extractText(getApplication(), Uri.parse(rawUri))
                }.trim()

                if (extracted.isBlank()) {
                    setOcrState(
                        capture.id,
                        OcrUiState(message = "Nenhum texto foi identificado nesta imagem.", isError = true),
                    )
                    return@launch
                }

                val combined = if (capture.content == "Imagem compartilhada") {
                    extracted
                } else {
                    listOf(capture.content, extracted).filter(String::isNotBlank).distinct().joinToString("\n")
                }
                withContext(Dispatchers.IO) {
                    captureDao.updateExtractedText(
                        capture.id,
                        combined,
                        extracted.lineSequence().first().take(72),
                    )
                }
                setOcrState(capture.id, OcrUiState(message = "Texto extraído e salvo nesta captura."))
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

    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) { checklistDao.delete(id) }
    }
}
