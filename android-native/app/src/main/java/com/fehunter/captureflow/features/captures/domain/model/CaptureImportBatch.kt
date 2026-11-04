package com.fehunter.captureflow.features.captures.domain.model

/** Validated set of records imported as one atomic operation. */
data class CaptureImportBatch(
    val captures: List<Capture>,
    val categories: List<CaptureCategory>,
    val subtasks: List<CaptureSubtask>,
    val media: List<CaptureMedia>,
)
