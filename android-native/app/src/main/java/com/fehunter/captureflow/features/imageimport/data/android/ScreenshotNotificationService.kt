package com.fehunter.captureflow.features.imageimport.data.android

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.fehunter.captureflow.MainActivity

/**
 * User-enabled, continuous screenshot discovery.
 *
 * The service only reads MediaStore metadata. It never captures the screen, reads image
 * pixels during discovery, uses Accessibility APIs, or uploads images. Import is always
 * initiated by the user from the notification and confirmed inside CaptureFlow.
 */
class ScreenshotNotificationService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private var observerRegistered = false
    private val seenUris = linkedSetOf<String>()
    private val scanRunnable = Runnable { findRecentScreenshots() }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            super.onChange(selfChange, uri)
            handler.removeCallbacks(scanRunnable)
            handler.postDelayed(scanRunnable, SCAN_DEBOUNCE_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        ServiceCompat.startForeground(
            this,
            ONGOING_NOTIFICATION_ID,
            buildOngoingNotification(),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                0
            },
        )
        loadSeenUris()
        contentResolver.registerContentObserver(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            true,
            observer,
        )
        observerRegistered = true
        handler.postDelayed(scanRunnable, INITIAL_SCAN_DELAY_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(PREF_MONITOR_ENABLED, false)
                .apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf(startId)
            return START_NOT_STICKY
        }

        if (!isMonitoringAllowed()) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(PREF_MONITOR_ENABLED, false)
                .apply()
            stopSelf(startId)
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    private fun isMonitoringAllowed(): Boolean {
        val imagePermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
        val imageGranted = checkSelfPermission(imagePermission) == PackageManager.PERMISSION_GRANTED
        val notificationsGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        val enabled = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            .getBoolean(PREF_MONITOR_ENABLED, false)
        return enabled && imageGranted && notificationsGranted
    }

    private fun findRecentScreenshots() {
        if (!isMonitoringAllowed()) {
            getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
                .putBoolean(PREF_MONITOR_ENABLED, false)
                .apply()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }

        val preferences = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
        val lastScan = preferences.getLong(
            PREF_LAST_SCAN_SECONDS,
            System.currentTimeMillis() / 1_000L - INITIAL_LOOKBACK_SECONDS,
        )
        val projection = buildList {
            add(MediaStore.Images.Media._ID)
            add(MediaStore.Images.Media.DISPLAY_NAME)
            add(MediaStore.Images.Media.DATE_ADDED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                add(MediaStore.Images.Media.RELATIVE_PATH)
            }
        }

        runCatching {
            contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection.toTypedArray(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    "${MediaStore.Images.Media.DATE_ADDED} >= ? AND ${MediaStore.MediaColumns.IS_PENDING} = 0"
                } else {
                    "${MediaStore.Images.Media.DATE_ADDED} >= ?"
                },
                arrayOf((lastScan - CREATION_GRACE_SECONDS).coerceAtLeast(0L).toString()),
                "${MediaStore.Images.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val pathIndex = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                } else {
                    -1
                }

                val candidates = mutableListOf<Pair<Long, Uri>>()
                var checked = 0
                var newestTimestamp = lastScan
                while (cursor.moveToNext() && checked < MAX_RECENT_IMAGES_TO_CHECK) {
                    checked++
                    val addedAt = cursor.getLong(dateIndex)
                    newestTimestamp = maxOf(newestTimestamp, addedAt)
                    val name = cursor.getString(nameIndex).orEmpty()
                    val path = if (pathIndex >= 0) cursor.getString(pathIndex).orEmpty() else ""
                    if (!looksLikeScreenshot(name, path)) continue
                    val imageUri = Uri.withAppendedPath(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        cursor.getLong(idIndex).toString(),
                    )
                    candidates += addedAt to imageUri
                }

                candidates.asReversed().forEach { (_, imageUri) ->
                    val key = imageUri.toString()
                    if (seenUris.add(key)) notifyScreenshot(imageUri)
                }
                preferences.edit()
                    .putLong(PREF_LAST_SCAN_SECONDS, newestTimestamp)
                    .apply()
                persistSeenUris()
            }
        }
    }

    private fun looksLikeScreenshot(displayName: String, relativePath: String): Boolean {
        val name = displayName.lowercase()
        val path = relativePath.lowercase()
        return listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screen shot",
            "screen_capture",
            "screen capture",
            "screencap",
            "captura de tela",
            "captura_de_tela",
            "captura-de-tela",
        ).any(name::contains) ||
            path.contains("/screenshots/") ||
            path.endsWith("/screenshots") ||
            path.contains("/capturas de tela/") ||
            path.endsWith("/capturas de tela")
    }

    private fun notifyScreenshot(uri: Uri) {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_IMPORT_SCREENSHOT
            data = Uri.parse("captureflow://screenshot/${uri.lastPathSegment ?: uri.hashCode()}")
            putExtra(EXTRA_SCREENSHOT_URI, uri.toString())
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val requestCode = uri.toString().hashCode()
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notificationId = (uri.toString().hashCode() and 0x7fffffff)
            .let { if (it == ONGOING_NOTIFICATION_ID) it + 1 else it }
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("Nova screenshot detectada")
            .setContentText("Toque para revisar e decidir se deseja salvar no CaptureFlow")
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Revisar captura",
                pendingIntent,
            )
            .setAutoCancel(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        getSystemService(NotificationManager::class.java).notify(notificationId, notification)
    }

    private fun buildOngoingNotification(): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            ONGOING_NOTIFICATION_ID,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            ONGOING_NOTIFICATION_ID + 1,
            Intent(this, ScreenshotNotificationService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, ONGOING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("CaptureFlow · Monitoramento ativo")
            .setContentText("Detectando novas screenshots. Toque em Desativar para encerrar.")
            .setContentIntent(openIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Desativar", stopIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(
                    ONGOING_CHANNEL_ID,
                    "Monitoramento de screenshots",
                    NotificationManager.IMPORTANCE_LOW,
                ),
            )
            manager.createNotificationChannel(
                NotificationChannel(
                    ALERT_CHANNEL_ID,
                    "Screenshots detectadas",
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }
    }

    private fun loadSeenUris() {
        val saved = getSharedPreferences(PREFERENCES, MODE_PRIVATE)
            .getStringSet(PREF_SEEN_URIS, emptySet())
            .orEmpty()
        seenUris.addAll(saved)
    }

    private fun persistSeenUris() {
        while (seenUris.size > MAX_REMEMBERED_URIS) {
            seenUris.remove(seenUris.first())
        }
        getSharedPreferences(PREFERENCES, MODE_PRIVATE).edit()
            .putStringSet(PREF_SEEN_URIS, seenUris.toSet())
            .apply()
    }

    override fun onDestroy() {
        if (observerRegistered) {
            contentResolver.unregisterContentObserver(observer)
            observerRegistered = false
        }
        handler.removeCallbacks(scanRunnable)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_STOP = "com.fehunter.captureflow.action.STOP_SCREENSHOT_MONITOR"
        const val ACTION_IMPORT_SCREENSHOT = "com.fehunter.captureflow.action.IMPORT_SCREENSHOT"
        const val EXTRA_SCREENSHOT_URI = "com.fehunter.captureflow.extra.SCREENSHOT_URI"
        const val PREFERENCES = "captureflow_preferences"
        const val PREF_MONITOR_ENABLED = "screenshot_monitor_enabled"
        const val ONGOING_NOTIFICATION_ID = 8201
        const val PREF_LAST_SCAN_SECONDS = "screenshot_monitor_last_scan_seconds"
        private const val PREF_SEEN_URIS = "screenshot_monitor_seen_uris"
        private const val ONGOING_CHANNEL_ID = "screenshot_monitor"
        private const val ALERT_CHANNEL_ID = "screenshot_alerts"
        private const val SCAN_DEBOUNCE_MS = 700L
        private const val INITIAL_SCAN_DELAY_MS = 1_000L
        private const val INITIAL_LOOKBACK_SECONDS = 3L
        private const val CREATION_GRACE_SECONDS = 2L
        private const val MAX_RECENT_IMAGES_TO_CHECK = 200
        private const val MAX_REMEMBERED_URIS = 100
    }
}
