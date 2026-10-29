package com.fehunter.captureflow.services

import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import java.io.Closeable

/**
 * Best-effort screenshot discovery while CaptureFlow is visible.
 * It does not run a foreground service or monitor other apps.
 */
class ScreenshotMonitor(
    context: Context,
    private val onScreenshotDetected: (Uri) -> Unit,
) : Closeable {
    private val resolver = context.applicationContext.contentResolver
    private val handler = Handler(Looper.getMainLooper())
    private val startedAtSeconds = System.currentTimeMillis() / 1_000L
    private var lastEmittedUri: Uri? = null
    private var isRegistered = false
    private val scanRunnable = Runnable { findLatestScreenshot() }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            handler.removeCallbacks(scanRunnable)
            handler.postDelayed(scanRunnable, SCAN_DEBOUNCE_MS)
        }
    }

    fun start() {
        if (isRegistered) return
        resolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        isRegistered = true
    }

    private fun findLatestScreenshot() {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Images.Media.RELATIVE_PATH)
        }
        runCatching {
            resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1
                var checked = 0
                while (cursor.moveToNext() && checked < MAX_RECENT_IMAGES_TO_CHECK) {
                    checked++
                    if (cursor.getLong(dateColumn) < startedAtSeconds - CREATION_GRACE_SECONDS) break
                    val name = cursor.getString(nameColumn).orEmpty()
                    val path = if (pathColumn >= 0) cursor.getString(pathColumn).orEmpty() else ""
                    if (!looksLikeScreenshot(name, path)) continue
                    val imageUri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idColumn),
                    )
                    if (imageUri != lastEmittedUri) {
                        lastEmittedUri = imageUri
                        onScreenshotDetected(imageUri)
                    }
                    return@use
                }
            }
        }
    }

    private fun looksLikeScreenshot(displayName: String, relativePath: String): Boolean {
        val name = displayName.lowercase()
        val path = relativePath.lowercase()
        return name.contains("screenshot") ||
            name.contains("screen_shot") ||
            name.contains("screen-shot") ||
            name.contains("screen shot") ||
            name.contains("screencap") ||
            path.contains("/screenshots/") ||
            path.endsWith("/screenshots")
    }

    override fun close() {
        if (isRegistered) {
            resolver.unregisterContentObserver(observer)
            isRegistered = false
        }
        handler.removeCallbacks(scanRunnable)
    }

    private companion object {
        const val SCAN_DEBOUNCE_MS = 650L
        const val MAX_RECENT_IMAGES_TO_CHECK = 50
        const val CREATION_GRACE_SECONDS = 2L
    }
}
