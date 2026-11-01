package com.fehunter.captureflow.features.imagecompression.domain.model

/** JPEG quality targets: Medium retains 70%; High retains 50% quality. */
enum class ImageCompressionProfile(val quality: Int, val label: String) {
    MEDIUM(70, "Médio"),
    HIGH(50, "Alto");

    companion object {
        fun fromPreference(value: String?): ImageCompressionProfile =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}
