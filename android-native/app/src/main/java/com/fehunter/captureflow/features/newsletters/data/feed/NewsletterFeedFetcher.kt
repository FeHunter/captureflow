package com.fehunter.captureflow.features.newsletters.data.feed

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import java.nio.charset.Charset

sealed interface NewsletterFetchResult {
    data class Success(
        val xml: String,
        val finalUrl: String,
        val etag: String?,
        val lastModified: String?,
    ) : NewsletterFetchResult

    data object NotModified : NewsletterFetchResult
}

/** Bounded HTTP fetcher for user-supplied public RSS/Atom URLs. */
class NewsletterFeedFetcher {
    fun fetch(feedUrl: String, etag: String? = null, lastModified: String? = null): NewsletterFetchResult {
        var current = URI(feedUrl)
        repeat(MAX_REDIRECTS + 1) { redirectCount ->
            validatePublicUrl(current)
            val connection = (URL(current.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                requestMethod = "GET"
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "application/atom+xml, application/rss+xml, application/rdf+xml, application/xml, text/xml, text/plain;q=0.9")
                if (redirectCount == 0) {
                    etag?.let { setRequestProperty("If-None-Match", it) }
                    lastModified?.let { setRequestProperty("If-Modified-Since", it) }
                }
            }
            try {
                val status = connection.responseCode
                if (status == HttpURLConnection.HTTP_NOT_MODIFIED) return NewsletterFetchResult.NotModified
                if (status in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirecionamento sem destino.")
                    current = current.resolve(location)
                    return@repeat
                }
                if (status !in 200..299) throw IOException("O servidor respondeu HTTP $status.")
                val contentType = connection.contentType.orEmpty().substringBefore(";").trim().lowercase()
                if (contentType == "text/html" || contentType.contains("html")) {
                    throw IOException("O endereço retornou HTML, não um feed RSS/Atom.")
                }
                val allowedType = contentType.isBlank() || contentType.contains("xml") ||
                    contentType.contains("rss") || contentType.contains("atom") ||
                    contentType == "text/plain" || contentType == "application/octet-stream"
                if (!allowedType) throw IOException("Tipo de conteúdo não suportado: $contentType.")
                val declaredLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L
                if (declaredLength > MAX_RESPONSE_BYTES) throw IOException("O feed excede o limite de 1 MB.")
                val bytes = connection.inputStream.use { input ->
                    val output = ByteArrayOutputStream()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_RESPONSE_BYTES) throw IOException("O feed excede o limite de 1 MB.")
                        output.write(buffer, 0, count)
                    }
                    output.toByteArray()
                }
                val charsetName = connection.contentType.orEmpty().substringAfter("charset=", "")
                    .substringBefore(";").trim().trim('"', '\'')
                val charset = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
                return NewsletterFetchResult.Success(
                    xml = bytes.toString(charset),
                    finalUrl = current.toString(),
                    etag = connection.getHeaderField("ETag"),
                    lastModified = connection.getHeaderField("Last-Modified"),
                )
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("O feed excedeu o limite de redirecionamentos.")
    }

    private fun validatePublicUrl(uri: URI) {
        require(uri.scheme?.lowercase() in setOf("http", "https")) { "Use um endereço HTTP ou HTTPS." }
        require(uri.userInfo.isNullOrBlank()) { "URLs com credenciais não são permitidas." }
        val host = uri.host?.trim('[', ']')?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Endereço de feed inválido.")
        require(uri.port == -1 || uri.port == 80 || uri.port == 443) {
            "A porta deste endereço não é permitida."
        }
        require(host != "localhost" && !host.endsWith(".local", ignoreCase = true)) {
            "Endereços locais não são permitidos."
        }
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.none {
            it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
                it.isSiteLocalAddress || it.isMulticastAddress
        }) { "O feed precisa estar hospedado em um endereço público." }
    }

    private companion object {
        const val TIMEOUT_MS = 6_000
        const val MAX_REDIRECTS = 5
        const val MAX_RESPONSE_BYTES = 1_048_576
        const val USER_AGENT = "CaptureFlow/1.0 (Android; RSS reader)"
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
