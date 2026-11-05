package com.fehunter.captureflow.features.share.domain.repository

import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload

interface CaptureShareRepository {
    suspend fun exportPdf(payload: CaptureSharePayload, destination: String)
    suspend fun exportRtf(payload: CaptureSharePayload, destination: String)
}
