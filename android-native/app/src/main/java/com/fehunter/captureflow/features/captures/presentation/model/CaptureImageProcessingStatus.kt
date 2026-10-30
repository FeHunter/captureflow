package com.fehunter.captureflow.features.captures.presentation.model

/** Minimal UI signal used by Capture cards without importing Image Import presentation state. */
data class CaptureImageProcessingStatus(
    val isProcessing: Boolean = false,
    val isError: Boolean = false,
)
