package com.fehunter.captureflow.features.captures.domain.usecase

import com.fehunter.captureflow.features.captures.domain.model.Capture
import java.util.UUID

/** Pure capture classification and construction rules. */
object CaptureFactory {
    private val urlPattern = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)

    fun normalize(raw: String): String = raw.trim()

    fun create(content: String, imageUri: String? = null): Capture {
        val normalized = normalize(content)
        val kind = when {
            imageUri != null -> "image"
            urlPattern.containsMatchIn(normalized) -> "link"
            else -> "text"
        }
        val url = urlPattern.find(normalized)?.value
        val title = when {
            kind == "image" -> "Imagem compartilhada"
            normalized.isBlank() && url != null -> androidHost(url)
            else -> normalized.replace(urlPattern, "").trim().ifBlank { normalized }.take(72).ifBlank { "Nova referência" }
        }
        val value = normalized.lowercase()
        val category = when {
            listOf("importante", "urgente", "prazo", "prioridade").any(value::contains) -> "Importante"
            listOf("verificar", "conferir", "checar", "revisar").any(value::contains) -> "Verificar"
            kind == "link" -> "Ler depois"
            else -> "Salvar"
        }
        return Capture(
            id = UUID.randomUUID().toString(),
            title = title,
            content = normalized,
            kind = kind,
            category = category,
            sourceUrl = url,
            imageUri = imageUri,
            createdAt = System.currentTimeMillis(),
        )
    }

    private fun androidHost(url: String): String =
        runCatching { java.net.URI(url).host?.removePrefix("www.") }.getOrNull() ?: "Link salvo"
}
