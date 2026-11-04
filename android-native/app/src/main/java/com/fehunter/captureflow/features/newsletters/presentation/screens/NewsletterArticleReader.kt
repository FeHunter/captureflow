package com.fehunter.captureflow.features.newsletters.presentation.screens

import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.fehunter.captureflow.features.newsletters.domain.model.NewsletterArticle

@Composable
fun NewsletterArticleReader(article: NewsletterArticle, onBack: () -> Unit) {
    BackHandler(onBack = onBack)
    val pageBackground = MaterialTheme.colorScheme.surface.toCssHex()
    val pageText = MaterialTheme.colorScheme.onSurface.toCssHex()
    val quoteBorder = MaterialTheme.colorScheme.primary.toCssHex()
    val quoteText = MaterialTheme.colorScheme.onSurfaceVariant.toCssHex()
    val quoteBackground = MaterialTheme.colorScheme.surfaceVariant.toCssHex()
    val styleKey = listOf(article.id, pageBackground, pageText, quoteBorder, quoteText, quoteBackground).joinToString("|")
    Column(Modifier.fillMaxSize()) {
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 2.dp) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar às publicações")
                }
                Column(Modifier.weight(1f).padding(vertical = 6.dp)) {
                    Text(article.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("Leitura", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                    webViewClient = WebViewClient()
                    webChromeClient = WebChromeClient()
                    tag = styleKey
                    val html = article.contentHtml?.takeIf(String::isNotBlank)
                    if (html != null) {
                        val document = """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><style>
                            body{font-family:sans-serif;line-height:1.7;padding:20px 18px 40px;color:$pageText;background:$pageBackground}
                            blockquote{margin-left:0;padding:8px 0 8px 14px;border-left:3px solid $quoteBorder;color:$quoteText;background:$quoteBackground}
                            img,video,iframe{max-width:100%;height:auto}pre,code{white-space:pre-wrap;overflow-wrap:anywhere}
                            blockquote{margin-left:0;padding:8px 0 8px 14px;border-left:3px solid $quoteBorder;color:$quoteText;background:$quoteBackground}
                            </style></head><body>$html</body></html>"""
                        loadDataWithBaseURL(article.canonicalUrl, document, "text/html", "UTF-8", null)
                    } else loadUrl(article.canonicalUrl)
                }
            },
            update = { webView ->
                if (webView.tag != styleKey) {
                    webView.tag = styleKey
                    val html = article.contentHtml?.takeIf(String::isNotBlank)
                    if (html != null) {
                        val document = """<!doctype html><html><head><meta name="viewport" content="width=device-width, initial-scale=1"><style>
                            body{font-family:sans-serif;line-height:1.7;padding:20px 18px 40px;color:$pageText;background:$pageBackground}
                            blockquote{margin-left:0;padding:8px 0 8px 14px;border-left:3px solid $quoteBorder;color:$quoteText;background:$quoteBackground}
                            img,video,iframe{max-width:100%;height:auto}pre,code{white-space:pre-wrap;overflow-wrap:anywhere}
                            </style></head><body>$html</body></html>"""
                        webView.loadDataWithBaseURL(article.canonicalUrl, document, "text/html", "UTF-8", null)
                    } else webView.loadUrl(article.canonicalUrl)
                }
            },
        )
    }
}


private fun Color.toCssHex(): String =
    String.format(java.util.Locale.US, "#%06X", 0xFFFFFF and toArgb())
