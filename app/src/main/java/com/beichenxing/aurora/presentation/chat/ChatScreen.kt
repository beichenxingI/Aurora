package com.beichenxing.aurora.presentation.chat

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.beichenxing.aurora.core.utils.BridgeShim
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            configureWebView(this)
        }
    }

    DisposableEffect(webView) {
        val bridge = KotlinBridge(
            scope = MainScope(),
            onReady = { viewModel.onWebViewReady() },
            onUserInput = { text -> viewModel.onUserInput(text) },
            onStopGeneration = { viewModel.stopGeneration() }
        )
        webView.addJavascriptInterface(bridge, "__kiraAndroidBridge")
        webView.loadUrl("file:///android_asset/chat/chat_stage.html")

        onDispose {
            webView.removeJavascriptInterface("__kiraAndroidBridge")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChatEvent.SendToWebView -> {
                    val js = buildJsDispatch(event.type, event.payloadJson)
                    Timber.d("[Bridge →] type=${event.type}")
                    webView.evaluateJavascript(js, null)
                }
                is ChatEvent.ScrollToBottom -> {
                    webView.evaluateJavascript(
                        "window.scrollTo(0,document.body.scrollHeight);", null
                    )
                }
            }
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    // No Scaffold here — parent Scaffold (AuroraNavGraph) already provides one.
    // Using Column to stack TopAppBar + WebView + SnackbarHost.
    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = uiState.character?.name ?: "聊天",
                    style = MaterialTheme.typography.titleMedium
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { webView },
                modifier = Modifier.fillMaxSize()
            )
        }

        SnackbarHost(snackbarHostState)
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun configureWebView(webView: WebView) {
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        mediaPlaybackRequiresUserGesture = false
        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        cacheMode = WebSettings.LOAD_DEFAULT
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            // Inject shim after page loads
            view?.evaluateJavascript(BridgeShim.getShimScript()) { result ->
                Timber.d("[BridgeShim] injected: $result")
            }
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onConsoleMessage(
            message: android.webkit.ConsoleMessage
        ): Boolean {
            Timber.d("[JS] ${message.message()} (${message.sourceId()}:${message.lineNumber()})")
            return true
        }
    }

    webView.setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
}

private fun buildJsDispatch(type: String, payloadJson: String): String = """
    (function() {
        try {
            var msg = { type: '$type', payload: $payloadJson };
            if (window.__bridgeDispatch) {
                window.__bridgeDispatch(JSON.stringify(msg));
            } else if (window.__kiraReceive) {
                window.__kiraReceive(JSON.stringify(msg));
            } else {
                console.warn('[Aurora] No bridge dispatcher for: $type');
            }
        } catch(e) {
            console.error('[Aurora] dispatch error:', e);
        }
    })();
""".trimIndent()