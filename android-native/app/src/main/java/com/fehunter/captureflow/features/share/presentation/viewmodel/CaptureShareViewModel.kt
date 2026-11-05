package com.fehunter.captureflow.features.share.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import com.fehunter.captureflow.features.share.domain.usecase.CaptureShareUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CaptureShareUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
    val shareablePdfUri: String? = null,
)

class CaptureShareViewModel(private val useCases: CaptureShareUseCases) : ViewModel() {
    private val _state = MutableStateFlow(CaptureShareUiState())
    val state = _state.asStateFlow()

    fun exportPdf(payload: CaptureSharePayload, destination: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = CaptureShareUiState(isBusy = true)
        runCatching { useCases.exportPdf(payload, destination) }
            .onSuccess { _state.value = CaptureShareUiState(message = "PDF exportado com sucesso.") }
            .onFailure { _state.value = CaptureShareUiState(error = it.message ?: "Não foi possível exportar o PDF.") }
    }

    fun exportPdfForSharing(payload: CaptureSharePayload, destination: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = CaptureShareUiState(isBusy = true)
        runCatching { useCases.exportPdf(payload, destination) }
            .onSuccess { _state.value = CaptureShareUiState(message = "PDF pronto para compartilhar.", shareablePdfUri = destination) }
            .onFailure { _state.value = CaptureShareUiState(error = it.message ?: "Não foi possível preparar o PDF para compartilhamento.") }
    }

    fun consumeShareablePdf() {
        _state.value = _state.value.copy(shareablePdfUri = null)
    }

    fun reportShareError(message: String) {
        _state.value = _state.value.copy(isBusy = false, error = message, shareablePdfUri = null)
    }

    fun exportRtf(payload: CaptureSharePayload, destination: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = CaptureShareUiState(isBusy = true)
        runCatching { useCases.exportRtf(payload, destination) }
            .onSuccess { _state.value = CaptureShareUiState(message = "RTF exportado com sucesso.") }
            .onFailure { _state.value = CaptureShareUiState(error = it.message ?: "Não foi possível exportar o RTF.") }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }
}
