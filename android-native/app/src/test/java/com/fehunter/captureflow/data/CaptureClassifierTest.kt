package com.fehunter.captureflow.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureClassifierTest {
    @Test fun identifiesUrlsAsLinks() = assertEquals("link", CaptureClassifier.classify("Veja https://example.com", null))
    @Test fun classifiesPlainText() = assertEquals("text", CaptureClassifier.classify("Uma ideia para depois", null))
    @Test fun imageUriTakesPrecedence() = assertEquals("image", CaptureClassifier.classify("Imagem", "content://images/1"))
    @Test fun trimsOuterWhitespace() = assertEquals("texto", CaptureClassifier.normalize(" texto "))
    
    @Test
    fun `extractsAndTrimsHttpSourceUrls`() {
        assertEquals(
            "https://example.com/article?id=1",
            SourceUrlDetector.extract("Read this https://example.com/article?id=1)."),
        )
    }

    @Test
    fun `rejectsNonHttpAndMalformedSourceUrls`() {
        assertEquals(null, SourceUrlDetector.extract("ftp://example.com/file"))
        assertEquals(null, SourceUrlDetector.extract("https://"))
    }
}
