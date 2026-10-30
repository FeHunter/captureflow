package com.fehunter.captureflow.features.captures.data.local

import android.content.Intent
import android.net.Uri
import android.os.Build
import java.util.UUID

data class IncomingCapture(val content: String, val imageUri: String? = null)

object CaptureClassifier {
    private val urlPattern = Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE)
    fun normalize(raw: String) = raw.trim()
    fun classify(content: String, imageUri: String?): String = when {
        imageUri != null -> "image"
        urlPattern.containsMatchIn(content) -> "link"
        else -> "text"
    }
    fun titleFor(content: String, kind: String): String {
        if (kind == "image") return "Imagem compartilhada"
        val url = urlPattern.find(content)?.value
        if (content.isBlank() && url != null) return Uri.parse(url).host?.removePrefix("www.") ?: "Link salvo"
        val clean = content.replace(urlPattern, "").trim()
        return (clean.ifBlank { content }).take(72).ifBlank { "Nova referência" }
    }
    fun categoryFor(content: String, kind: String): String {
        val value = content.lowercase()
        return when {
            listOf("importante", "urgente", "prazo", "prioridade").any(value::contains) -> "Importante"
            listOf("verificar", "conferir", "checar", "revisar").any(value::contains) -> "Verificar"
            kind == "link" -> "Ler depois"
            else -> "Salvar"
        }
    }
    fun fromIntent(intent: Intent): IncomingCapture? {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) return null
        val type = intent.type.orEmpty().lowercase()
        if (type.startsWith("image/")) {
            val uri = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
            return uri?.let { IncomingCapture("Imagem compartilhada", it.toString()) }
        }

        // Different apps (including video/social apps) use text/plain, text/html,
        // text/uri-list or another text/* MIME type for the same shared URL.
        if (!type.startsWith("text/") && type != "application/x-www-form-urlencoded") return null

        val sharedText = runCatching {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }.getOrNull()
        val clipText = intent.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { index ->
                val item = clip.getItemAt(index)
                item.text?.toString()?.takeIf(String::isNotBlank)
                    ?: item.uri?.toString()?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            }.joinToString("\n").takeIf(String::isNotBlank)
        }
        val dataUrl = intent.data?.takeIf {
            it.scheme.equals("http", true) || it.scheme.equals("https", true)
        }?.toString()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf(String::isNotBlank)

        val content = sequenceOf(sharedText, clipText, dataUrl, subject)
            .filterNotNull()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .joinToString("\n")
        return content.takeIf(String::isNotBlank)?.let(::IncomingCapture)
    }
    fun create(content: String, imageUri: String? = null): CaptureEntity {
        val normalized = normalize(content)
        val kind = classify(normalized, imageUri)
        val url = urlPattern.find(normalized)?.value
        return CaptureEntity(UUID.randomUUID().toString(), titleFor(normalized, kind), normalized, kind,
            categoryFor(normalized, kind), url, imageUri, System.currentTimeMillis())
    }
}
