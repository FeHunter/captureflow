package com.fehunter.captureflow.services

import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class PageMetadata(val title: String, val description: String?, val imageUrl: String?)

class LinkMetadataService {
    suspend fun fetch(rawUrl: String): PageMetadata? = withContext(Dispatchers.IO) {
        val uri = runCatching { Uri.parse(rawUrl) }.getOrNull() ?: return@withContext null
        val scheme = uri.scheme?.lowercase(Locale.ROOT)
        val host = uri.host?.lowercase(Locale.ROOT) ?: return@withContext null
        if (scheme !in setOf("http", "https") || !host.contains('.') ||
            host == "localhost" || host.endsWith(".local") ||
            host.matches(Regex("\\d{1,3}(\\.\\d{1,3}){3}"))) {
            return@withContext null
        }

        val connection = runCatching { URL(rawUrl).openConnection() as? HttpURLConnection }.getOrNull()
            ?: return@withContext null
        try {
            connection.connectTimeout = 4_000
            connection.readTimeout = 4_000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "CaptureFlow/0.3 (Android)")
            connection.setRequestProperty("Accept", "text/html,application/xhtml+xml")
            connection.connect()
            if (connection.responseCode !in 200..299 ||
                !connection.contentType.orEmpty().contains("html", ignoreCase = true)) {
                return@withContext null
            }
            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(8_192)
                val result = StringBuilder()
                while (result.length < 512_000) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, 512_000 - result.length))
                    if (count <= 0) break
                    result.append(buffer, 0, count)
                }
                result.toString()
            }
            val title = meta(html, "og:title")
                ?: meta(html, "twitter:title")
                ?: tag(html, "title")
            val description = meta(html, "og:description")
                ?: meta(html, "description")
                ?: meta(html, "twitter:description")
            val rawImage = meta(html, "og:image")
                ?: meta(html, "twitter:image")
                ?: meta(html, "twitter:image:src")
            val imageUrl = rawImage?.let { runCatching { URL(URL(connection.url.toString()), it).toString() }.getOrNull() }
                ?.takeIf { candidate ->
                    runCatching {
                        val imageUri = Uri.parse(candidate)
                        imageUri.scheme in setOf("http", "https") && !imageUri.host.isNullOrBlank()
                    }.getOrDefault(false)
                }
            title?.takeIf(String::isNotBlank)?.let { PageMetadata(it, description, imageUrl) }
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun meta(html: String, key: String): String? {
        val tags = Regex("<meta\\b[^>]*>", RegexOption.IGNORE_CASE)
        for (tag in tags.findAll(html)) {
            val element = tag.value
            val property = attribute(element, "property") ?: attribute(element, "name")
            if (property.equals(key, ignoreCase = true)) {
                return attribute(element, "content")?.let(::clean)
            }
        }
        return null
    }

    private fun tag(html: String, name: String): String? =
        Regex("<$name\\b[^>]*>(.*?)</$name>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .find(html)?.groupValues?.getOrNull(1)?.let(::clean)

    private fun attribute(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
            .find(tag)?.let { match ->
                match.groupValues.drop(1).firstOrNull(String::isNotEmpty)
            }

    private fun clean(value: String): String = value
        .replace(Regex("<[^>]+>"), " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace(Regex("\\s+"), " ")
        .trim()
}
