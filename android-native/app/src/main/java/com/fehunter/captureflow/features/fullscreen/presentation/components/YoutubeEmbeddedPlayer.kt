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
 * Loads YouTube's official privacy-enhanced embed endpoint directly in WebView.
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
                        // Allow iframe/player subresources. Restrict only top-level navigation.
                        if (!request.isForMainFrame) return false
                        val host = request.url.host?.lowercase().orEmpty().removePrefix("www.")
                        return request.url.scheme != "https" ||
                            host !in setOf("youtube.com", "youtube-nocookie.com")
                    }
                }
                loadUrl(embedUrl)
            }
        },
        update = { webView ->
            if (webView.url != embedUrl) webView.loadUrl(embedUrl)
        },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        },
    )
}
