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
        val margin = 48f
        val contentWidth = pageWidth - margin * 2
        val ink = Color.rgb(57, 56, 47)
        val secondary = Color.rgb(117, 111, 97)
        val accent = Color.rgb(154, 113, 56)
        val paper = Color.rgb(243, 235, 221)
        val divider = Color.rgb(216, 204, 184)
        var pageNumber = 0
        var page: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = 76f
        val regularTypeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.NORMAL)
        val mediumTypeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        val boldTypeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)

        fun finishCurrentPage() {
            val current = page ?: return
            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = secondary; textSize = 8f; typeface = regularTypeface }
            val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = divider; strokeWidth = 0.8f }
            canvas.drawLine(margin, pageHeight - 36f, pageWidth - margin, pageHeight - 36f, linePaint)
            canvas.drawText("CAPTURE FLOW", margin, pageHeight - 21f, footerPaint)
            val pageLabel = "Página $pageNumber"
            canvas.drawText(pageLabel, pageWidth - margin - footerPaint.measureText(pageLabel), pageHeight - 21f, footerPaint)
            document.finishPage(current)
            page = null
        }
        fun startPage() {
            finishCurrentPage()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            canvas = page!!.canvas
            canvas.drawColor(Color.WHITE)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 48f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = paper })
            canvas.drawText("CAPTURE FLOW  /  CADERNO DE IDEIAS", margin, 30f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 9f; typeface = boldTypeface })
            y = 76f
        }
        fun ensureSpace(height: Float) {
            if (page == null || y + height > pageHeight - 52f) startPage()
        }
        fun drawWrappedText(value: String, paint: Paint, lineHeight: Float) {
            value.split('\n').forEach { paragraph ->
                if (paragraph.isBlank()) { y += lineHeight * 0.55f; return@forEach }
                var line = ""
                paragraph.split(Regex("\\s+")).forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (line.isNotEmpty() && paint.measureText(candidate) > contentWidth) {
                        ensureSpace(lineHeight); canvas.drawText(line, margin, y, paint); y += lineHeight; line = ""
                    }
                    if (paint.measureText(word) > contentWidth) {
                        var fragment = ""
                        word.forEach { char ->
                            val next = fragment + char
                            if (paint.measureText(next) > contentWidth && fragment.isNotEmpty()) {
                                ensureSpace(lineHeight); canvas.drawText(fragment, margin, y, paint); y += lineHeight; fragment = char.toString()
                            } else fragment = next
                        }
                        line = fragment
                    } else line = if (line.isEmpty()) word else "$line $word"
                }
                if (line.isNotEmpty()) { ensureSpace(lineHeight); canvas.drawText(line, margin, y, paint); y += lineHeight }
            }
        }
        fun paragraph(value: String, size: Float = 11f, bold: Boolean = false, color: Int = ink, extraSpacing: Float = 7f) {
            if (value.isBlank()) return
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                typeface = when { bold && size >= 18f -> boldTypeface; bold -> mediumTypeface; else -> regularTypeface }
                this.color = color
            }
            drawWrappedText(value, paint, size * 1.48f)
            y += extraSpacing
        }
        fun section(title: String) {
            ensureSpace(28f)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent; textSize = 9f; typeface = boldTypeface }
            canvas.drawText(title.uppercase(Locale.getDefault()), margin, y, paint)
            y += 7f
            canvas.drawLine(margin, y, pageWidth - margin, y, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = divider; strokeWidth = 0.8f })
            y += 17f
        }
        fun addImage(rawUri: String?, label: String) {
            if (rawUri.isNullOrBlank()) return
            val bitmap = runCatching { decodeImage(rawUri) }.getOrNull()
                ?: throw IOException("Não foi possível incluir a imagem '$label' no PDF.")
            try {
                val scale = minOf(contentWidth / bitmap.width, 300f / bitmap.height, 1f)
                val width = bitmap.width * scale
                val height = bitmap.height * scale
                ensureSpace(height + 46f)
                paragraph(label, 9f, true, secondary, 8f)
                canvas.drawBitmap(bitmap, null, RectF(margin, y, margin + width, y + height),
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
                y += height + 22f
            } finally { bitmap.recycle() }
        }
        try {
            startPage()
            paragraph(payload.title.ifBlank { "Referência sem título" }, 24f, true, ink, 10f)
            paragraph(payload.category, 10f, true, accent, 14f)
            section("Informações")
            paragraph("Criado em  ${SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale.getDefault()).format(Date(payload.createdAt))}", 9f, false, secondary, 5f)
            payload.sourceUrl?.takeIf(String::isNotBlank)?.let { paragraph("Link de origem  $it", 9f, false, secondary, 5f) }
            payload.content.takeIf(String::isNotBlank)?.let { paragraph(it, 11f, false, ink, 10f) }
            payload.coverUri?.takeIf(String::isNotBlank)?.let { section("Imagem de capa"); addImage(it, "Capa") }
            val galleryImages = payload.mediaUris.filter { it.isNotBlank() && it != payload.coverUri }.distinct()
            if (galleryImages.isNotEmpty()) {
                section("Galeria")
                galleryImages.forEachIndexed { index, uri -> addImage(uri, "Imagem ${index + 1} da galeria") }
            }
            if (payload.notes.isNotEmpty()) {
                section("Anotações")
                payload.notes.forEachIndexed { index, note ->
                    ensureSpace(24f)
                    paragraph("Anotação ${index + 1}", 10f, true, accent, 4f)
                    paragraph(note.text, 11f, false, ink, 8f)
                    addImage(note.imageUri, "Imagem da anotação ${index + 1}")
                }
            }
            finishCurrentPage()
            appContext.contentResolver.openOutputStream(Uri.parse(destination), "w")?.use { document.writeTo(it) }
                ?: throw IOException("Não foi possível gravar o PDF no destino selecionado.")
        } finally { document.close() }
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
