package com.beichenxing.aurora.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.beichenxing.aurora.presentation.common.theme.AuroraColors

/**
 * Aurora background: a full-screen aurora image with a dark scrim overlay.
 *
 * Layers (bottom to top):
 *   1. AuroraColors.BgPrimary — solid fallback color shown while image loads
 *   2. aurora_bg.jpg — full-bleed aurora image at reduced alpha (0.55)
 *   3. Dark gradient scrim — BgPrimary @ 55% opacity, ensures UI readability
 *   4. content — all page UI
 *
 * The image alpha (0.55) + scrim (0.55) together reduce the aurora to about
 * 20% visual weight, present but not competing with UI elements.
 *
 * @param navImpulse      Reserved for future parallax animation.
 * @param impulseVersion  Reserved for future parallax animation.
 */
@Composable
fun AuroraBackground(
    modifier       : Modifier = Modifier,
    navImpulse     : Float    = 0f,
    impulseVersion : Int      = 0,
    content        : @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraColors.BgPrimary)   // solid fallback
    ) {
        // ── Layer 1: Aurora image ─────────────────────────────────────────
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data("file:///android_asset/bg/aurora_bg.jpg")
                .crossfade(800)
                .build(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .fillMaxSize()
                .alpha(0.55f)       // reduce image to 55% — present but not dominant
        )

        // ── Layer 2: Dark scrim gradient ──────────────────────────────────
        // Top and bottom edges darken more so status bar + nav bar areas
        // have higher contrast for text and icons.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.00f to AuroraColors.BgPrimary.copy(alpha = 0.72f),  // top: darker
                            0.20f to AuroraColors.BgPrimary.copy(alpha = 0.45f),  // upper content
                            0.50f to AuroraColors.BgPrimary.copy(alpha = 0.38f),  // center: lightest scrim
                            0.80f to AuroraColors.BgPrimary.copy(alpha = 0.45f),  // lower content
                            1.00f to AuroraColors.BgPrimary.copy(alpha = 0.70f)   // bottom: darker for nav
                        )
                    )
                )
        )

        // ── Layer 3: Page content ─────────────────────────────────────────
        content()
    }
}