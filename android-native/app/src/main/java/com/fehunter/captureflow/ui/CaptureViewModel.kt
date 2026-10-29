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
import com.fehunter.captureflow.data.SourceUrlDetector
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

data class PendingSharedLink(
    val id: String,
    val originalContent: String,
    val url: String,
    val title: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val isLoading: Boolean = true,
    val metadataFound: Boolean = false,
)

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

    private val _pendingSharedLink = MutableStateFlow<PendingSharedLink?>(null)
    val pendingSharedLink = _pendingSharedLink.asStateFlow()

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
    fun addImageCapture(
        uri: Uri,
        category: String = "Referência",
        deleteSourceScreenshotAfterImport: Boolean = false,
        sourceUrl: String? = null,
        sourceApp: String? = null,
        sourceDetectionMethod: String? = null,
        sourceVerified: Boolean = false,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val storedUri = runCatching {
                imageStorageService.copyToPrivateStorage(getApplication(), uri).toString()
            }.getOrElse {
                // Keep the granted URI as a fallback; OCR will show an actionable error if
                // the provider no longer allows access when the user starts analysis.
                uri.toString()
            }
            val capture = CaptureClassifier.create("Imagem compartilhada", storedUri)
                .copy(
                    title = "Imagem selecionada",
                    category = category,
                    sourceUrl = SourceUrlDetector.extract(sourceUrl.orEmpty()),
                    sourceApp = sourceApp,
                    sourceDetectionMethod = sourceDetectionMethod,
                    sourceVerified = SourceUrlDetector.extract(sourceUrl.orEmpty()) != null && sourceVerified,
                )
            captureDao.insert(capture)
            // Only notification-driven screenshot imports opt into source cleanup.
            // Manually selected gallery images are never deleted automatically.
            if (deleteSourceScreenshotAfterImport && storedUri != uri.toString()) {
                imageStorageService.deleteSourceScreenshot(getApplication(), uri)
            }
            extractText(capture)
        }
    }

    fun receiveSharedIntent(intent: Intent) {
        val incoming = CaptureClassifier.fromIntent(intent) ?: return
        if (incoming.imageUri != null) {
            addImageCapture(
                uri = Uri.parse(incoming.imageUri),
                sourceUrl = incoming.sourceUrl,
                sourceApp = incoming.sourceApp,
                sourceDetectionMethod = incoming.sourceDetectionMethod,
                sourceVerified = incoming.sourceVerified,
            )
            return
        }

        val content = CaptureClassifier.normalize(incoming.content)
        val url = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
            .find(content)?.value?.trimEnd('.', ',', ')', ']', '}', ';')

        if (url != null) {
            prepareSharedLink(content, url)
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val capture = CaptureClassifier.create(content)
            if (!captureDao.containsContent(content)) captureDao.insert(capture)
        }
    }

    private fun prepareSharedLink(content: String, url: String) {
        val requestId = UUID.randomUUID().toString()
        val fallbackTitle = CaptureClassifier.create(content).title
        _pendingSharedLink.value = PendingSharedLink(
            id = requestId,
            originalContent = content,
            url = url,
            title = fallbackTitle,
            isLoading = true,
        )

        viewModelScope.launch {
            val metadata = linkMetadataService.fetch(url)
            val current = _pendingSharedLink.value
            if (current?.id != requestId) return@launch
            _pendingSharedLink.value = current.copy(
                title = metadata?.title?.takeIf(String::isNotBlank) ?: fallbackTitle,
                description = metadata?.description,
                imageUrl = metadata?.imageUrl,
                isLoading = false,
                metadataFound = metadata != null,
            )
        }
    }

    fun cancelPendingSharedLink() {
        _pendingSharedLink.value = null
    }

    fun confirmPendingSharedLink(category: String) {
        val pending = _pendingSharedLink.value ?: return
        if (pending.isLoading || category !in listOf("Ler depois", "Verificar", "Salvar", "Importante")) return
        _pendingSharedLink.value = null

        viewModelScope.launch(Dispatchers.IO) {
            if (captureDao.containsSourceUrl(pending.url)) return@launch

            val fallback = CaptureClassifier.create(pending.originalContent)
            val enrichedContent = listOf(
                pending.originalContent.trim(),
                pending.description?.takeIf(String::isNotBlank),
            ).filterNotNull().filter(String::isNotBlank).distinct().joinToString("\n")
            val capture = fallback.copy(
                title = pending.title.trim().takeIf(String::isNotBlank) ?: fallback.title,
                content = enrichedContent,
                category = category,
                sourceUrl = pending.url,
                sourceDetectionMethod = "shared_text",
                sourceVerified = true,
            )
            captureDao.insert(capture)

            pending.imageUrl?.let { imageUrl ->
                imageStorageService.downloadToPrivateStorage(getApplication(), imageUrl)?.let { localImage ->
                    captureDao.updateImageUri(capture.id, localImage.toString())
                }
            }
        }
    }

    private suspend fun enrichLink(id: String, content: String, fallbackTitle: String) {
        val url = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE).find(content)?.value ?: return
        val metadata = linkMetadataService.fetch(url) ?: return
        val description = metadata.description?.takeIf(String::isNotBlank)
        val enrichedContent = listOf(url, description).filterNotNull().joinToString("\n")
        captureDao.updateExtractedText(id, enrichedContent, metadata.title.ifBlank { fallbackTitle })
        metadata.imageUrl?.let { imageUrl ->
            imageStorageService.downloadToPrivateStorage(getApplication(), imageUrl)?.let { localImage ->
                captureDao.updateImageUri(id, localImage.toString())
            }
        }
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

    fun confirmSourceUrl(id: String) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.confirmSourceUrl(id) }
    }

    fun setCaptureCategory(id: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) { captureDao.updateCategory(id, category) }
    }

    fun updateCaptureTitle(id: String, rawTitle: String) {
        val title = rawTitle.trim().take(160)
        if (title.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) { captureDao.updateTitle(id, title) }
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

                val ocrUrlCandidate = if (capture.sourceUrl == null) SourceUrlDetector.extract(extracted) else null
                if (ocrUrlCandidate != null) {
                    withContext(Dispatchers.IO) {
                        captureDao.updateSourceUrlIfMissing(
                            capture.id,
                            ocrUrlCandidate,
                            "ocr_candidate",
                            false,
                        )
                    }
                }

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

    fun setTaskCategory(id: String, category: String) {
        viewModelScope.launch(Dispatchers.IO) { checklistDao.updateCategory(id, category) }
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

    fun deleteTask(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val task = tasks.value.firstOrNull { it.id == id }
            imageStorageService.deletePrivateImage(getApplication(), task?.imageUri)
            checklistDao.delete(id)
        }
    }
}
