package com.fehunter.captureflow.features.youtubeplayer.presentation

import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerHtmlTest {
    @Test
    fun createIncludesClientIdentityAndExpectedPlayerConfiguration() {
        val html = YoutubePlayerHtml.create(
            videoId = "dQw4w9WgXcQ",
            origin = "https://com.fehunter.captureflow",
        )

        assertTrue(html.contains("strict-origin-when-cross-origin"))
        assertTrue(html.contains("videoId:'dQw4w9WgXcQ'"))
        assertTrue(html.contains("origin:'https://com.fehunter.captureflow'"))
        assertTrue(html.contains("autoplay:0"))
        assertTrue(html.contains("controls:1"))
        assertTrue(html.contains("https://www.youtube.com/iframe_api"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun createRejectsInvalidVideoIdBeforeEmbeddingItInHtml() {
        YoutubePlayerHtml.create(
            videoId = "not-a-valid-id",
            origin = "https://com.fehunter.captureflow",
        )
    }
}
