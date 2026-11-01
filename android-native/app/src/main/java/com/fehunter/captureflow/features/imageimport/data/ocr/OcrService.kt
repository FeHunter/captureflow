package com.fehunter.captureflow.features.imageimport.data.ocr

import android.content.Context
import android.net.Uri
import com.fehunter.captureflow.features.imageimport.domain.ocr.OcrImageSize
import com.fehunter.captureflow.features.imageimport.domain.ocr.OcrTextLine
import com.fehunter.captureflow.features.imageimport.domain.ocr.TitleDetectionService
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

class OcrService(
    private val titleDetectionService: TitleDetectionService = TitleDetectionService(),
) {
    suspend fun extractText(context: Context, uri: Uri): String =
        recognize(context, uri).text

    suspend fun extractStructuredText(context: Context, uri: Uri): OcrTextResult {
        val image = InputImage.fromFilePath(context, uri)
        val result = recognizeImage(image)
        val lines = result.textBlocks.flatMap { block ->
            block.lines.mapNotNull { line ->
                val bounds = line.boundingBox ?: return@mapNotNull null
                val text = line.text.trim()
                if (text.isBlank()) return@mapNotNull null
                OcrTextLine(
                    text = text,
                    left = bounds.left,
                    top = bounds.top,
                    right = bounds.right,
                    bottom = bounds.bottom,
                    confidence = line.confidence,
                )
            }
        }
        val detection = titleDetectionService.detect(
            lines = lines,
            imageSize = OcrImageSize(image.width, image.height),
        )

        return OcrTextResult(
            fullText = result.text,
            title = detection.title,
            description = detection.description,
        )
    }

    private suspend fun recognize(context: Context, uri: Uri): Text =
        recognizeImage(InputImage.fromFilePath(context, uri))

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
