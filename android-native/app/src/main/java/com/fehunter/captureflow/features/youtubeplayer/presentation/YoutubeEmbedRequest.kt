package com.fehunter.captureflow.features.youtubeplayer.presentation

import java.net.URLEncoder

internal data class YoutubeEmbedRequest(
    val url: String,
    val headers: Map<String, String>,
)

internal object YoutubeEmbedRequestFactory {
    fun create(videoId: String, applicationId: String): YoutubeEmbedRequest {
        require(videoId.matches(Regex("[A-Za-z0-9_-]{11}"))) { "Invalid YouTube video ID" }
        require(applicationId.matches(Regex("[A-Za-z0-9_.]+"))) { "Invalid Android application ID" }

        val origin = "https://${applicationId.lowercase()}"
        val referrer = "$origin/"
        val encodedOrigin = URLEncoder.encode(origin, "UTF-8")
        return YoutubeEmbedRequest(
            url = "https://www.youtube-nocookie.com/embed/$videoId" +
                "?enablejsapi=1&autoplay=0&controls=1&playsinline=1&rel=0&origin=$encodedOrigin",
            headers = mapOf("Referer" to referrer),
        )
    }
}
