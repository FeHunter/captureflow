package com.fehunter.captureflow.features.captures.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class CaptureFactoryTest {
    @Test
    fun createsLinkCaptureWithExpectedCategoryAndSourceUrl() {
        val capture = CaptureFactory.create("Read this https://example.com/article")
        assertEquals("link", capture.kind)
        assertEquals("Ler depois", capture.category)
        assertEquals("https://example.com/article", capture.sourceUrl)
    }

    @Test
    fun imageCaptureKeepsLocalUriAndImageKind() {
        val capture = CaptureFactory.create("Imagem compartilhada", "file:///private/image.jpg")
        assertEquals("image", capture.kind)
        assertEquals("file:///private/image.jpg", capture.imageUri)
        assertNotNull(capture.id)
    }

    @Test
    fun normalizesWhitespaceAndClassifiesPriorityText() {
        assertEquals("Importante", CaptureFactory.create("  prazo urgente  ").category)
        assertEquals("prazo urgente", CaptureFactory.normalize("  prazo urgente  "))
    }
}
