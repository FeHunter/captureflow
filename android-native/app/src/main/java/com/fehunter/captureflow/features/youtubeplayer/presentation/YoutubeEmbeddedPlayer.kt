package com.fehunter.captureflow.features.youtubeplayer.presentation

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

private sealed interface PlayerStatus {
    data object Loading : PlayerStatus
    data object Ready : PlayerStatus
    data class Failed(val message: String) : PlayerStatus
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeEmbeddedPlayer(videoId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(videoId) { mutableStateOf<PlayerStatus>(PlayerStatus.Loading) }
    var reloadAttempt by remember(videoId) { mutableIntStateOf(0) }
    val latestStatus by rememberUpdatedState(status)
    val request = remember(videoId, context.packageName) {
        YoutubeEmbedRequestFactory.create(videoId, context.packageName)
    }
    var webView by remember(videoId) { mutableStateOf<WebView?>(null) }

    DisposableEffect(lifecycleOwner, webView) {
        val currentWebView = webView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentWebView?.onResume()
                Lifecycle.Event.ON_PAUSE -> currentWebView?.onPause()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .heightIn(min = 200.dp).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(videoId, reloadAttempt) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    WebView(viewContext).apply {
                        webView = this
                        setBackgroundColor(AndroidColor.BLACK)
                        isVerticalScrollBarEnabled = false
                        isHorizontalScrollBarEnabled = false
                        overScrollMode = View.OVER_SCROLL_NEVER
                        isFocusable = true
                        isFocusableInTouchMode = true
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = true
                            allowFileAccess = false
                            allowContentAccess = false
                            javaScriptCanOpenWindowsAutomatically = false
                            setSupportMultipleWindows(false)
                            safeBrowsingEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(
                                view: WebView,
                                request: WebResourceRequest,
                            ): Boolean {
                                if (!request.isForMainFrame) return false
                                val host = request.url.host?.lowercase().orEmpty().removePrefix("www.")
                                val youtubeHost = host == "youtube.com" || host.endsWith(".youtube.com") ||
                                    host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
                                return request.url.scheme != "https" || !youtubeHost
                            }

                            override fun onPageStarted(view: WebView, url: String, favicon: android.graphics.Bitmap?) {
                                super.onPageStarted(view, url, favicon)
                                status = PlayerStatus.Loading
                            }

                            override fun onPageFinished(view: WebView, url: String) {
                                super.onPageFinished(view, url)
                                if (url.startsWith("https://www.youtube-nocookie.com/embed/")) {
                                    status = PlayerStatus.Ready
                                }
                            }

                            override fun onReceivedError(
                                view: WebView,
                                request: WebResourceRequest,
                                error: android.webkit.WebResourceError,
                            ) {
                                super.onReceivedError(view, request, error)
                                if (request.isForMainFrame) {
                                    status = PlayerStatus.Failed(
                                        "Não foi possível carregar o player. Verifique sua conexão e tente novamente.",
                                    )
                                }
                            }

                            override fun onReceivedHttpError(
                                view: WebView,
                                request: WebResourceRequest,
                                errorResponse: android.webkit.WebResourceResponse,
                            ) {
                                super.onReceivedHttpError(view, request, errorResponse)
                                if (request.isForMainFrame && errorResponse.statusCode >= 400) {
                                    status = PlayerStatus.Failed(
                                        "O YouTube respondeu com HTTP ${errorResponse.statusCode}. Abra o link original se o problema persistir.",
                                    )
                                }
                            }
                        }
                        loadUrl(request.url, request.headers)
                        requestFocus()
                    }
                },
                update = { view ->
                    if (view.tag != request.url) {
                        view.tag = request.url
                        view.loadUrl(request.url, request.headers)
                    }
                },
                onRelease = { released ->
                    released.stopLoading()
                    released.onPause()
                    released.loadUrl("about:blank")
                    released.clearHistory()
                    released.destroy()
                    if (webView === released) webView = null
                },
            )
        }

        when (val current = status) {
            PlayerStatus.Loading -> CircularProgressIndicator(color = Color.White)
            PlayerStatus.Ready -> Unit
            is PlayerStatus.Failed -> Column(
                modifier = Modifier.fillMaxWidth(0.88f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    current.message,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = {
                    status = PlayerStatus.Loading
                    reloadAttempt += 1
                }) {
                    Text("Tentar novamente", color = Color.White)
                }
            }
        }
    }
}
