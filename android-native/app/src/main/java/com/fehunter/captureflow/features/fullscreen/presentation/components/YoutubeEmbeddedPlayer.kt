package com.fehunter.captureflow.features.fullscreen.presentation.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Loads YouTube's official privacy-enhanced embed endpoint directly in the system WebView.
 *
 * The WebView keeps its default hardware-accelerated rendering path. Forcing an additional
 * hardware layer can interfere with Chromium's video-surface composition on some devices.
 * No JavaScript bridge is exposed to the page.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeEmbeddedPlayer(videoId: String, modifier: Modifier = Modifier) {
    val embedUrl = "https://www.youtube-nocookie.com/embed/$videoId" +
        "?controls=1&playsinline=1&rel=0&autoplay=0&fs=1"

    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f).heightIn(min = 200.dp),
        factory = { context ->
            WebView(context).apply {
                setBackgroundColor(android.graphics.Color.BLACK)
                // Do not call setLayerType here: WebView already uses the Activity's
                // hardware acceleration and must retain Chromium's native video composition.
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                overScrollMode = android.view.View.OVER_SCROLL_NEVER
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.javaScriptCanOpenWindowsAutomatically = false
                settings.setSupportMultipleWindows(false)
                settings.safeBrowsingEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        if (!request.isForMainFrame) return false
                        val host = request.url.host?.lowercase().orEmpty().removePrefix("www.")
                        return request.url.scheme != "https" ||
                            !(host == "youtube.com" || host.endsWith(".youtube.com") ||
                                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com"))
                    }
                }
                loadUrl(embedUrl, mapOf("Referer" to "https://${context.packageName.lowercase()}/"))
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) {
                webView.loadUrl(
                    embedUrl,
                    mapOf("Referer" to "https://${webView.context.packageName.lowercase()}/"),
                )
            }
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.destroy()
        },
    )
}
