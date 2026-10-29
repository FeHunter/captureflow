package com.fehunter.captureflow.services

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrService {
    suspend fun extractText(context: Context, uri: Uri): String = suspendCancellableCoroutine { continuation ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            recognizer.process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener { result -> if (continuation.isActive) continuation.resume(result.text) }
                .addOnFailureListener { error -> if (continuation.isActive) continuation.resumeWithException(error) }
                .addOnCompleteListener { recognizer.close() }
        } catch (error: Exception) {
            recognizer.close()
            if (continuation.isActive) continuation.resumeWithException(error)
        }
    }
}
