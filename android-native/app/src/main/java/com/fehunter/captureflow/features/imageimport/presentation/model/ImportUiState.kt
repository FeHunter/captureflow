package com.fehunter.captureflow.features.imageimport.presentation.model

data class PendingSharedLink(
    val id: String,
    val originalContent: String,
    val url: String,
    val title: String,
    val titleManuallyEdited: Boolean = false,
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
