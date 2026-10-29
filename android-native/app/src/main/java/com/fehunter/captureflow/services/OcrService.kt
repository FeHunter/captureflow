package com.fehunter.captureflow.services

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class OcrTextResult(
    val fullText: String,
    val title: String,
    val description: String,
)

class OcrService {
    suspend fun extractText(context: Context, uri: Uri): String =
        recognize(context, uri).text

    suspend fun extractStructuredText(context: Context, uri: Uri): OcrTextResult {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizeImage(image)
        val lines = result.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val box = line.boundingBox ?: return@mapNotNull null
                val text = line.text.trim()
                if (text.isBlank()) return@mapNotNull null
                OcrLine(text, box)
            }
        }.sortedWith(compareBy<OcrLine> { it.bounds.top }.thenBy { it.bounds.left })

        if (lines.isEmpty()) return OcrTextResult(result.text, "", result.text)

        val imageHeight = image.height.coerceAtLeast(1)
        val imageWidth = image.width.coerceAtLeast(1)
        val candidateLines = lines.filterNot { isInterfaceNoise(it.text) }
        if (candidateLines.isEmpty()) {
            return OcrTextResult(result.text, "", lines.joinToString("\n") { it.text })
        }

        // ML Kit does not expose font weight/bold reliably. Approximate visual emphasis
        // using line height, width, position and the amount of text in the bounding box.
        val medianHeight = candidateLines.map { it.bounds.height().coerceAtLeast(1) }
            .sorted().let { it[it.size / 2] }.coerceAtLeast(1)
        val titleCandidates = candidateLines.filter { line ->
            line.bounds.top < imageHeight * 0.78 &&
                (line.bounds.height() >= medianHeight * 1.18 ||
                    line.bounds.width() >= imageWidth * 0.45)
        }
        val titlePool = if (titleCandidates.isNotEmpty()) titleCandidates else candidateLines.take(3)
        val titleAnchor = titlePool.maxByOrNull { line ->
            val heightScore = line.bounds.height().toFloat() / imageHeight
            val widthScore = line.bounds.width().toFloat() / imageWidth
            val topPenalty = line.bounds.top.toFloat() / imageHeight
            heightScore * 4f + widthScore * 0.8f - topPenalty * 0.7f +
                if (line.text.length in 12..110) 0.12f else 0f
        } ?: candidateLines.first()

        // Headline text is commonly split into multiple adjacent lines with similar
        // visual size. Collect those nearby, then keep all remaining recognized text
        // as a description so the OCR does not discard useful details.
        val titleLines = titlePool.filter { line ->
            line.bounds.top <= titleAnchor.bounds.bottom + medianHeight * 2 &&
                line.bounds.bottom >= titleAnchor.bounds.top - medianHeight &&
                line.bounds.height() >= titleAnchor.bounds.height() * 0.68
        }.sortedWith(compareBy<OcrLine> { it.bounds.top }.thenBy { it.bounds.left })
            .ifEmpty { listOf(titleAnchor) }
        val titleText = titleLines.joinToString(" ") { it.text }.take(180)
        val titleSet = titleLines.map { it.text }.toSet()
        val descriptionText = candidateLines.filterNot { it.text in titleSet }
            .joinToString("\n") { it.text }.take(3_000)

        return OcrTextResult(result.text, titleText, descriptionText)
    }

    private data class OcrLine(val text: String, val bounds: Rect)

    private fun isInterfaceNoise(text: String): Boolean {
        val value = text.trim()
        return value.length <= 2 ||
            value.matches(Regex("\\d{1,2}:\\d{2}")) ||
            value.matches(Regex("\\d{1,2}\\s+(minutes?|hours?|days?)\\s+ago", RegexOption.IGNORE_CASE)) ||
            value.startsWith("http", ignoreCase = true) ||
            value.matches(Regex("[^ ]+\\.(com|org|net|co)(/.*)?", RegexOption.IGNORE_CASE)) ||
            listOf("share", "save", "subscribe", "bbc", "home", "search", "menu")
                .any { value.equals(it, ignoreCase = true) }
    }

    private suspend fun recognize(context: Context, uri: Uri): Text {
        return recognizeImage(InputImage.fromFilePath(context, uri))
    }

    private suspend fun recognizeImage(image: InputImage): Text =
        suspendCancellableCoroutine { continuation ->
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            try {
                recognizer.process(image)
                    .addOnSuccessListener { result ->
                        if (continuation.isActive) continuation.resume(result)
                    }
                    .addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
                    .addOnCompleteListener { recognizer.close() }
            } catch (error: Exception) {
                recognizer.close()
                if (continuation.isActive) continuation.resumeWithException(error)
            }
        }
}
