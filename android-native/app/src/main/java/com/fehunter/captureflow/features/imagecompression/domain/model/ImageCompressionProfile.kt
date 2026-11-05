package com.fehunter.captureflow.features.imagecompression.domain.model

/** Adaptive encoder targets; values are quality parameters, not size percentages. */
enum class ImageCompressionProfile(
    val jpegQuality: Int,
    val webpQuality: Int,
    val label: String,
) {
    MEDIUM(70, 78, "Médio"),
    HIGH(52, 66, "Alto");

    /** Backwards-compatible alias for callers that still use the previous JPEG field. */
    val quality: Int get() = jpegQuality

    companion object {
        fun fromPreference(value: String?): ImageCompressionProfile =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}
