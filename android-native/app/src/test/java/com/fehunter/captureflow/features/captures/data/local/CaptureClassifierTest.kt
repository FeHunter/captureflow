package com.fehunter.captureflow.features.captures.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureClassifierTest {
    @Test fun identifiesUrlsAsLinks() = assertEquals("link", CaptureClassifier.classify("Veja https://example.com", null))
    @Test fun classifiesPlainText() = assertEquals("text", CaptureClassifier.classify("Uma ideia para depois", null))
    @Test fun imageUriTakesPrecedence() = assertEquals("image", CaptureClassifier.classify("Imagem", "content://images/1"))
    @Test fun trimsOuterWhitespace() = assertEquals("texto", CaptureClassifier.normalize(" texto "))
}
