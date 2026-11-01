package com.fehunter.captureflow.features.fullscreen.domain

import com.fehunter.captureflow.core.util.YoutubeVideoIdParser as SharedYoutubeVideoIdParser

/** Backwards-compatible facade; URL recognition is shared from Core. */
object YoutubeVideoIdParser {
    fun extractVideoId(rawUrl: String?): String? =
        SharedYoutubeVideoIdParser.extractVideoId(rawUrl)
}
