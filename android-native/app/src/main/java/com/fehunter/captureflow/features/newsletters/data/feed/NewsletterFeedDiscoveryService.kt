package com.fehunter.captureflow.features.newsletters.data.feed

import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL

/** Discovers public RSS/Atom endpoints from rel=alternate and common feed paths. */
class NewsletterFeedDiscoveryService {
    fun discoverFeedUrls(rawSiteUrl: String): List<String> {
        var current = URI(rawSiteUrl)
        repeat(MAX_REDIRECTS + 1) {
            validatePublicUrl(current)
            val connection = (URL(current.toString()).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", USER_AGENT)
                setRequestProperty("Accept", "text/html,application/xhtml+xml")
            }
            try {
                val status = connection.responseCode
                if (status in REDIRECT_CODES) {
                    val location = connection.getHeaderField("Location")
                        ?: throw IOException("Redirecionamento sem destino.")
                    current = current.resolve(location)
                    return@repeat
                }
                if (status !in 200..299) return emptyList()
                if (!connection.contentType.orEmpty().contains("html", ignoreCase = true)) return emptyList()
                val html = connection.inputStream.use { input ->
                    val output = StringBuilder()
                    val buffer = ByteArray(8_192)
                    var total = 0
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        if (total > MAX_HTML_BYTES) throw IOException("A página excede o limite de descoberta.")
                        output.append(String(buffer, 0, count, Charsets.UTF_8))
                    }
                    output.toString()
                }
                val base = current.toURL()
                val discovered = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE).findAll(html).mapNotNull { match ->
                    val tag = match.value
                    val rel = attribute(tag, "rel").orEmpty().lowercase()
                    val type = attribute(tag, "type").orEmpty().lowercase()
                    val href = attribute(tag, "href") ?: return@mapNotNull null
                    val feedType = type.contains("rss") || type.contains("atom") ||
                        type.contains("xml") || href.endsWith(".rss", true) ||
                        href.endsWith(".xml", true) || href.contains("/feed", true)
                    if (!rel.split(Regex("\\s+")).contains("alternate") || !feedType) return@mapNotNull null
                    resolve(base, href)
                }.toList()
                val host = current.host
                val commonPaths = listOf("/feed", "/feed.xml", "/rss.xml", "/atom.xml", "/rss", "/index.xml")
                    .mapNotNull { path -> resolve(base, path) }
                val substackFeed = if (host.endsWith(".substack.com", true)) {
                    listOfNotNull(resolve(base, "/feed"))
                } else emptyList()
                return (discovered + substackFeed + commonPaths).distinct().take(MAX_CANDIDATES)
            } finally {
                connection.disconnect()
            }
        }
        throw IOException("A página excedeu o limite de redirecionamentos.")
    }

    private fun validatePublicUrl(uri: URI) {
        require(uri.scheme?.lowercase() in setOf("http", "https")) { "Use um endereço HTTP ou HTTPS." }
        require(uri.userInfo.isNullOrBlank()) { "URLs com credenciais não são permitidas." }
        val host = uri.host?.trim('[', ']')?.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Endereço de publicação inválido.")
        require(uri.port == -1 || uri.port == 80 || uri.port == 443) { "A porta deste endereço não é permitida." }
        require(host != "localhost" && !host.endsWith(".local", true)) { "Endereços locais não são permitidos." }
        val addresses = InetAddress.getAllByName(host)
        require(addresses.isNotEmpty() && addresses.none {
            it.isAnyLocalAddress || it.isLoopbackAddress || it.isLinkLocalAddress ||
                it.isSiteLocalAddress || it.isMulticastAddress
        }) { "A publicação precisa estar hospedada em um endereço público." }
    }

    private fun attribute(tag: String, name: String): String? =
        Regex("""\b$name\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s>]+))""", RegexOption.IGNORE_CASE)
            .find(tag)?.let { it.groupValues.drop(1).firstOrNull(String::isNotEmpty) }

    private fun resolve(base: URL, href: String): String? =
        runCatching { URL(base, href).takeIf { it.protocol == "http" || it.protocol == "https" }?.toString() }.getOrNull()

    private companion object {
        const val TIMEOUT_MS = 6_000
        const val MAX_REDIRECTS = 5
        const val MAX_HTML_BYTES = 512 * 1024
        const val MAX_CANDIDATES = 8
        const val USER_AGENT = "CaptureFlow/1.0 (Android; RSS discovery)"
        val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)
    }
}
