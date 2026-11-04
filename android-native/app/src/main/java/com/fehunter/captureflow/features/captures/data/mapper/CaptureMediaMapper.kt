package com.fehunter.captureflow.features.captures.data.mapper

import com.fehunter.captureflow.features.captures.data.local.CaptureMediaEntity
import com.fehunter.captureflow.features.captures.domain.model.CaptureMedia

internal fun CaptureMediaEntity.toDomain() = CaptureMedia(
    id, captureId, uri, mediaType, position, isCover, sourceUrl, createdAt,
)
internal fun CaptureMedia.toEntity() = CaptureMediaEntity(
    id, captureId, uri, mediaType, position, isCover, sourceUrl, createdAt,
)
