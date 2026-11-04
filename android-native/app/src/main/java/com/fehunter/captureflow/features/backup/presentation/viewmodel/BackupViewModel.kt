package com.fehunter.captureflow.features.backup.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fehunter.captureflow.features.backup.domain.usecase.BackupUseCases
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class BackupUiState(
    val isBusy: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)

class BackupViewModel(private val useCases: BackupUseCases) : ViewModel() {
    private val _state = MutableStateFlow(BackupUiState())
    val state = _state.asStateFlow()

    fun exportTo(uri: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = BackupUiState(isBusy = true)
        runCatching { useCases.export(uri) }
            .onSuccess { result ->
                _state.value = BackupUiState(message =
                    "Backup criado: ${result.captures} referências, ${result.categories} categorias e ${result.images} imagens.")
            }
            .onFailure { error ->
                _state.value = BackupUiState(error = error.message ?: "Não foi possível exportar o backup.")
            }
    }

    fun importFrom(uri: String) = viewModelScope.launch(Dispatchers.IO) {
        _state.value = BackupUiState(isBusy = true)
        runCatching { useCases.import(uri) }
            .onSuccess { result ->
                _state.value = BackupUiState(message =
                    "Importação concluída: ${result.captures} referências, ${result.categories} categorias e ${result.images} imagens.")
            }
            .onFailure { error ->
                _state.value = BackupUiState(error = error.message ?: "Não foi possível importar o backup.")
            }
    }

    fun clearMessage() { _state.value = _state.value.copy(message = null, error = null) }
}
