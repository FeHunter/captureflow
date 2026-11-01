package com.fehunter.captureflow.features.fullscreen.domain

import java.net.URI

/** Converts common YouTube watch, short, Shorts, live and embed URLs to a validated video ID. */
object YoutubeVideoIdParser {
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val allowedHosts = setOf(
        "youtube.com", "www.youtube.com", "m.youtube.com", "music.youtube.com",
        "youtu.be", "www.youtube-nocookie.com",
    )

    fun extractVideoId(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host !in allowedHosts && host != "youtube-nocookie.com") return null
        val path = uri.path.orEmpty().trim('/')
        val candidate = when {
            host == "youtu.be" -> path.substringBefore('/')
            path.startsWith("watch") -> uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("v=") }?.substringAfter('=')
            path.startsWith("shorts/") -> path.removePrefix("shorts/").substringBefore('/')
            path.startsWith("live/") -> path.removePrefix("live/").substringBefore('/')
            path.startsWith("embed/") -> path.removePrefix("embed/").substringBefore('/')
            else -> null
        }?.let { runCatching { java.net.URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        return candidate?.takeIf(videoIdPattern::matches)
    }
}
