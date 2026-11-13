package com.beichenxing.aurora.core.utils

/**
 * JavaScript compatibility shim injected into the WebView after page load.
 *
 * chat_stage.html was originally written for flutter_inappwebview, which uses:
 *   window.flutter_inappwebview.callHandler('bridge', jsonString)
 *
 * Android native WebView uses @JavascriptInterface, which works like:
 *   window.__kiraAndroidBridge.onMessage(jsonString)
 *
 * This shim patches window.flutter_inappwebview so that chat_stage.html
 * works without any modification.
 */
object BridgeShim {

    fun getShimScript(): String = """
    (function() {
        // Patch flutter_inappwebview
        window.flutter_inappwebview = {
            callHandler: function(handlerName, data) {
                try {
                    if (window.__kiraAndroidBridge) {
                        var payload = typeof data === 'string'
                            ? data
                            : JSON.stringify(data);
                        window.__kiraAndroidBridge.onMessage(payload);
                    }
                } catch(e) {
                    console.error('[BridgeShim] callHandler error:', e);
                }
            }
        };
        
        // Patch sendToFlutter
        window.sendToFlutter = function(type, payload) {
            try {
                if (window.__kiraAndroidBridge) {
                    var msg = JSON.stringify({
                        type: type,
                        payload: payload || {}
                    });
                    window.__kiraAndroidBridge.onMessage(msg);
                }
            } catch(e) {
                console.error('[BridgeShim] sendToFlutter error:', e);
            }
        };
        
        console.log('[BridgeShim] installed');
        
        // If the page already fired ready before shim was installed,
        // fire it again now so Kotlin knows the WebView is ready.
        try {
            if (window.__kiraAndroidBridge) {
                window.__kiraAndroidBridge.onMessage(
                    JSON.stringify({ type: 'ready', payload: {} })
                );
            }
        } catch(e) {
            console.error('[BridgeShim] ready re-fire error:', e);
        }
    })();
""".trimIndent()
}