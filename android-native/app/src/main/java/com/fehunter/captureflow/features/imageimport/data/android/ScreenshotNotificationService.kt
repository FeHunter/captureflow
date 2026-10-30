package com.fehunter.captureflow.features.imageimport.data.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import com.fehunter.captureflow.MainActivity

/**
 * Optional, user-enabled foreground service that watches MediaStore for recent screenshots.
 * It never reads screen contents or uses Accessibility Services.
 */
class ScreenshotNotificationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var observerRegistered = false
    private var lastNotifiedUri: Uri? = null
    private val scanRunnable = Runnable { findLatestScreenshot() }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            handler.removeCallbacks(scanRunnable)
            handler.postDelayed(scanRunnable, DEBOUNCE_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        startForeground(ONGOING_NOTIFICATION_ID, buildOngoingNotification())
        contentResolver.registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer)
        observerRegistered = true
        handler.postDelayed(scanRunnable, DEBOUNCE_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun findLatestScreenshot() {
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) add(MediaStore.Images.Media.RELATIVE_PATH)
        }
        runCatching {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else -1
                var checked = 0
                while (cursor.moveToNext() && checked < MAX_IMAGES_TO_CHECK) {
                    checked++
                    val addedAt = cursor.getLong(dateIndex)
                    if (addedAt < System.currentTimeMillis() / 1_000L - RECENT_WINDOW_SECONDS) break
                    val name = cursor.getString(nameIndex).orEmpty()
                    val path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else ""
                    if (!looksLikeScreenshot(name, path)) continue
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idIndex),
                    )
                    if (uri != lastNotifiedUri) {
                        lastNotifiedUri = uri
                        notifyScreenshot(uri)
                    }
                    return@use
                }
            }
        }
    }

    private fun looksLikeScreenshot(name: String, relativePath: String): Boolean {
        val normalizedName = name.lowercase()
        val normalizedPath = relativePath.lowercase()
        return listOf("screenshot", "screen_shot", "screen-shot", "screen shot", "screencap")
            .any(normalizedName::contains) ||
            normalizedPath.contains("/screenshots/") ||
            normalizedPath.endsWith("/screenshots")
    }

    private fun notifyScreenshot(uri: Uri) {
        val taskIntent = actionIntent(ACTION_CREATE_TASK, uri, 2)
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Screenshot pronta para tarefa")
            .setContentText("Toque para abrir o CaptureFlow e salvar a imagem como tarefa")
            .setContentIntent(taskIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Salvar como tarefa",
                taskIntent,
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(ALERT_NOTIFICATION_ID, notification)
    }

    private fun actionIntent(action: String, uri: Uri, requestCode: Int): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            this.action = action
            data = Uri.parse("captureflow://screenshot/${uri.lastPathSegment ?: uri.hashCode()}")
            putExtra(EXTRA_SCREENSHOT_URI, uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun buildOngoingNotification(): Notification =
        NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("CaptureFlow")
            .setContentText("Detecção de screenshots ativada")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(ONGOING_CHANNEL_ID, "Monitoramento de screenshots", NotificationManager.IMPORTANCE_LOW)
            )
            manager.createNotificationChannel(
                NotificationChannel(ALERT_CHANNEL_ID, "Screenshots detectadas", NotificationManager.IMPORTANCE_HIGH)
            )
        }
    }

    override fun onDestroy() {
        if (observerRegistered) contentResolver.unregisterContentObserver(observer)
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.fehunter.captureflow.action.STOP_SCREENSHOT_MONITOR"
        const val ACTION_SAVE_REFERENCE = "com.fehunter.captureflow.action.SAVE_SCREENSHOT_REFERENCE"
        const val ACTION_CREATE_TASK = "com.fehunter.captureflow.action.CREATE_SCREENSHOT_TASK"
        const val EXTRA_SCREENSHOT_URI = "com.fehunter.captureflow.extra.SCREENSHOT_URI"
        const val PREFERENCES = "captureflow_preferences"
        const val PREF_MONITOR_ENABLED = "screenshot_monitor_enabled"
        const val ONGOING_NOTIFICATION_ID = 8201
        const val ALERT_NOTIFICATION_ID = 8202
        private const val ONGOING_CHANNEL_ID = "screenshot_monitor"
        private const val ALERT_CHANNEL_ID = "screenshot_alerts"
        private const val DEBOUNCE_MS = 700L
        private const val MAX_IMAGES_TO_CHECK = 50
        private const val RECENT_WINDOW_SECONDS = 30L
    }
}
