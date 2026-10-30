package com.fehunter.captureflow.features.imageimport.data.metadata

import android.net.Uri
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.Normalizer
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

            // Read a bounded portion of the response body so large pages cannot consume
            // unbounded memory. This is best-effort HTML parsing, not a browser renderer.
            val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val buffer = CharArray(8_192)
                val result = StringBuilder()
                while (result.length < MAX_HTML_CHARS) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_HTML_CHARS - result.length))
                    if (count <= 0) break
                    result.append(buffer, 0, count)
                }
                result.toString()
            }

            // Prefer the page's visible H1. Fall back to standard metadata and document title.
            val title = tag(html, "h1")
                ?: meta(html, "og:title")
                ?: meta(html, "twitter:title")
                ?: tag(html, "title")
                ?: host.removePrefix("www.")

            // Prefer a content image inside article/main over generic social preview metadata.
            val rawImage = imageInContent(html)
                ?: meta(html, "og:image")
                ?: meta(html, "og:image:url")
                ?: meta(html, "twitter:image")
                ?: meta(html, "twitter:image:src")
            val imageUrl = rawImage?.let { resolveHttpUrl(connection.url.toString(), it) }
            val description = meta(html, "og:description")
                ?: meta(html, "description")
                ?: meta(html, "twitter:description")

            PageMetadata(
                title = normalizeTitle(title, host),
                description = description?.takeIf(String::isNotBlank),
                imageUrl = imageUrl,
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun imageInContent(html: String): String? {
        val article = Regex(
            "<article\\b[^>]*>(.*?)</article>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.getOrNull(1)
        val main = Regex(
            "<main\\b[^>]*>(.*?)</main>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.getOrNull(1)
        val body = Regex(
            "<body\\b[^>]*>(.*?)</body>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.groupValues?.getOrNull(1) ?: html

        // Prefer semantic content containers, then inspect the body as fallback.
        // Rank images so a logo or tracking pixel does not win just because it appears first.
        return bestContentImage(article)
            ?: bestContentImage(main)
            ?: bestContentImage(body)
    }

    private data class ImageCandidate(val source: String, val score: Int)

    private fun bestContentImage(region: String?): String? {
        if (region.isNullOrBlank()) return null
        val candidates = mutableListOf<ImageCandidate>()
        for (match in Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(region)) {
            val element = match.value
            val srcset = attribute(element, "srcset")?.let(::bestSrcSetCandidate)
            val source = sequenceOf(
                attribute(element, "data-src"),
                attribute(element, "data-lazy-src"),
                attribute(element, "data-original"),
                attribute(element, "data-url"),
                attribute(element, "src"),
                srcset,
            ).filterNotNull().firstOrNull(String::isNotBlank) ?: continue
            val alt = attribute(element, "alt").orEmpty()
            val hint = listOf(alt, attribute(element, "class").orEmpty(),
                attribute(element, "id").orEmpty()).joinToString(" ").lowercase()
            val width = attribute(element, "width")?.filter(Char::isDigit)?.toIntOrNull() ?: 0
            val height = attribute(element, "height")?.filter(Char::isDigit)?.toIntOrNull() ?: 0
            if (source.startsWith("data:", ignoreCase = true) ||
                source.endsWith(".svg", ignoreCase = true) ||
                listOf("logo", "avatar", "icon", "sprite", "tracking", "pixel", "spinner")
                    .any(hint::contains)) continue

            var score = 0
            if (alt.isNotBlank()) score += 3
            if (listOf("hero", "featured", "cover", "thumbnail", "article", "content", "post")
                    .any(hint::contains)) score += 5
            if (listOf("logo", "avatar", "icon", "banner-ad", "advert")
                    .any(hint::contains)) score -= 8
            if (width >= 300) score += 4 else if (width in 1..120) score -= 4
            if (height >= 180) score += 4 else if (height in 1..80) score -= 4
            candidates += ImageCandidate(source, score)
        }
        return candidates.maxByOrNull { it.score }?.source
    }

    private fun bestSrcSetCandidate(srcset: String): String? =
        srcset.split(',').mapNotNull { entry ->
            val parts = entry.trim().split(Regex("\\s+"))
            val url = parts.firstOrNull()?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val descriptor = parts.getOrNull(1)?.filter(Char::isDigit)?.toIntOrNull() ?: 0
            url to descriptor
        }.maxByOrNull { it.second }?.first

    private fun resolveHttpUrl(base: String, candidate: String): String? =
        runCatching { URL(URL(base), candidate.trim()).toString() }.getOrNull()
            ?.takeIf { resolved ->
                runCatching {
                    val imageUri = Uri.parse(resolved)
                    imageUri.scheme in setOf("http", "https") && !imageUri.host.isNullOrBlank()
                }.getOrDefault(false)
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

    private fun normalizeTitle(raw: String, host: String): String {
        val decoded = clean(raw)
            .replace(Regex("[\\u200B-\\u200D\\uFEFF]"), "")
            .let { Normalizer.normalize(it, Normalizer.Form.NFC) }
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim { it in " |-–—:·\"'“”" }

        // Social platforms often append branding or long context to the page title.
        val withoutPlatformSuffix = decoded
            .replace(Regex("(?i)\\s*[|·–—-]\\s*(instagram|youtube|facebook|tiktok)\\s*$"), "")
            .trim()
        val words = withoutPlatformSuffix.split(Regex("\\s+")).filter(String::isNotBlank)
        val boundedWords = words.take(MAX_TITLE_WORDS).joinToString(" ")
        val boundedChars = if (boundedWords.length > MAX_TITLE_CHARS) {
            boundedWords.take(MAX_TITLE_CHARS).substringBeforeLast(' ').ifBlank {
                boundedWords.take(MAX_TITLE_CHARS)
            }
        } else boundedWords
        return boundedChars.trim().ifBlank { host.removePrefix("www.") }
    }

    private fun clean(value: String): String =
        Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("[\\u0000-\\u001F\\u007F]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    private companion object {
        const val MAX_HTML_CHARS = 512_000
        const val MAX_TITLE_WORDS = 12
        const val MAX_TITLE_CHARS = 110
    }
}
