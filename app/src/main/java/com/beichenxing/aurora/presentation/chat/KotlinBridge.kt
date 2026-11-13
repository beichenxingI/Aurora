package com.beichenxing.aurora.presentation.chat

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Android @JavascriptInterface bridge.
 *
 * Registered as window.__kiraAndroidBridge in WebView.
 * The BridgeShim patches window.flutter_inappwebview to forward all
 * calls to this interface, so chat_stage.html needs no modification.
 *
 * All JS calls arrive on the WebView's JS thread.
 * We dispatch work to the provided coroutine scope.
 */
class KotlinBridge(
    private val scope: CoroutineScope,
    private val onReady: () -> Unit,
    private val onUserInput: (text: String) -> Unit,
    private val onStopGeneration: () -> Unit,
    private val onLog: (message: String) -> Unit = {}
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @JavascriptInterface
    fun onMessage(jsonString: String) {
        try {
            val envelope = json.parseToJsonElement(jsonString).jsonObject
            val type = envelope["type"]?.jsonPrimitive?.content ?: return
            val payload = envelope["payload"] ?: envelope // some messages use flat format

            Timber.d("[Bridge ←] type=$type")

            scope.launch {
                when (type) {
                    "ready" -> {
                        Timber.i("[Bridge] WebView ready")
                        onReady()
                    }

                    "userInput" -> {
                        // Payload: { text: "..." }
                        val text = try {
                            payload.jsonObject["text"]?.jsonPrimitive?.content ?: ""
                        } catch (e: Exception) {
                            jsonString // fallback: use raw string
                        }
                        if (text.isNotBlank()) {
                            onUserInput(text)
                        }
                    }

                    "sendMessage" -> {
                        // Alternative key used by some versions of chat_stage.html
                        val text = try {
                            payload.jsonObject["text"]?.jsonPrimitive?.content
                                ?: payload.jsonObject["message"]?.jsonPrimitive?.content
                                ?: ""
                        } catch (e: Exception) { "" }
                        if (text.isNotBlank()) {
                            onUserInput(text)
                        }
                    }

                    "stopGeneration" -> {
                        onStopGeneration()
                    }

                    "log" -> {
                        val message = try {
                            payload.jsonObject["message"]?.jsonPrimitive?.content
                                ?: jsonString
                        } catch (e: Exception) { jsonString }
                        onLog(message)
                        Timber.d("[Bridge JS] $message")
                    }

                    "error" -> {
                        val message = try {
                            payload.jsonObject["message"]?.jsonPrimitive?.content
                                ?: jsonString
                        } catch (e: Exception) { jsonString }
                        Timber.e("[Bridge JS Error] $message")
                    }

                    else -> {
                        Timber.v("[Bridge] Unhandled type: $type")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[Bridge] Failed to parse message: $jsonString")
        }
    }
}