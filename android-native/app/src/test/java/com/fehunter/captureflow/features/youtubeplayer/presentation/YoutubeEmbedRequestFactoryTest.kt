package com.fehunter.captureflow.features.youtubeplayer.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeEmbedRequestFactoryTest {
    @Test
    fun requestUsesDirectEmbedUrlAndExplicitApplicationReferer() {
        val request = YoutubeEmbedRequestFactory.create(
            videoId = "dQw4w9WgXcQ",
            applicationId = "com.fehunter.captureflow",
        )

        assertTrue(request.url.startsWith("https://www.youtube-nocookie.com/embed/dQw4w9WgXcQ?"))
        assertTrue(request.url.contains("enablejsapi=1"))
        assertTrue(request.url.contains("autoplay=0"))
        assertTrue(request.url.contains("controls=1"))
        assertTrue(request.url.contains("playsinline=1"))
        assertTrue(request.url.contains("origin=https%3A%2F%2Fcom.fehunter.captureflow"))
        assertEquals("https://com.fehunter.captureflow/", request.headers["Referer"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun requestRejectsInvalidVideoId() {
        YoutubeEmbedRequestFactory.create("invalid", "com.fehunter.captureflow")
    }

    @Test(expected = IllegalArgumentException::class)
    fun requestRejectsInvalidApplicationId() {
        YoutubeEmbedRequestFactory.create("dQw4w9WgXcQ", "not a package")
    }
}
