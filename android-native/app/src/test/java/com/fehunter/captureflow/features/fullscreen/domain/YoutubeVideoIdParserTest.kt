package com.fehunter.captureflow.features.fullscreen.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class YoutubeVideoIdParserTest {
    private val id = "dQw4w9WgXcQ"

    @Test fun parsesWatchUrl() {
        assertEquals(id, YoutubeVideoIdParser.extractVideoId("https://www.youtube.com/watch?v=$id&t=20"))
    }

    @Test fun parsesShortUrl() {
        assertEquals(id, YoutubeVideoIdParser.extractVideoId("https://youtu.be/$id"))
    }

    @Test fun parsesShortsLiveAndEmbedUrls() {
        assertEquals(id, YoutubeVideoIdParser.extractVideoId("https://youtube.com/shorts/$id"))
        assertEquals(id, YoutubeVideoIdParser.extractVideoId("https://youtube.com/live/$id"))
        assertEquals(id, YoutubeVideoIdParser.extractVideoId("https://youtube.com/embed/$id"))
    }

    @Test fun rejectsUnknownHostsAndMalformedIds() {
        assertNull(YoutubeVideoIdParser.extractVideoId("https://example.com/watch?v=$id"))
        assertNull(YoutubeVideoIdParser.extractVideoId("https://youtube.com/watch?v=short"))
        assertNull(YoutubeVideoIdParser.extractVideoId("javascript:alert(1)"))
    }
}
