package com.fehunter.captureflow.features.youtubeplayer.presentation

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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

private const val TAG = "YoutubePlayer"
private const val BRIDGE_NAME = "CaptureFlowYoutube"

private sealed interface PlayerStatus {
    data object Loading : PlayerStatus
    data object Ready : PlayerStatus
    data class Failed(val code: Int?, val message: String) : PlayerStatus
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YoutubeEmbeddedPlayer(videoId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var status by remember(videoId) { mutableStateOf<PlayerStatus>(PlayerStatus.Loading) }
    val setCurrentStatus by rememberUpdatedState<(PlayerStatus) -> Unit>({ status = it })
    val origin = remember(context.packageName) { "https://" + context.packageName.lowercase() }
    val html = remember(videoId, origin) { YoutubePlayerHtml.create(videoId, origin) }
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
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box(
        modifier = modifier.fillMaxWidth().aspectRatio(16f / 9f)
            .heightIn(min = 200.dp).background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        key(videoId) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                val bridge = PlayerJavascriptBridge { event ->
                    when (event) {
                        PlayerEvent.Ready -> setCurrentStatus(PlayerStatus.Ready)
                        is PlayerEvent.Error -> setCurrentStatus(PlayerStatus.Failed(event.code, describeError(event.code)))
                        is PlayerEvent.StateChanged -> Log.d(TAG, "Player state=" + event.state)
                    }
                }
                WebView(viewContext).apply {
                    webView = this
                    setBackgroundColor(AndroidColor.BLACK)
                    setLayerType(View.LAYER_TYPE_HARDWARE, null)
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
                    addJavascriptInterface(bridge, BRIDGE_NAME)
                    webChromeClient = object : WebChromeClient() {
                        override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                            Log.d(TAG, message.message() + " (" + message.sourceId() + ":" + message.lineNumber() + ")")
                            return true
                        }
                    }
                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                            if (!request.isForMainFrame) return false
                            val host = request.url.host?.lowercase().orEmpty().removePrefix("www.")
                            val youtubeHost = host == "youtube.com" || host.endsWith(".youtube.com") ||
                                host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com")
                            return request.url.scheme != "https" || !youtubeHost
                        }

                        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                            super.onReceivedError(view, request, error)
                            if (request.isForMainFrame) {
                                setCurrentStatus(PlayerStatus.Failed(null, "Não foi possível carregar o player. Verifique sua conexão e tente novamente."))
                            }
                        }
                    }
                    // Android's documented WebView approach: baseUrl supplies the HTTP Referer.
                    tag = videoId
                    loadDataWithBaseURL(origin + "/", html, "text/html", "UTF-8", null)
                    requestFocus()
                }
            },
            update = { view ->
                if (view.tag != videoId) {
                    view.tag = videoId
                    view.loadDataWithBaseURL(origin + "/", html, "text/html", "UTF-8", null)
                }
            },
            onRelease = { released ->
                released.stopLoading()
                released.onPause()
                released.removeJavascriptInterface(BRIDGE_NAME)
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
            is PlayerStatus.Failed -> Text(
                text = current.message + (current.code?.let { "\nCódigo do YouTube: " + it } ?: ""),
                modifier = Modifier.fillMaxWidth(0.88f),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private sealed interface PlayerEvent {
    data object Ready : PlayerEvent
    data class Error(val code: Int) : PlayerEvent
    data class StateChanged(val state: Int) : PlayerEvent
}

private class PlayerJavascriptBridge(private val onEvent: (PlayerEvent) -> Unit) {
    private val mainHandler = Handler(Looper.getMainLooper())

    @JavascriptInterface fun onReady() = dispatch(PlayerEvent.Ready)
    @JavascriptInterface fun onError(code: Int) = dispatch(PlayerEvent.Error(code))
    @JavascriptInterface fun onStateChange(state: Int) = dispatch(PlayerEvent.StateChanged(state))

    private fun dispatch(event: PlayerEvent) {
        mainHandler.post { onEvent(event) }
    }
}

private fun describeError(code: Int): String = when (code) {
    2 -> "O YouTube recebeu um identificador ou parâmetro inválido."
    5 -> "O vídeo não pôde ser reproduzido pelo player HTML5."
    100 -> "Este vídeo não está disponível ou é privado."
    101, 150, 152 -> "Este vídeo não permite reprodução incorporada. Abra o link original no YouTube."
    153 -> "O YouTube não reconheceu a identificação da aplicação (HTTP Referer)."
    else -> "O YouTube não conseguiu reproduzir este vídeo."
}
