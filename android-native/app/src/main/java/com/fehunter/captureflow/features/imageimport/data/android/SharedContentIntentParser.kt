package com.fehunter.captureflow.features.imageimport.data.android

import android.content.Intent
import android.net.Uri
import android.os.Build

data class IncomingSharedContent(
    val content: String,
    val imageUri: String? = null,
)

/**
 * Android Sharesheet adapter. It extracts platform payloads only; content classification,
 * validation and persistence remain in the Image Import and Captures feature boundaries.
 */
object SharedContentIntentParser {
    fun parse(intent: Intent): IncomingSharedContent? {
        if (intent.action != Intent.ACTION_SEND && intent.action != Intent.ACTION_SEND_MULTIPLE) {
            return null
        }

        val type = intent.type.orEmpty().lowercase()
        if (type.startsWith("image/")) {
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            } ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

            return uri?.let { IncomingSharedContent("Imagem compartilhada", it.toString()) }
        }

        // Apps may share the same URL as text/plain, text/html, text/uri-list or another text/* type.
        if (!type.startsWith("text/") && type != "application/x-www-form-urlencoded") {
            return null
        }

        val sharedText = runCatching {
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        }.getOrNull()
        val clipText = intent.clipData?.let { clip ->
            (0 until clip.itemCount).mapNotNull { index ->
                val item = clip.getItemAt(index)
                item.text?.toString()?.takeIf(String::isNotBlank)
                    ?: item.uri?.toString()?.takeIf {
                        it.startsWith("http://") || it.startsWith("https://")
                    }
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

        return content.takeIf(String::isNotBlank)?.let(::IncomingSharedContent)
    }
}
