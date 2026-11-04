package com.fehunter.captureflow.features.newsletters.data.feed

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RssAtomFeedParserTest {
    private val parser = RssAtomFeedParser()

    @Test
    fun parsesRssItemsAndResolvesRelativeLinksAndImages() {
        val xml = """
            <rss version="2.0">
              <channel>
                <title>Example publication</title>
                <link>https://example.com</link>
                <description>Public feed</description>
                <item>
                  <guid>entry-1</guid>
                  <title>First article</title>
                  <link>/articles/first</link>
                  <pubDate>Wed, 04 Nov 2026 10:00:00 GMT</pubDate>
                  <description><![CDATA[<p>Summary</p><img src="/images/cover.jpg"/>]]></description>
                </item>
              </channel>
            </rss>
        """.trimIndent()

        val result = parser.parse(xml, "https://example.com/feed.xml")
        assertEquals("Example publication", result.title)
        assertEquals("https://example.com", result.siteUrl)
        assertEquals(1, result.articles.size)
        assertEquals("https://example.com/articles/first", result.articles.single().canonicalUrl)
        assertEquals("https://example.com/images/cover.jpg", result.articles.single().imageUrl)
        assertEquals("Summary", result.articles.single().summary)
        assertNotNull(result.articles.single().publishedAt)
    }

    @Test
    fun parsesAtomAlternateLinksAndAllowsMissingDates() {
        val xml = """
            <feed xmlns="http://www.w3.org/2005/Atom">
              <title>Atom publication</title>
              <link rel="alternate" href="https://atom.example/"/>
              <entry>
                <id>tag:atom.example,2026:one</id>
                <title>Atom article</title>
                <link rel="alternate" href="/posts/one"/>
                <summary>Plain summary</summary>
              </entry>
            </feed>
        """.trimIndent()

        val result = parser.parse(xml, "https://atom.example/feed")
        assertEquals("Atom publication", result.title)
        assertEquals("https://atom.example/", result.siteUrl)
        assertEquals(1, result.articles.size)
        assertEquals("https://atom.example/posts/one", result.articles.single().canonicalUrl)
        assertTrue(result.articles.single().publishedAt == null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsOversizedFeedPayloads() {
        parser.parse("x".repeat(1_000_001))
    }
}
