package com.fehunter.captureflow.features.share.domain.model

data class CaptureShareNote(
    val text: String,
    val imageUri: String?,
)

data class CaptureSharePayload(
    val title: String,
    val category: String,
    val createdAt: Long,
    val sourceUrl: String?,
    val content: String,
    val coverUri: String?,
    val mediaUris: List<String>,
    val notes: List<CaptureShareNote>,
)
