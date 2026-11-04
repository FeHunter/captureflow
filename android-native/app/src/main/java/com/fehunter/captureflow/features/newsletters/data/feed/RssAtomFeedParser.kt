package com.fehunter.captureflow.features.newsletters.data.feed

import android.text.Html
import android.util.Xml
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticleDraft
import com.fehunter.captureflow.features.newsletters.domain.model.ParsedNewsletterFeed
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TimeZone

/** Parses RSS 2.0 and Atom XML. Network fetching and response-size enforcement stay outside this parser. */
class RssAtomFeedParser {
    fun parse(xml: String, baseUrl: String? = null): ParsedNewsletterFeed {
        require(xml.isNotBlank()) { "Feed vazio" }
        require(xml.length <= MAX_XML_CHARS) { "Feed excede o limite de tamanho" }

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
        runCatching { parser.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL, false) }
        parser.setInput(StringReader(xml))

        var feedTitle: String? = null
        var feedDescription: String? = null
        var siteUrl: String? = null
        var current: ItemBuilder? = null
        val articles = mutableListOf<NewsletterArticleDraft>()
        var event = parser.eventType

        while (event != XmlPullParser.END_DOCUMENT && articles.size < MAX_ITEMS) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase(Locale.ROOT)
                    val namespace = parser.namespace.orEmpty()
                    if (name == "item" || name == "entry") {
                        current = ItemBuilder()
                    } else if (current == null) {
                        when (name) {
                            "title" -> if (feedTitle == null) feedTitle = readElementText(parser).cleanText()
                            "description", "subtitle" -> if (feedDescription == null) feedDescription = readElementText(parser).cleanText()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                val rel = parser.getAttributeValue(null, "rel")
                                if (!href.isNullOrBlank()) {
                                    if (rel.isNullOrBlank() || rel == "alternate") siteUrl = resolve(baseUrl, href)
                                    skipElement(parser)
                                } else {
                                    val text = readElementText(parser)
                                    if (siteUrl == null) siteUrl = resolve(baseUrl, text)
                                }
                            }
                        }
                    } else {
                        val item = current
                        when (name) {
                            "title" -> item.title = readElementText(parser).cleanText()
                            "link" -> {
                                val href = parser.getAttributeValue(null, "href")
                                val rel = parser.getAttributeValue(null, "rel")
                                if (!href.isNullOrBlank()) {
                                    if (rel.isNullOrBlank() || rel == "alternate") item.link = href
                                    skipElement(parser)
                                } else item.link = readElementText(parser).trim()
                            }
                            "guid", "id" -> item.id = readElementText(parser).trim()
                            "description", "summary" -> item.summary = readElementText(parser)
                            "content", "encoded" -> {
                                if (namespace.contains("media", ignoreCase = true)) {
                                    item.imageUrl = parser.getAttributeValue(null, "url") ?: item.imageUrl
                                    skipElement(parser)
                                } else {
                                    item.content = readElementText(parser).take(MAX_CONTENT_CHARS)
                                }
                            }
                            "pubdate", "published", "updated", "date" -> item.date = readElementText(parser).trim()
                            "enclosure" -> {
                                val type = parser.getAttributeValue(null, "type").orEmpty()
                                val url = parser.getAttributeValue(null, "url")
                                if (type.startsWith("image/", ignoreCase = true)) item.imageUrl = url ?: item.imageUrl
                                skipElement(parser)
                            }
                            "thumbnail" -> {
                                item.imageUrl = parser.getAttributeValue(null, "url") ?: item.imageUrl
                                skipElement(parser)
                            }
                            else -> Unit
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    val name = parser.name.lowercase(Locale.ROOT)
                    if (name == "item" || name == "entry") {
                        current?.build(baseUrl)?.let(articles::add)
                        current = null
                    }
                }
            }
            event = parser.next()
        }

        return ParsedNewsletterFeed(
            title = feedTitle?.takeIf(String::isNotBlank),
            description = feedDescription?.takeIf(String::isNotBlank),
            siteUrl = siteUrl?.let { resolve(baseUrl, it) },
            articles = articles.distinctBy { it.stableId }.take(MAX_ITEMS),
        )
    }

    private fun readElementText(parser: XmlPullParser): String {
        val depth = parser.depth
        val value = StringBuilder()
        while (true) {
            when (parser.next()) {
                XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                    val remaining = MAX_CONTENT_CHARS - value.length
                    if (remaining > 0) value.append(parser.text.take(remaining))
                }
                XmlPullParser.END_TAG -> if (parser.depth == depth) return value.toString()
                XmlPullParser.END_DOCUMENT -> return value.toString()
            }
        }
    }

    private fun skipElement(parser: XmlPullParser) {
        val depth = parser.depth
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.END_TAG && parser.depth == depth) return
        }
    }

    private fun ItemBuilder.build(baseUrl: String?): NewsletterArticleDraft? {
        val titleValue = title?.takeIf(String::isNotBlank) ?: return null
        val url = resolve(baseUrl, link) ?: return null
        val stable = id?.takeIf(String::isNotBlank) ?: url
        val contentValue = content?.takeIf(String::isNotBlank)
        val summaryRaw = summary?.takeIf(String::isNotBlank)
        val summaryValue = summaryRaw?.cleanText()
        val image = imageUrl ?: extractImage(contentValue) ?: extractImage(summaryRaw)
        return NewsletterArticleDraft(
            stableId = stable.take(MAX_ID_CHARS),
            title = titleValue.take(MAX_TITLE_CHARS),
            canonicalUrl = url,
            publishedAt = parseDate(date),
            summary = summaryValue?.take(MAX_SUMMARY_CHARS),
            contentHtml = contentValue?.take(MAX_CONTENT_CHARS),
            imageUrl = resolve(baseUrl, image),
        )
    }

    private fun extractImage(html: String?): String? {
        if (html.isNullOrBlank()) return null
        val tag = Regex("<img\\b[^>]*>", RegexOption.IGNORE_CASE).find(html)?.value ?: return null
        return Regex("""(?:src|data-src)\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            .find(tag)?.groupValues?.getOrNull(1)
    }

    private fun resolve(base: String?, candidate: String?): String? {
        if (candidate.isNullOrBlank()) return null
        return runCatching {
            val resolved = if (base.isNullOrBlank()) URL(candidate) else URL(URL(base), candidate)
            resolved.takeIf { it.protocol == "http" || it.protocol == "https" }?.toString()
        }.getOrNull()
    }

    private fun String.cleanText(): String =
        Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY).toString()
            .replace(Regex("\\s+"), " ").trim()

    private fun parseDate(raw: String?): Long? {
        if (raw.isNullOrBlank()) return null
        runCatching { return Instant.parse(raw).toEpochMilli() }
        runCatching { return OffsetDateTime.parse(raw).toInstant().toEpochMilli() }
        val patterns = listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm Z",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd",
        )
        for (pattern in patterns) {
            val parsed = runCatching {
                SimpleDateFormat(pattern, Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.parse(raw)
            }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return null
    }

    private class ItemBuilder {
        var title: String? = null
        var link: String? = null
        var id: String? = null
        var summary: String? = null
        var content: String? = null
        var date: String? = null
        var imageUrl: String? = null
    }

    private companion object {
        const val MAX_XML_CHARS = 1_000_000
        const val MAX_ITEMS = 100
        const val MAX_ID_CHARS = 2_000
        const val MAX_TITLE_CHARS = 300
        const val MAX_SUMMARY_CHARS = 5_000
        const val MAX_CONTENT_CHARS = 100_000
    }
}
