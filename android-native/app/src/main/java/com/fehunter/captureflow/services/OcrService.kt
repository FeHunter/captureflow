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
        // Remove browser chrome and URL fragments before ranking title candidates.
        // OCR can merge "Not secure", the address bar URL and page title into one line.
        val candidateLines = lines.mapNotNull { line ->
            val cleaned = sanitizeOcrLine(line.text)
            cleaned.takeIf { it.isNotBlank() && !isInterfaceNoise(it) }
                ?.let { line.copy(text = it) }
        }
        if (candidateLines.isEmpty()) {
            return OcrTextResult(result.text, "", lines.joinToString("\n") { it.text })
        }

        // ML Kit does not expose font weight/bold reliably. Approximate visual emphasis
        // using line height, width, position and the amount of text in the bounding box.
        val medianHeight = candidateLines.map { it.bounds.height().coerceAtLeast(1) }
            .sorted().let { it[it.size / 2] }.coerceAtLeast(1)
        val titleCandidates = candidateLines.filter { line ->
            line.bounds.top < imageHeight * 0.90 &&
                (line.bounds.height() >= medianHeight * 1.18 ||
                    line.bounds.width() >= imageWidth * 0.45)
        }
        val uiProfile = detectUiProfile(lines.map { it.text })
        val titlePool = if (titleCandidates.isNotEmpty()) titleCandidates else candidateLines.take(3)
        val titleAnchor = titlePool.maxByOrNull { line ->
            val heightScore = line.bounds.height().toFloat() / imageHeight
            val widthScore = line.bounds.width().toFloat() / imageWidth
            val y = line.bounds.centerY().toFloat() / imageHeight
            val topPenalty = line.bounds.top.toFloat() / imageHeight
            // Users often scroll important content into the center before taking a screenshot.
            val centerScore = (1f - (kotlin.math.abs(y - 0.50f) / 0.50f)).coerceIn(0f, 1f)
            val lengthScore = when (line.text.length) {
                in 18..120 -> 0.35f
                in 10..160 -> 0.18f
                else -> -0.12f
            }
            val contentScore = if (looksLikeContentTitle(line.text)) 0.45f else 0f
            val profilePositionScore = when (uiProfile) {
                UiProfile.VIDEO -> if (y in 0.20f..0.88f) 0.55f else -0.25f
                UiProfile.SOCIAL -> if (y in 0.16f..0.90f) 0.45f else -0.25f
                UiProfile.NEWS -> if (y in 0.12f..0.88f) 0.55f else -0.25f
                UiProfile.SHOPPING -> if (y in 0.12f..0.88f) 0.4f else -0.2f
                UiProfile.PRODUCTIVITY, UiProfile.GENERIC -> 0f
            }
            heightScore * 4f + widthScore * 0.65f - topPenalty * 0.12f +
                centerScore * 0.95f + lengthScore + contentScore + profilePositionScore
        } ?: candidateLines.first()

        // Headline text is commonly split into multiple adjacent lines with similar
        // visual size. Collect those nearby, then keep all remaining recognized text
        // as a description so the OCR does not discard useful details.
        // Do not limit the headline to titlePool: that pool is only for finding
        // the anchor. The remaining headline lines may be slightly narrower (for
        // example, "to soaring fuel costs") and would otherwise be dropped.
        val anchorHeight = titleAnchor.bounds.height().coerceAtLeast(1)
        val headlineWindow = maxOf(anchorHeight * 5, medianHeight * 8)
        val maxTitleTop = when (uiProfile) {
            UiProfile.VIDEO, UiProfile.SOCIAL -> imageHeight * 0.90
            UiProfile.NEWS, UiProfile.SHOPPING -> imageHeight * 0.90
            UiProfile.PRODUCTIVITY, UiProfile.GENERIC -> imageHeight * 0.88
        }
        val titleLines = candidateLines.filter { line ->
            val verticalOffset = line.bounds.top - titleAnchor.bounds.top
            verticalOffset >= -medianHeight &&
                verticalOffset <= headlineWindow &&
                line.bounds.height() >= anchorHeight * 0.62 &&
                line.bounds.top < maxTitleTop
        }.sortedWith(compareBy<OcrLine> { it.bounds.top }.thenBy { it.bounds.left })
            .ifEmpty { listOf(titleAnchor) }
        val titleText = titleLines.joinToString(" ") { it.text }
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(220)
        val titleSet = titleLines.map { it.text }.toSet()
        val descriptionText = candidateLines.filterNot { it.text in titleSet }
            .joinToString("\n") { it.text }.take(3_000)

        return OcrTextResult(result.text, titleText, descriptionText)
    }

    private data class OcrLine(val text: String, val bounds: Rect)

    private enum class UiProfile { VIDEO, SOCIAL, NEWS, SHOPPING, PRODUCTIVITY, GENERIC }

    private fun detectUiProfile(texts: List<String>): UiProfile {
        val all = texts.joinToString(" ").lowercase()
        return when {
            listOf("youtube.com", "tiktok", "reels", "shorts", "subscribe", "views").any(all::contains) ->
                UiProfile.VIDEO
            listOf("instagram", "facebook", "threads", "pinterest", "snapchat", "follow").any(all::contains) ->
                UiProfile.SOCIAL
            listOf("bbc", "news", "published", "article", "by ").any(all::contains) ->
                UiProfile.NEWS
            listOf("add to cart", "shopping", "amazon", "temu", "price", "reviews", "buy now").any(all::contains) ->
                UiProfile.SHOPPING
            listOf("gmail", "outlook", "google docs", "notion", "calendar", "chatgpt").any(all::contains) ->
                UiProfile.PRODUCTIVITY
            else -> UiProfile.GENERIC
        }
    }

    private fun looksLikeContentTitle(text: String): Boolean {
        val value = text.trim()
        val normalized = value.lowercase()
        if (value.length < 10 || value.length > 180) return false
        if (isInterfaceNoise(value)) return false
        val words = value.split(Regex("\\s+"))
        val wordRatio = words.count { it.any(Char::isLetter) }.toFloat() / words.size.coerceAtLeast(1)
        val hasSentenceLikeContent = value.any(Char::isLetter) &&
            (words.size >= 3 || value.contains(":") || value.contains("/"))
        return wordRatio >= 0.55f && hasSentenceLikeContent &&
            !normalized.matches(Regex(".*\\b\\d+\\s*(likes?|views?|comments?)\\b.*"))
    }

    private fun sanitizeOcrLine(raw: String): String {
        return raw.trim()
            .replace(Regex("(?i)\\bnot\\s+secure\\b"), " ")
            .replace(Regex("(?i)\\bhttps?://\\S+"), " ")
            .replace(Regex("(?i)\\bwww\\.\\S+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun isInterfaceNoise(text: String): Boolean {
        val value = text.trim()
        val normalized = value.lowercase().replace(Regex("[^a-z0-9@./ ]"), " ")
            .replace(Regex("\\s+"), " ").trim()
        val compact = normalized.replace(" ", "")

        // The OCR engine may merge the browser URL, page controls and metadata
        // into a single line. Reject those lines, but don't reject a headline just
        // because it contains a generic word such as "views" or "share".
        val browserChrome = compact.contains("youtube.com/watch") ||
            compact.contains("m.youtube") ||
            normalized.contains("open app") ||
            normalized.contains("preferred on google") ||
            normalized.matches(Regex(".*\\b\\d{1,2}:\\d{2}\\b.*")) && normalized.length < 32 ||
            normalized.startsWith("http") ||
            normalized.matches(Regex("[^ ]+\\.(com|org|net|co)(/.*)?", RegexOption.IGNORE_CASE))
        val socialMetadata = normalized.startsWith("@") ||
            Regex("""\b\d+(?:\.\d+)?\s*[km]?\s*(likes?|views?|comments?)\b""").containsMatchIn(normalized) ||
            normalized.matches(Regex(".*\\b\\d+\\s*(followers?|following)\\b.*"))
        val navigationOnly = listOf(
            "share", "save", "subscribe", "home", "search", "menu", "more", "watch",
            "youtube", "open app", "comments", "shorts", "for you", "following",
            "add to cart", "buy now", "description", "see more",
        ).any { normalized == it }

        return value.length <= 2 ||
            value.matches(Regex("\\d{1,2}:\\d{2}")) ||
            value.matches(Regex("\\d{1,2}\\s+(minutes?|hours?|days?)\\s+ago", RegexOption.IGNORE_CASE)) ||
            browserChrome || socialMetadata || navigationOnly
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
