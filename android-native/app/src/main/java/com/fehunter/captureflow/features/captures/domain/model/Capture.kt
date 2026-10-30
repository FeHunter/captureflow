package com.fehunter.captureflow.features.captures.domain.model

data class Capture(
    val id: String,
    val title: String,
    val content: String,
    val kind: String,
    val category: String,
    val sourceUrl: String?,
    val imageUri: String?,
    val createdAt: Long,
    val isCompleted: Boolean = false,
    val previousCategory: String? = null,
    val notes: String? = null,
)

data class CaptureCategory(val name: String, val sortOrder: Int)

data class CaptureSubtask(
    val id: String,
    val captureId: String,
    val title: String,
    val isCompleted: Boolean = false,
    val createdAt: Long,
)
