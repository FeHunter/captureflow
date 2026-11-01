package com.fehunter.captureflow.features.imagecompression

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fehunter.captureflow.features.imagecompression.data.ImageCompressionService
import com.fehunter.captureflow.features.imagecompression.domain.model.ImageCompressionProfile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.Assume.assumeTrue
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImageCompressionServiceTest {
    private val context: Context
        get() = ApplicationProvider.getApplicationContext()

    @Test
    fun compressesAppOwnedFileUriIntoPrivateJpeg() {
        val source = File(context.cacheDir, "compression-source-${System.nanoTime()}.png")
        val bitmap = Bitmap.createBitmap(900, 600, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.MAGENTA)
        }

        try {
            FileOutputStream(source).use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }

            val outputUri = Uri.parse(
                ImageCompressionService(context).compressToPrivateStorage(
                    Uri.fromFile(source).toString(),
                    ImageCompressionProfile.MEDIUM,
                )
            )
            val output = File(outputUri.path!!)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(output.path, bounds)

            assertTrue(output.isFile && output.length() > 0L)
            assertEquals("image/jpeg", bounds.outMimeType)
            assertTrue(output.path.endsWith(".jpg"))
            output.delete()
        } finally {
            bitmap.recycle()
            source.delete()
        }
    }


    @Test
    fun compressesContentProviderUriIntoPrivateJpeg() {
        assumeTrue("MediaStore RELATIVE_PATH requires Android 10+", Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "captureflow-compression-${System.nanoTime()}.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/CaptureFlowCompressionTest",
            )
        }
        val sourceUri = context.contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values,
        )
        assertNotNull(sourceUri)

        val bitmap = Bitmap.createBitmap(640, 480, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.CYAN)
        }
        try {
            val sourceOutput = context.contentResolver.openOutputStream(sourceUri!!)
                ?: error("MediaStore não disponibilizou um fluxo de escrita.")
            sourceOutput.use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
            }

            val outputUri = Uri.parse(
                ImageCompressionService(context).compressToPrivateStorage(
                    sourceUri.toString(),
                    ImageCompressionProfile.MEDIUM,
                )
            )
            val output = File(outputUri.path!!)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(output.path, bounds)

            assertTrue(output.isFile && output.length() > 0L)
            assertEquals("image/jpeg", bounds.outMimeType)
            output.delete()
        } finally {
            bitmap.recycle()
            context.contentResolver.delete(sourceUri!!, null, null)
        }
    }

    @Test
    fun compressesLosslessCropBytesWithoutAnIntermediateFileUri() {
        val source = Bitmap.createBitmap(3000, 1800, Bitmap.Config.ARGB_8888).apply {
            eraseColor(Color.TRANSPARENT)
            setPixel(10, 10, Color.RED)
        }
        val encoded = ByteArrayOutputStream().use { output ->
            assertTrue(source.compress(Bitmap.CompressFormat.PNG, 100, output))
            output.toByteArray()
        }

        try {
            val outputUri = Uri.parse(
                ImageCompressionService(context).compressEncodedImageToPrivateStorage(
                    encoded,
                    ImageCompressionProfile.HIGH,
                )
            )
            val output = File(outputUri.path!!)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(output.path, bounds)

            assertTrue(output.isFile && output.length() > 0L)
            assertEquals("image/jpeg", bounds.outMimeType)
            assertTrue(bounds.outWidth <= 2560)
            assertTrue(bounds.outHeight <= 2560)
            val decoded = BitmapFactory.decodeFile(output.path)
            assertNotNull(decoded)
            decoded?.recycle()
            output.delete()
        } finally {
            source.recycle()
        }
    }
}
