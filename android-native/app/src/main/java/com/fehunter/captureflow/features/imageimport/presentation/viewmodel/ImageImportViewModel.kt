package com.fehunter.captureflow.features.imageimport.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureFactory
import com.fehunter.captureflow.features.captures.domain.usecase.CaptureUseCases
import com.fehunter.captureflow.features.imageimport.domain.gateway.ImageImportGateway
import com.fehunter.captureflow.features.imageimport.presentation.model.OcrUiState
import com.fehunter.captureflow.features.imageimport.presentation.model.PendingSharedLink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Owns link sharing, image ingestion and OCR presentation state. */
class ImageImportViewModel(
    private val captureUseCases: CaptureUseCases,
    private val gateway: ImageImportGateway,
) : ViewModel() {
    private val _ocrStates = MutableStateFlow<Map<String, OcrUiState>>(emptyMap())
    val ocrStates = _ocrStates.asStateFlow()

    private val _pendingSharedLink = MutableStateFlow<PendingSharedLink?>(null)
    val pendingSharedLink = _pendingSharedLink.asStateFlow()

    fun receiveSharedContent(content: String, imageUri: String? = null) {
        if (imageUri != null) {
            addImageCapture(imageUri)
            return
        }
        val normalized = CaptureFactory.normalize(content)
        val url = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
            .find(normalized)?.value?.trimEnd('.', ',', ')', ']', '}', ';')
        if (url != null) {
            prepareSharedLink(normalized, url)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (normalized.isNotBlank() && !captureUseCases.findByContent(normalized)) {
                captureUseCases.save(CaptureFactory.create(normalized))
            }
        }
    }

    fun addImageCapture(
        uri: String,
        category: String = "Salvar",
        deleteSourceScreenshotAfterImport: Boolean = false,
    ) {
        val targetCategory = category.takeUnless { it == "Concluído" } ?: "Salvar"
        viewModelScope.launch(Dispatchers.IO) {
            val storedUri = runCatching {
                gateway.copyImageToPrivateStorage(uri)
            }.getOrElse { uri.toString() }
            val capture = CaptureFactory.create("Imagem compartilhada", storedUri)
                .copy(title = "Imagem selecionada", category = targetCategory)
            captureUseCases.save(capture)
            if (deleteSourceScreenshotAfterImport && storedUri != uri.toString()) {
                gateway.deleteSourceScreenshot(uri)
            }
            extractText(capture)
        }
    }

    private fun prepareSharedLink(content: String, url: String) {
        val requestId = UUID.randomUUID().toString()
        val fallbackTitle = CaptureFactory.create(content).title
        _pendingSharedLink.value = PendingSharedLink(
            id = requestId,
            originalContent = content,
            url = url,
            title = fallbackTitle,
            isLoading = true,
        )
        viewModelScope.launch {
            val metadata = gateway.fetchLinkMetadata(url)
            val current = _pendingSharedLink.value
            if (current?.id != requestId) return@launch
            _pendingSharedLink.value = current.copy(
                title = if (current.titleManuallyEdited) current.title
                    else metadata?.title?.takeIf(String::isNotBlank) ?: fallbackTitle,
                description = metadata?.description,
                imageUrl = metadata?.imageUrl,
                isLoading = false,
                metadataFound = metadata != null,
            )
        }
    }

    fun updatePendingSharedLinkTitle(title: String) {
        _pendingSharedLink.value = _pendingSharedLink.value?.copy(
            title = title.take(160),
            titleManuallyEdited = true,
        )
    }

    fun cancelPendingSharedLink() {
        _pendingSharedLink.value = null
    }

    fun confirmPendingSharedLink(category: String) {
        val pending = _pendingSharedLink.value ?: return
        if (pending.isLoading || category == "Concluído") return

        viewModelScope.launch(Dispatchers.IO) {
            // Read the repository flow directly: the StateFlow below is WhileSubscribed
            // and may not have an active UI collector when confirmation is invoked.
            val availableCategories = captureUseCases.observeCategories().first().map { it.name }
            if (category !in availableCategories) return@launch
            if (_pendingSharedLink.value?.id != pending.id) return@launch

            _pendingSharedLink.value = null
            if (captureUseCases.findBySourceUrl(pending.url)) return@launch
            val fallback = CaptureFactory.create(pending.originalContent)
            val enrichedContent = listOf(
                pending.originalContent.trim(),
                pending.description?.takeIf(String::isNotBlank),
            ).filterNotNull().filter(String::isNotBlank).distinct().joinToString("\n")
            val capture = fallback.copy(
                title = pending.title.trim().takeIf(String::isNotBlank) ?: fallback.title,
                content = enrichedContent,
                category = category,
                sourceUrl = pending.url,
            )
            captureUseCases.save(capture)
            pending.imageUrl?.let { imageUrl ->
                gateway.downloadPreviewToPrivateStorage(imageUrl)?.let { localImage ->
                    captureUseCases.updateImage(capture.id, localImage)
                }
            }
        }
    }

    fun extractText(capture: Capture) {
        val rawUri = capture.imageUri ?: return
        if (_ocrStates.value[capture.id]?.isProcessing == true) return
        setOcrState(capture.id, OcrUiState(isProcessing = true, message = "Lendo imagem…"))
        viewModelScope.launch {
            try {
                val analysis = withContext(Dispatchers.IO) {
                    gateway.analyzeImage(rawUri)
                }
                val extracted = analysis.fullText.trim()
                if (extracted.isBlank()) {
                    setOcrState(capture.id, OcrUiState(
                        message = "Nenhum texto foi identificado nesta imagem.",
                        isError = true,
                    ))
                    return@launch
                }
                val title = analysis.title.ifBlank {
                    extracted.lineSequence().firstOrNull(String::isNotBlank)?.take(72)
                        ?: "Screenshot analisado"
                }
                withContext(Dispatchers.IO) {
                    captureUseCases.updateExtractedContent(capture.id, "", title)
                }
                setOcrState(capture.id, OcrUiState())
            } catch (error: Exception) {
                setOcrState(capture.id, OcrUiState(
                    message = error.localizedMessage?.takeIf(String::isNotBlank)
                        ?: "Não foi possível ler esta imagem. Tente compartilhá-la novamente.",
                    isError = true,
                ))
            }
        }
    }

    private fun setOcrState(id: String, state: OcrUiState) {
        _ocrStates.value = _ocrStates.value.toMutableMap().apply { put(id, state) }
    }
}
