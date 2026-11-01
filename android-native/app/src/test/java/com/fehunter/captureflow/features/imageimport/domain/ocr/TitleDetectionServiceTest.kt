package com.fehunter.captureflow.features.imageimport.domain.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TitleDetectionServiceTest {

    private val detector = TitleDetectionService()
    private val image = OcrImageSize(width = 1080, height = 1920)

    @Test
    fun prefersVisuallyProminentHeadlineOverNavigationAndBodyText() {
        val result = detector.detect(
            lines = listOf(
                line("Home", 32, 90, 90, 112),
                line("Como a inteligência artificial está transformando o desenvolvimento", 90, 430, 850, 480),
                line("O avanço das ferramentas está mudando a rotina de equipes.", 80, 560, 980, 582),
                line("Profissionais estão adotando novos fluxos de trabalho.", 80, 590, 960, 612),
            ),
            imageSize = image,
        )

        assertEquals(
            "Como a inteligência artificial está transformando o desenvolvimento",
            result.title,
        )
        assertTrue(result.description.contains("O avanço das ferramentas"))
    }

    @Test
    fun joinsAdjacentHeadlineLinesAndKeepsFollowingParagraphAsDescription() {
        val result = detector.detect(
            lines = listOf(
                line("Mapped: Unemployment Rates", 80, 350, 850, 398),
                line("Around the World in 2026", 80, 405, 780, 449),
                line("The global labor market continues to change.", 80, 520, 980, 542),
                line("Several countries reported different trends.", 80, 550, 970, 572),
            ),
            imageSize = image,
        )

        assertEquals(
            "Mapped: Unemployment Rates Around the World in 2026",
            result.title,
        )
        assertTrue(result.description.contains("global labor market"))
    }

    @Test
    fun ignoresBrowserChromeAndSocialEngagementMetadata() {
        val result = detector.detect(
            lines = listOf(
                line("https://m.youtube.com/watch?v=abc", 30, 30, 1050, 55),
                line("Open App", 70, 80, 250, 105),
                line("10 Easy craft ideas for your weekend", 90, 900, 970, 950),
                line("12K views", 100, 1040, 260, 1062),
                line("Subscribe", 820, 1100, 1030, 1120),
            ),
            imageSize = image,
        )

        assertEquals("10 Easy craft ideas for your weekend", result.title)
        assertFalseContains(result.title, "Open App")
        assertFalseContains(result.title, "views")
    }


    @Test
    fun considersRelativeTextAreaWhenFontHeightsAreSimilar() {
        val result = detector.detect(
            lines = listOf(
                line("A broad headline occupying more of the image", 80, 450, 980, 478),
                line("Small secondary label", 80, 560, 360, 588),
            ),
            imageSize = image,
        )

        assertTrue(result.title.contains("broad headline"))
    }

    @Test
    fun doesNotDiscardARealHeadlineThatContainsTheWordShare() {
        val result = detector.detect(
            lines = listOf(
                line("How creators share ideas with the world", 80, 450, 950, 500),
                line("A short introduction to the topic.", 80, 570, 900, 592),
            ),
            imageSize = image,
        )

        assertTrue(result.title.contains("share"))
    }

    @Test
    fun returnsEmptyResultWhenNoLinesAreRecognized() {
        assertEquals(OcrTitleDetection("", ""), detector.detect(emptyList(), image))
    }

    @Test
    fun toleratesMissingConfidenceValues() {
        val result = detector.detect(
            lines = listOf(
                line("A visually prominent title with missing confidence", 80, 450, 970, 500),
                line("Small supporting copy.", 80, 560, 500, 580, confidence = 0f),
            ),
            imageSize = image,
        )

        assertTrue(result.title.contains("visually prominent title"))
    }

    private fun line(
        text: String,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        confidence: Float = 0.9f,
    ) = OcrTextLine(text, left, top, right, bottom, confidence)

    private fun assertFalseContains(value: String, expected: String) {
        assertTrue("'$value' should not contain '$expected'", !value.contains(expected, ignoreCase = true))
    }
}
