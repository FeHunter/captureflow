package com.fehunter.captureflow.features.notes.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotesCodecTest {
    @Test fun roundTripsTextAndImageBlocks() {
        val notes = listOf(
            NoteBlock(id = "one", text = "Ideia com \n quebra", imageUri = null, createdAt = 1L),
            NoteBlock(id = "two", text = "Referência", imageUri = "file:///private/image.jpg", createdAt = 2L),
        )
        assertEquals(notes, NotesCodec.decode(NotesCodec.encode(notes)))
    }

    @Test fun readsLegacyPlainTextAsSingleNote() {
        val decoded = NotesCodec.decode("Anotação antiga")
        assertEquals(1, decoded.size)
        assertEquals("Anotação antiga", decoded.single().text)
        assertNull(decoded.single().imageUri)
    }

    @Test fun emptyNoteListEncodesToNull() {
        assertNull(NotesCodec.encode(emptyList()))
    }
}
