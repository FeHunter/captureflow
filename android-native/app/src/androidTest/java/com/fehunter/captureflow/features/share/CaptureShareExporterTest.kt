package com.fehunter.captureflow.features.share

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fehunter.captureflow.features.share.data.CaptureShareExporter
import com.fehunter.captureflow.features.share.domain.model.CaptureShareNote
import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureShareExporterTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun exportsPdfAndRtfWithCoverAndNoteImage() = runBlocking {
        val imageDirectory = File(context.filesDir, "captures").apply { mkdirs() }
        val image = File(imageDirectory, "share-export-test-${System.nanoTime()}.png")
        val pdf = File(context.cacheDir, "share-export-test-${System.nanoTime()}.pdf")
        val rtf = File(context.cacheDir, "share-export-test-${System.nanoTime()}.rtf")
        val bitmap = Bitmap.createBitmap(160, 100, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.BLUE)
        }
        try {
            FileOutputStream(image).use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }
            val imageUri = Uri.fromFile(image).toString()
            val payload = CaptureSharePayload(
                title = "Linha de pensamento",
                category = "Ideias",
                createdAt = 1_700_000_000_000L,
                sourceUrl = "https://example.com/article",
                content = "Conteúdo principal da referência",
                coverUri = imageUri,
                mediaUris = listOf(imageUri),
                notes = listOf(CaptureShareNote("Anotação importante", imageUri)),
            )
            val exporter = CaptureShareExporter(context)
            exporter.exportPdf(payload, Uri.fromFile(pdf).toString())
            exporter.exportRtf(payload, Uri.fromFile(rtf).toString())

            assertTrue(pdf.isFile && pdf.length() > 100L)
            assertTrue(pdf.readBytes().copyOfRange(0, 4).toString(Charsets.US_ASCII) == "%PDF")
            val rtfContent = rtf.readText(Charsets.UTF_8)
            assertTrue(rtfContent.startsWith("{\\rtf1"))
            assertTrue(rtfContent.contains("\\jpegblip"))
            assertTrue(rtfContent.contains("Anota") && rtfContent.contains("importante"))
        } finally {
            bitmap.recycle()
            image.delete()
            pdf.delete()
            rtf.delete()
        }
    }
}
