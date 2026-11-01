package com.fehunter.captureflow.features.notes.domain.model

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.UUID

/** Versioned, pure-Kotlin serialization. Legacy plain-text notes are read as a single block. */
object NotesCodec {
    private const val PREFIX = "CAPTUREFLOW_NOTES_V1\n"

    fun decode(raw: String?): List<NoteBlock> {
        if (raw.isNullOrBlank()) return emptyList()
        if (!raw.startsWith(PREFIX)) {
            return listOf(NoteBlock(id = UUID.randomUUID().toString(), text = raw))
        }
        return raw.removePrefix(PREFIX).lineSequence().mapNotNull { line ->
            val fields = line.split('\t')
            if (fields.size != 4) return@mapNotNull null
            runCatching {
                NoteBlock(
                    id = fields[0],
                    createdAt = fields[1].toLong(),
                    text = decodeText(fields[2]),
                    imageUri = decodeText(fields[3]).takeIf(String::isNotBlank),
                )
            }.getOrNull()
        }.toList()
    }

    fun encode(notes: List<NoteBlock>): String? {
        val valid = notes.filter { it.text.isNotBlank() || !it.imageUri.isNullOrBlank() }
        if (valid.isEmpty()) return null
        return PREFIX + valid.joinToString("\n") { note ->
            listOf(
                note.id,
                note.createdAt.toString(),
                encodeText(note.text),
                encodeText(note.imageUri.orEmpty()),
            ).joinToString("\t")
        }
    }

    private fun encodeText(value: String): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray(StandardCharsets.UTF_8))

    private fun decodeText(value: String): String =
        String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
}
