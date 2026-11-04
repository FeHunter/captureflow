package com.fehunter.captureflow.features.imageimport.presentation.model

data class PendingSharedLink(
    val id: String,
    val originalContent: String,
    val url: String,
    val title: String,
    val titleManuallyEdited: Boolean = false,
    val description: String? = null,
    val imageUrl: String? = null,
    val imageUrls: List<String> = listOfNotNull(imageUrl),
    val isLoading: Boolean = true,
    val metadataFound: Boolean = false,
)

data class OcrUiState(
    val isProcessing: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false,
)


/** Temporary image draft. It is not persisted until the user confirms the review. */
data class PendingImageReview(
    val id: String,
    val imageUri: String,
    val title: String,
    val category: String = "Salvar",
    val isProcessing: Boolean = true,
    val error: String? = null,
)
