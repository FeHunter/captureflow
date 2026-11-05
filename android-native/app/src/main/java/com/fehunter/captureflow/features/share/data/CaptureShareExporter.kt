package com.fehunter.captureflow.features.share.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.fehunter.captureflow.features.share.domain.model.CaptureSharePayload
import com.fehunter.captureflow.features.share.domain.repository.CaptureShareRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

class CaptureShareExporter(context: Context) : CaptureShareRepository {
    private val appContext = context.applicationContext

    override suspend fun exportPdf(payload: CaptureSharePayload, destination: String) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = 52f

        fun startPage() {
            page?.let(document::finishPage)
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            canvas.drawColor(Color.WHITE)
            y = 54f
        }
        fun ensureSpace(height: Float) {
            if (page == null || y + height > pageHeight - 48f) startPage()
        }
        fun paragraph(value: String, size: Float = 11f, bold: Boolean = false, color: Int = Color.DKGRAY) {
            if (value.isBlank()) return
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = if (bold) android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
                    else android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
                this.color = color
            }
            val maxWidth = pageWidth - 96f
            value.split('\n').forEach { paragraph ->
                var line = ""
                paragraph.split(Regex("\\s+")).forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                        ensureSpace(size * 1.7f)
                        canvas.drawText(line, 48f, y, paint)
                        y += size * 1.55f
                        line = word
                    } else line = candidate
                }
                if (line.isNotEmpty()) {
                    ensureSpace(size * 1.7f)
                    canvas.drawText(line, 48f, y, paint)
                    y += size * 1.55f
                }
            }
            y += 8f
        }
        fun addImage(rawUri: String?, label: String) {
            if (rawUri.isNullOrBlank()) return
            val bitmap = decodeImage(rawUri) ?: return
            try {
                val scale = minOf(500f / bitmap.width, 270f / bitmap.height, 1f)
                val width = bitmap.width * scale
                val height = bitmap.height * scale
                ensureSpace(height + 42f)
                paragraph(label, 10f, true)
                canvas.drawBitmap(bitmap, null, RectF(48f, y, 48f + width, y + height), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                y += height + 22f
            } finally {
                bitmap.recycle()
            }
        }

        try {
            startPage()
            paragraph("CAPTURE FLOW · CADERNO DE IDEIAS", 9f, true, Color.GRAY)
            paragraph(payload.title, 22f, true, Color.BLACK)
            paragraph("Categoria: ${payload.category}", 11f, true)
            paragraph("Criado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(payload.createdAt))}", 9f)
            payload.sourceUrl?.let { paragraph("Link: $it", 9f) }
            paragraph(payload.content, 11f)
            addImage(payload.coverUri, "Imagem de capa")
            payload.mediaUris.filter { it != payload.coverUri }.distinct().forEachIndexed { index, uri ->
                addImage(uri, "Imagem relacionada ${index + 1}")
            }
            payload.notes.forEachIndexed { index, note ->
                paragraph("ANOTAÇÃO ${index + 1}", 10f, true)
                paragraph(note.text, 11f)
                addImage(note.imageUri, "Imagem da anotação ${index + 1}")
            }
            page?.let(document::finishPage)
            appContext.contentResolver.openOutputStream(Uri.parse(destination), "w")?.use { output -> document.writeTo(output) }
                ?: throw IOException("Não foi possível gravar o PDF no destino selecionado.")
        } finally {
            document.close()
        }
    }

    override suspend fun exportRtf(payload: CaptureSharePayload, destination: String) = withContext(Dispatchers.IO) {
        val rtf = StringBuilder("{\\rtf1\\ansi\\deff0{\\fonttbl{\\f0 Arial;}}\\f0\\fs22 ")
        fun text(value: String, bold: Boolean = false) {
            if (bold) rtf.append("\\b ")
            value.forEach { char ->
                when (char) {
                    '\\' -> rtf.append("\\\\")
                    '{' -> rtf.append("\\{")
                    '}' -> rtf.append("\\}")
                    '\n' -> rtf.append("\\par ")
                    else -> if (char.code in 32..126) rtf.append(char) else rtf.append("\\u${char.code.toShort()}?")
                }
            }
            if (bold) rtf.append("\\b0 ")
            rtf.append("\\par ")
        }
        fun image(rawUri: String?, label: String) {
            if (rawUri.isNullOrBlank()) return
            val bitmap = decodeImage(rawUri) ?: return
            try {
                val exportBitmap = if (bitmap.hasAlpha()) {
                    Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888).also { opaque ->
                        Canvas(opaque).apply {
                            drawColor(Color.WHITE)
                            drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
                        }
                        opaque.setHasAlpha(false)
                    }
                } else bitmap
                val output = ByteArrayOutputStream()
                try {
                    if (!exportBitmap.compress(Bitmap.CompressFormat.JPEG, 78, output)) return
                } finally {
                    if (exportBitmap !== bitmap) exportBitmap.recycle()
                }
                val bytes = output.toByteArray()
                text(label, true)
                val imageScale = minOf(7500f / bitmap.width, 6000f / bitmap.height, 1f)
                val widthGoal = (bitmap.width * imageScale).toInt().coerceAtLeast(1)
                val heightGoal = (bitmap.height * imageScale).toInt().coerceAtLeast(1)
                rtf.append("{\\pict\\jpegblip\\picw${bitmap.width}\\pich${bitmap.height}\\picwgoal$widthGoal\\pichgoal$heightGoal ")
                val hexDigits = "0123456789abcdef"
                bytes.forEach { byte ->
                    val value = byte.toInt() and 0xff
                    rtf.append(hexDigits[value ushr 4]).append(hexDigits[value and 0x0f])
                }
                rtf.append("}\\par ")
            } finally {
                bitmap.recycle()
            }
        }
        text("CAPTURE FLOW · CADERNO DE IDEIAS", true)
        text(payload.title, true)
        text("Categoria: ${payload.category}")
        text("Criado em: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(payload.createdAt))}")
        payload.sourceUrl?.let { text("Link: $it") }
        text(payload.content)
        image(payload.coverUri, "Imagem de capa")
        payload.mediaUris.filter { it != payload.coverUri }.distinct().forEachIndexed { index, uri ->
            image(uri, "Imagem relacionada ${index + 1}")
        }
        payload.notes.forEachIndexed { index, note ->
            text("ANOTAÇÃO ${index + 1}", true)
            text(note.text)
            image(note.imageUri, "Imagem da anotação ${index + 1}")
        }
        rtf.append("}")
        appContext.contentResolver.openOutputStream(Uri.parse(destination), "w")?.use {
            it.write(rtf.toString().toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Não foi possível gravar o RTF no destino selecionado.")
    }

    private fun decodeImage(rawUri: String): Bitmap? {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        appContext.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > 1400 || bounds.outHeight / sample > 1400) sample *= 2
        return appContext.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        }
    }
}
