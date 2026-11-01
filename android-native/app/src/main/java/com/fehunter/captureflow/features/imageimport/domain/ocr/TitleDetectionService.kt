package com.fehunter.captureflow.features.imageimport.domain.ocr

import kotlin.math.abs
import kotlin.math.ln

/**
 * Platform-independent OCR line geometry. Coordinates use the original image pixel space.
 * A confidence of 0 means the recognizer did not provide a confidence value.
 */
data class OcrTextLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val confidence: Float = 0f,
) {
    val width: Int get() = (right - left).coerceAtLeast(1)
    val height: Int get() = (bottom - top).coerceAtLeast(1)
    val area: Long get() = width.toLong() * height.toLong()
    val centerY: Float get() = (top + bottom) / 2f
}

data class OcrImageSize(val width: Int, val height: Int) {
    val safeWidth: Int get() = width.coerceAtLeast(1)
    val safeHeight: Int get() = height.coerceAtLeast(1)
}

data class OcrTitleDetection(
    val title: String,
    val description: String,
)

/**
 * Ranks recognized lines using relative font height, bounding-box area, width,
 * vertical position, UI family, text shape and ML Kit confidence.
 *
 * Bold weight is intentionally not inferred from OCR metadata: ML Kit does not expose
 * a reliable font-weight attribute. Height/area are visual-prominence proxies, not
 * a claim that a line is bold.
 */
class TitleDetectionService {

    fun detect(lines: List<OcrTextLine>, imageSize: OcrImageSize): OcrTitleDetection {
        if (lines.isEmpty()) return OcrTitleDetection("", "")

        val orderedLines = lines.mapNotNull { line ->
            val cleaned = sanitize(line.text)
            cleaned.takeIf { it.isNotBlank() && !isInterfaceNoise(it) }
                ?.let { line.copy(text = it) }
        }.sortedWith(compareBy<OcrTextLine> { it.top }.thenBy { it.left })

        if (orderedLines.isEmpty()) {
            return OcrTitleDetection("", lines.joinToString("\n") { it.text }.take(MAX_DESCRIPTION_LENGTH))
        }

        val imageWidth = imageSize.safeWidth
        val imageHeight = imageSize.safeHeight
        val medianHeight = orderedLines.map { it.height }.sorted()
            .let { it[it.size / 2] }.coerceAtLeast(1)
        val medianArea = orderedLines.map { it.area }.sorted()
            .let { it[it.size / 2] }.coerceAtLeast(1L)
        val profile = detectProfile(lines.map { it.text })

        val anchor = orderedLines.maxByOrNull { line ->
            score(line, imageWidth, imageHeight, medianHeight, medianArea, profile)
        } ?: return OcrTitleDetection("", orderedLines.joinToString("\n") { it.text })

        val titleLines = collectHeadlineLines(
            anchor = anchor,
            candidates = orderedLines,
            imageWidth = imageWidth,
            imageHeight = imageHeight,
            medianHeight = medianHeight,
        )
        val title = titleLines.joinToString(" ") { it.text }
            .replace(WHITESPACE, " ")
            .trim()
            .take(MAX_TITLE_LENGTH)
        val selectedTexts = titleLines.map { it.text }.toSet()
        val description = orderedLines.asSequence()
            .filterNot { it.text in selectedTexts }
            .joinToString("\n") { it.text }
            .take(MAX_DESCRIPTION_LENGTH)

        return OcrTitleDetection(title, description)
    }

    private fun score(
        line: OcrTextLine,
        imageWidth: Int,
        imageHeight: Int,
        medianHeight: Int,
        medianArea: Long,
        profile: UiProfile,
    ): Float {
        val heightRatio = (line.height.toFloat() / medianHeight).coerceIn(0.5f, 3f)
        val areaRatio = (line.area.toDouble() / medianArea).coerceIn(0.25, 8.0)
        val widthRatio = (line.width.toFloat() / imageWidth).coerceIn(0f, 1f)
        val centerY = (line.centerY / imageHeight).coerceIn(0f, 1f)
        val centerScore = (1f - abs(centerY - 0.5f) / 0.5f).coerceIn(0f, 1f)
        val confidenceScore = if (line.confidence > 0f) {
            (line.confidence.coerceIn(0f, 1f) - 0.5f) * 0.8f
        } else {
            0f // Confidence can be unavailable on older/unbundled ML Kit installations.
        }
        val lengthScore = when (line.text.length) {
            in 18..120 -> 0.35f
            in 10..160 -> 0.18f
            else -> -0.12f
        }
        val contentScore = if (looksLikeContentTitle(line.text)) 0.45f else 0f
        val topPenalty = (line.top.toFloat() / imageHeight) * 0.12f

        // Relative height and area reward visual prominence. Width is intentionally weaker,
        // so a long URL or a full-width navigation label cannot win on width alone.
        return ln(heightRatio.toDouble()).toFloat() * 1.45f +
            ln(areaRatio).toFloat() * 0.75f +
            widthRatio * 0.45f +
            centerScore * 0.95f +
            profile.positionScore(centerY) +
            confidenceScore +
            lengthScore +
            contentScore -
            topPenalty
    }

    private fun collectHeadlineLines(
        anchor: OcrTextLine,
        candidates: List<OcrTextLine>,
        imageWidth: Int,
        imageHeight: Int,
        medianHeight: Int,
    ): List<OcrTextLine> {
        val maxGap = maxOf(medianHeight * 2.4f, anchor.height * 1.8f)
        val horizontalTolerance = imageWidth * 0.18f
        val selected = mutableListOf(anchor)

        // Expand down and up from the strongest line. This handles headlines whose
        // second line is slightly smaller, without blindly absorbing nearby paragraphs.
        fun isHeadlineNeighbor(candidate: OcrTextLine, previous: OcrTextLine, gap: Int): Boolean {
            val similarHeight = candidate.height >= anchor.height * 0.58f &&
                candidate.height <= anchor.height * 1.75f
            val aligned = abs(candidate.left - anchor.left) <= horizontalTolerance ||
                (candidate.left < anchor.right && candidate.right > anchor.left)
            val plausibleText = candidate.text.length >= 3 &&
                !isInterfaceNoise(candidate.text) &&
                (looksLikeContentTitle(candidate.text) || candidate.height >= medianHeight * 1.2f)
            return gap >= 0 && gap.toFloat() <= maxGap && similarHeight && aligned && plausibleText
        }

        val below = candidates.filter { it.top >= anchor.bottom && it.top < imageHeight * 0.92f }
            .sortedBy { it.top }
        below.forEach { candidate ->
            val previous = selected.last()
            if (isHeadlineNeighbor(candidate, previous, candidate.top - previous.bottom)) {
                selected += candidate
            }
        }

        val above = candidates.filter { it.bottom <= anchor.top }
            .sortedByDescending { it.bottom }
        above.forEach { candidate ->
            val next = selected.first()
            if (isHeadlineNeighbor(candidate, next, next.top - candidate.bottom)) {
                selected.add(0, candidate)
            }
        }

        return selected.sortedWith(compareBy<OcrTextLine> { it.top }.thenBy { it.left })
    }

    private fun detectProfile(texts: List<String>): UiProfile {
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
        if (value.length !in 10..180 || isInterfaceNoise(value)) return false
        val words = value.split(WHITESPACE)
        val wordRatio = words.count { word -> word.any(Char::isLetter) }.toFloat() /
            words.size.coerceAtLeast(1)
        val sentenceLike = words.size >= 3 || value.contains(":") || value.contains("/")
        return wordRatio >= 0.55f && sentenceLike &&
            !value.lowercase().matches(Regex(".*\\b\\d+\\s*(likes?|views?|comments?)\\b.*"))
    }

    private fun sanitize(raw: String): String = raw.trim()
        .replace(Regex("(?i)\\bnot\\s+secure\\b"), " ")
        .replace(Regex("(?i)https?://\\S+"), " ")
        .replace(Regex("(?i)www\\.\\S+"), " ")
        .replace(WHITESPACE, " ")
        .trim()

    private fun isInterfaceNoise(text: String): Boolean {
        val normalized = text.trim().lowercase()
            .replace(Regex("[^a-z0-9@./ ]"), " ")
            .replace(WHITESPACE, " ").trim()
        val compact = normalized.replace(" ", "")
        val browserChrome = compact.contains("youtube.com/watch") ||
            compact.contains("m.youtube") ||
            normalized.contains("open app") ||
            normalized.contains("preferred on google") ||
            (normalized.matches(Regex(".*\\b\\d{1,2}:\\d{2}\\b.*")) && normalized.length < 32) ||
            normalized.startsWith("http") ||
            normalized.matches(Regex("[^ ]+\\.(com|org|net|co)(/.*)?", RegexOption.IGNORE_CASE))
        val socialMetadata = normalized.startsWith("@") ||
            Regex("""\b\d+(?:\.\d+)?\s*[km]?\s*(likes?|views?|comments?)\b""")
                .containsMatchIn(normalized) ||
            normalized.matches(Regex(".*\\b\\d+\\s*(followers?|following)\\b.*"))
        val navigationOnly = setOf(
            "share", "save", "subscribe", "home", "search", "menu", "more", "watch",
            "youtube", "open app", "comments", "shorts", "for you", "following",
            "add to cart", "buy now", "description", "see more",
        ).contains(normalized)

        return text.trim().length <= 2 ||
            text.trim().matches(Regex("\\d{1,2}:\\d{2}")) ||
            text.trim().matches(Regex("\\d{1,2}\\s+(minutes?|hours?|days?)\\s+ago", RegexOption.IGNORE_CASE)) ||
            browserChrome || socialMetadata || navigationOnly
    }

    private enum class UiProfile {
        VIDEO, SOCIAL, NEWS, SHOPPING, PRODUCTIVITY, GENERIC;

        fun positionScore(centerY: Float): Float = when (this) {
            VIDEO -> if (centerY in 0.20f..0.90f) 0.45f else -0.2f
            SOCIAL -> if (centerY in 0.16f..0.92f) 0.4f else -0.2f
            NEWS -> if (centerY in 0.12f..0.90f) 0.45f else -0.2f
            SHOPPING -> if (centerY in 0.12f..0.90f) 0.35f else -0.2f
            PRODUCTIVITY, GENERIC -> 0f
        }
    }

    private companion object {
        const val MAX_TITLE_LENGTH = 220
        const val MAX_DESCRIPTION_LENGTH = 3_000
        val WHITESPACE = Regex("\\s+")
    }
}
