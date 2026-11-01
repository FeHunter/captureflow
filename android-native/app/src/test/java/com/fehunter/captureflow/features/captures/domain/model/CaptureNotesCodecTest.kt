package com.fehunter.captureflow.features.captures.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CaptureNotesCodecTest {
    @Test fun roundTripsTextAndImageBlocks() {
        val notes = listOf(
            CaptureNote(id = "one", text = "Ideia com \n quebra", imageUri = null, createdAt = 1L),
            CaptureNote(id = "two", text = "Referência", imageUri = "file:///private/image.jpg", createdAt = 2L),
        )
        assertEquals(notes, CaptureNotesCodec.decode(CaptureNotesCodec.encode(notes)))
    }

    @Test fun readsLegacyPlainTextAsSingleNote() {
        val decoded = CaptureNotesCodec.decode("Anotação antiga")
        assertEquals(1, decoded.size)
        assertEquals("Anotação antiga", decoded.single().text)
        assertNull(decoded.single().imageUri)
    }

    @Test fun emptyNoteListEncodesToNull() {
        assertNull(CaptureNotesCodec.encode(emptyList()))
    }
}
