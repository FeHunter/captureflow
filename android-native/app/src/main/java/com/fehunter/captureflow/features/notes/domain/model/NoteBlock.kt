package com.fehunter.captureflow.features.notes.domain.model

data class NoteBlock(
    val id: String,
    val text: String,
    val imageUri: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)
