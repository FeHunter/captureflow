package com.fehunter.captureflow.features.captures.data.mapper

import com.fehunter.captureflow.features.captures.data.local.CaptureCategoryEntity
import com.fehunter.captureflow.features.captures.data.local.CaptureEntity
import com.fehunter.captureflow.features.captures.data.local.CaptureSubtaskEntity
import com.fehunter.captureflow.features.captures.domain.model.Capture
import com.fehunter.captureflow.features.captures.domain.model.CaptureCategory
import com.fehunter.captureflow.features.captures.domain.model.CaptureSubtask

internal fun CaptureEntity.toDomain() = Capture(
    id, title, content, kind, category, sourceUrl, imageUri, createdAt,
    isCompleted, previousCategory, notes,
)

internal fun Capture.toEntity() = CaptureEntity(
    id, title, content, kind, category, sourceUrl, imageUri, createdAt,
    isCompleted, previousCategory, notes,
)

internal fun CaptureCategoryEntity.toDomain() = CaptureCategory(name, sortOrder)
internal fun CaptureCategory.toEntity() = CaptureCategoryEntity(name, sortOrder)

internal fun CaptureSubtaskEntity.toDomain() =
    CaptureSubtask(id, captureId, title, isCompleted, createdAt)

internal fun CaptureSubtask.toEntity() =
    CaptureSubtaskEntity(id, captureId, title, isCompleted, createdAt)
