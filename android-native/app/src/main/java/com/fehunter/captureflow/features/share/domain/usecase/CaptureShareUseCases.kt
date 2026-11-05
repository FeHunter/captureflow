package com.fehunter.captureflow.features.share.domain.usecase

import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import com.fehunter.captureflow.features.share.domain.repository.CaptureShareRepository

class CaptureShareUseCases(private val repository: CaptureShareRepository) {
    suspend fun exportPdf(payload: CaptureSharePayload, destination: String) =
        repository.exportPdf(payload, destination)

    suspend fun exportRtf(payload: CaptureSharePayload, destination: String) =
        repository.exportRtf(payload, destination)
}
