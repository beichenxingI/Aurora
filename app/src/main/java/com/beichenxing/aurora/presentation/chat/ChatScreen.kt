package com.beichenxing.aurora.presentation.chat

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
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
    val uiState         by viewModel.uiState.collectAsState()
    val snackbarState   = remember { SnackbarHostState() }
    val context         = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var showCopied by remember { mutableStateOf(false) }

    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            settings.apply {
                javaScriptEnabled              = true
                domStorageEnabled              = true
                allowFileAccess                = true
                allowContentAccess             = true
                mediaPlaybackRequiresUserGesture = false
                mixedContentMode               = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                setSupportZoom(false)
                builtInZoomControls            = false
                displayZoomControls            = false
            }
            setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

            webViewClient = object : WebViewClient() {
                override fun onReceivedError(
                    view: WebView?, errorCode: Int,
                    description: String?, failingUrl: String?
                ) { Timber.e("[WebView] Error $errorCode: $description") }
            }

            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(
                    message: android.webkit.ConsoleMessage
                ): Boolean {
                    Timber.d("[JS] ${message.message()} (line ${message.lineNumber()})")
                    return true
                }
            }
        }
    }

    DisposableEffect(webView) {
        val bridge = KotlinBridge(
            scope               = MainScope(),
            onReady             = { viewModel.onWebViewReady() },
            onUserInput         = { text -> viewModel.onUserInput(text) },
            onStopGeneration    = { viewModel.stopGeneration() },
            onMessageLongPress  = { id -> viewModel.onMessageLongPress(id) }
        )
        webView.addJavascriptInterface(bridge, "__auroraBridge")
        webView.loadUrl("file:///android_asset/chat/index.html")
        onDispose { webView.removeJavascriptInterface("__auroraBridge") }
    }

    // Forward ViewModel events to WebView
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is ChatEvent.SendToWebView -> {
                    Timber.d("[Bridge →] ${event.type}")
                    webView.evaluateJavascript(
                        "window.__auroraDispatch('${event.type}', ${event.payloadJson});",
                        null
                    )
                }
                is ChatEvent.ScrollToBottom -> {
                    webView.evaluateJavascript(
                        "document.getElementById('message-list')?.scrollTo(0, 999999);",
                        null
                    )
                }
            }
        }
    }

    // Error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text     = uiState.character?.name ?: "聊天",
                    style    = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector        = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回"
                    )
                }
            }
        )

        Box(modifier = Modifier.weight(1f)) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory  = { webView },
                modifier = Modifier.fillMaxSize()
            )
        }

        SnackbarHost(snackbarState)
    }

    // Copy snackbar trigger
    LaunchedEffect(showCopied) {
        if (showCopied) {
            snackbarState.showSnackbar("已复制")
            showCopied = false
        }
    }

    // ── Message action dialog ─────────────────────────────────────────────
    uiState.actionTarget?.let { message ->
        MessageActionDialog(
            message        = message,
            onRetry        = { viewModel.retryFromMessage(message.id) },
            onEdit         = { viewModel.beginEditMessage(message.id) },
            onCopy         = {
                clipboardManager.setText(AnnotatedString(message.currentContent))
                viewModel.dismissActionSheet()
                showCopied = true
            },
            onDelete       = { viewModel.requestDeleteMessage(message.id) },
            onDismiss      = { viewModel.dismissActionSheet() }
        )
    }

    // ── Edit dialog ───────────────────────────────────────────────────────
    uiState.editingMessage?.let { message ->
        EditMessageDialog(
            initialContent = message.currentContent,
            onConfirm      = { viewModel.confirmEditMessage(it) },
            onDismiss      = { viewModel.cancelEditMessage() }
        )
    }

    // ── Delete confirm dialog ─────────────────────────────────────────────
    uiState.deleteTarget?.let {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDeleteMessage() },
            title            = { Text("删除消息") },
            text             = { Text("确认删除这条消息？此操作无法撤销。") },
            confirmButton    = {
                Button(
                    onClick = { viewModel.confirmDeleteMessage() },
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = AuroraColors.Danger
                    )
                ) { Text("删除") }
            },
            dismissButton    = {
                TextButton(onClick = { viewModel.cancelDeleteMessage() }) {
                    Text("取消")
                }
            }
        )
    }
}

// ─── Message Action Dialog ────────────────────────────────────────────────────

@Composable
private fun MessageActionDialog(
    message  : com.beichenxing.aurora.domain.model.Message,
    onRetry  : () -> Unit,
    onEdit   : () -> Unit,
    onCopy   : () -> Unit,
    onDelete : () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties       = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside   = true
        )
    ) {
        Surface(
            modifier  = Modifier
                .fillMaxWidth(0.78f),
            shape     = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            color     = AuroraColors.BgSoft,
            tonalElevation = 0.dp
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // Message preview
                Text(
                    text     = message.currentContent.take(80),
                    style    = MaterialTheme.typography.bodySmall,
                    color    = AuroraColors.TextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(
                        horizontal = 20.dp,
                        vertical   = 10.dp
                    )
                )

                HorizontalDivider(color = AuroraColors.Border)

                // Actions
                ActionItem(
                    icon    = Icons.Default.Refresh,
                    label   = "重新生成",
                    tint    = AuroraColors.Mint,
                    onClick = { onDismiss(); onRetry() }
                )

                ActionItem(
                    icon    = Icons.Default.Edit,
                    label   = "编辑",
                    tint    = AuroraColors.Sky,
                    onClick = { onDismiss(); onEdit() }
                )

                ActionItem(
                    icon    = Icons.Default.ContentCopy,
                    label   = "复制",
                    tint    = AuroraColors.TextSecondary,
                    onClick = { onCopy() }
                )

                ActionItem(
                    icon    = Icons.Default.Delete,
                    label   = "删除",
                    tint    = AuroraColors.Danger,
                    onClick = { onDismiss(); onDelete() }
                )
            }
        }
    }
}

@Composable
private fun ActionItem(
    icon   : ImageVector,
    label  : String,
    tint   : androidx.compose.ui.graphics.Color,
    onClick: () -> Unit
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment   = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = label,
            tint               = tint,
            modifier           = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.bodyLarge,
            color = AuroraColors.TextPrimary
        )
    }
}

// ─── Edit Message Dialog ──────────────────────────────────────────────────────

@Composable
private fun EditMessageDialog(
    initialContent: String,
    onConfirm     : (String) -> Unit,
    onDismiss     : () -> Unit
) {
    var text by remember(initialContent) { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("编辑消息") },
        text             = {
            OutlinedTextField(
                value         = text,
                onValueChange = { text = it },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(160.dp),
                label         = { Text("内容") }
            )
        },
        confirmButton    = {
            Button(onClick = { onConfirm(text) }) { Text("保存") }
        },
        dismissButton    = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}