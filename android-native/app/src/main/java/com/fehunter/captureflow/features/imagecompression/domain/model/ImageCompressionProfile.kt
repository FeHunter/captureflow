package com.fehunter.captureflow.features.imagecompression.domain.model

/** JPEG encoder quality settings; these values are not percentages of file-size reduction. */
enum class ImageCompressionProfile(val quality: Int, val label: String) {
    MEDIUM(70, "Médio"),
    HIGH(50, "Alto");

    companion object {
        fun fromPreference(value: String?): ImageCompressionProfile =
            values().firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEDIUM
    }
}
