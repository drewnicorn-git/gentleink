package com.gentleink.reader.ui

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun EpubWebView(
    html: String,
    modifier: Modifier = Modifier,
    onWebViewReady: (WebView) -> Unit = {}
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = false
                settings.allowFileAccess = false
                webViewClient = WebViewClient()
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                onWebViewReady(this)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
        }
    )
}
