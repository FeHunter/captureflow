package com.fehunter.captureflow.features.captures.domain.model

/** A locally stored image belonging to a Capture. */
data class CaptureMedia(
    val id: String,
    val captureId: String,
    val uri: String,
    val mediaType: String,
    val position: Int,
    val isCover: Boolean,
    val sourceUrl: String?,
    val createdAt: Long,
)
