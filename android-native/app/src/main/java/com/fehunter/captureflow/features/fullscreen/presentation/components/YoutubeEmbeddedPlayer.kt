package com.fehunter.captureflow.features.fullscreen.presentation.components

import android.annotation.SuppressLint
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Official YouTube iframe player hosted in the OS WebView.
 * JavaScript is enabled only for the trusted YouTube embed document; no JS bridge is exposed.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeEmbeddedPlayer(videoId: String, modifier: Modifier = Modifier) {
    val html = """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
          <style>
            html, body { margin:0; padding:0; width:100%; height:100%; background:#000; overflow:hidden; }
            #player { position:absolute; inset:0; width:100%; height:100%; }
            iframe { width:100%; height:100%; border:0; }
          </style>
        </head>
        <body>
          <div id="player">
            <iframe
              src="https://www.youtube-nocookie.com/embed/$videoId?playsinline=1&rel=0&autoplay=0"
              title="YouTube video player"
              allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share"
              referrerpolicy="strict-origin-when-cross-origin"
              allowfullscreen>
            </iframe>
          </div>
        </body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f),
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
                webChromeClient = WebChromeClient()
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val host = request.url.host?.lowercase().orEmpty()
                        return request.url.scheme != "https" ||
                            !(host == "youtube.com" || host.endsWith(".youtube.com") ||
                              host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") ||
                              host == "googlevideo.com" || host.endsWith(".googlevideo.com") ||
                              host == "ytimg.com" || host.endsWith(".ytimg.com"))
                    }
                }
                loadDataWithBaseURL(
                    "https://www.youtube-nocookie.com/",
                    html,
                    "text/html",
                    "UTF-8",
                    null,
                )
            }
        },
        update = { },
        onRelease = { webView ->
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        },
    )
}
