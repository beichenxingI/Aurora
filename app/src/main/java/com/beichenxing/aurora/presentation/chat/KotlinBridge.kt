package com.beichenxing.aurora.presentation.chat

import android.webkit.JavascriptInterface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber

/**
 * Android @JavascriptInterface registered as window.__auroraBridge.
 *
 * Aurora Chat Protocol v1:
 *   JS → Kotlin: window.__auroraBridge.onMessage(jsonString)
 *   Kotlin → JS: window.__auroraDispatch(type, payloadObject)
 *
 * Message format: { "type": "...", "payload": { ... } }
 */
class KotlinBridge(
    private val scope: CoroutineScope,
    private val onReady: () -> Unit,
    private val onUserInput: (text: String) -> Unit,
    private val onStopGeneration: () -> Unit
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @JavascriptInterface
    fun onMessage(jsonString: String) {
        try {
            val root = json.parseToJsonElement(jsonString).jsonObject
            val type = root["type"]?.jsonPrimitive?.content ?: return
            val payload = root["payload"]?.jsonObject

            Timber.d("[Bridge ←] $type")

            scope.launch {
                when (type) {
                    "ready" -> {
                        Timber.i("[Bridge] WebView ready")
                        onReady()
                    }

                    "userInput" -> {
                        val text = payload?.get("text")?.jsonPrimitive?.content ?: return@launch
                        if (text.isNotBlank()) {
                            onUserInput(text)
                        }
                    }

                    "stopGeneration" -> {
                        onStopGeneration()
                    }

                    else -> {
                        Timber.v("[Bridge] Unhandled: $type")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "[Bridge] Parse error: $jsonString")
        }
    }
}