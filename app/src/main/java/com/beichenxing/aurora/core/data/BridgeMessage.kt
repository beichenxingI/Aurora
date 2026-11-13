package com.beichenxing.aurora.core.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull

/**
 * Bridge message types used between Kotlin and chat_stage.html.
 *
 * Outbound = Kotlin → JS (via evaluateJavascript)
 * Inbound  = JS → Kotlin (via @JavascriptInterface)
 */
object BridgeType {
    // ─── Outbound: Kotlin → JS ─────────────────────────────────────────────
    const val SET_MESSAGES        = "setMessages"       // push full message list
    const val APPEND_TOKEN        = "appendToken"       // streaming token delta
    const val UPDATE_MESSAGE      = "updateMessage"     // update single message in place
    const val STREAM_END          = "streamEnd"         // AI generation complete
    const val STREAM_ERROR        = "streamError"       // AI generation failed
    const val SET_CHARACTER       = "setCharacter"      // push character info
    const val SET_CONFIG          = "setConfig"         // push app config
    const val SET_GENERATING      = "setGenerating"     // show/hide generating indicator
    const val SCROLL_TO_BOTTOM    = "scrollToBottom"    // scroll chat to bottom

    // ─── Inbound: JS → Kotlin ──────────────────────────────────────────────
    const val READY               = "ready"             // WebView loaded and ready
    const val USER_INPUT          = "userInput"         // user submitted a message
    const val STOP_GENERATION     = "stopGeneration"    // user cancelled generation
    const val LOG                 = "log"               // debug log from JS
    const val ERROR               = "error"             // error from JS
    const val SCROLL_HEIGHT       = "scrollHeight"      // JS reporting scroll position
    const val KEYBOARD_INSETS     = "keyboardInsets"    // keyboard visibility change
    const val TOP_BAR_VISIBILITY  = "topBarVisibility"  // top bar show/hide request
}

/**
 * A bridge message envelope.
 * Both directions use the same format: { type, payload }
 */
@Serializable
data class BridgeEnvelope(
    val type: String,
    val payload: JsonElement = JsonNull
)