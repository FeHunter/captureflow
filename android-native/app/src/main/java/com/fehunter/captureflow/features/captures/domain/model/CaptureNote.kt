package com.fehunter.captureflow.features.captures.domain.model

/** A thought-note block owned by a Capture aggregate. */
data class CaptureNote(
    val id: String,
    val text: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
