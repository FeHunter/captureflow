package com.fehunter.captureflow.features.captures.data.mapper

import com.fehunter.captureflow.features.captures.data.local.CaptureEntity
import com.fehunter.captureflow.features.captures.domain.model.Capture
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureMapperTest {
    @Test
    fun captureRoundTripPreservesAllPersistedFields() {
        val original = Capture(
            id = "capture-1",
            title = "Reference",
            content = "https://example.com",
            kind = "link",
            category = "Ler depois",
            sourceUrl = "https://example.com",
            imageUri = null,
            createdAt = 123L,
            isCompleted = true,
            previousCategory = "Salvar",
            notes = "Review this",
        )

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun entityMappingPreservesNullableAndCompletionFields() {
        val entity = CaptureEntity(
            id = "capture-2",
            title = "Image",
            content = "",
            kind = "image",
            category = "Concluído",
            sourceUrl = null,
            imageUri = "file:///private/image.jpg",
            createdAt = 456L,
            isCompleted = true,
            previousCategory = "Importante",
            notes = null,
        )

        val mapped = entity.toDomain()
        assertEquals(entity.id, mapped.id)
        assertEquals(entity.imageUri, mapped.imageUri)
        assertEquals(entity.previousCategory, mapped.previousCategory)
        assertEquals(entity.notes, mapped.notes)
        assertEquals(entity.isCompleted, mapped.isCompleted)
    }
}
