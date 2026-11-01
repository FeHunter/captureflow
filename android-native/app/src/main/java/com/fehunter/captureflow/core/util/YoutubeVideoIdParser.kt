package com.fehunter.captureflow.core.util

import java.net.URI
import java.net.URLDecoder

/** Shared URL parser for YouTube links used by import and fullscreen playback. */
object YoutubeVideoIdParser {
    private val videoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val allowedHosts = setOf(
        "youtube.com", "m.youtube.com", "music.youtube.com",
        "youtu.be", "youtube-nocookie.com",
    )

    fun extractVideoId(rawUrl: String?): String? {
        if (rawUrl.isNullOrBlank()) return null
        val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() !in setOf("http", "https")) return null
        val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
        if (host !in allowedHosts) return null
        val path = uri.path.orEmpty().trim('/')
        val candidate = when {
            host == "youtu.be" -> path.substringBefore('/')
            path == "watch" -> uri.rawQuery.orEmpty().split('&')
                .firstOrNull { it.startsWith("v=") }?.substringAfter('=')
            path.startsWith("shorts/") -> path.removePrefix("shorts/").substringBefore('/')
            path.startsWith("live/") -> path.removePrefix("live/").substringBefore('/')
            path.startsWith("embed/") -> path.removePrefix("embed/").substringBefore('/')
            else -> null
        }?.let { runCatching { URLDecoder.decode(it, "UTF-8") }.getOrDefault(it) }
        return candidate?.takeIf(videoIdPattern::matches)
    }
}
