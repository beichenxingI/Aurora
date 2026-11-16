package com.beichenxing.aurora.presentation.common.component

import android.provider.Settings
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.InfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion

/**
 * Aurora animated background — three elongated aurora light bands
 * drifting slowly across a deep navy base.
 *
 * Visual design:
 * - Each blob is drawn as a wide ellipse (scaleX >> scaleY) with a radial gradient
 * - Slight rotation gives each band a natural tilt
 * - alpha is intentionally low-to-mid so backgrounds don't overwhelm content
 * - Colors are saturated but the dark navy base keeps overall brightness low
 *
 * @param blobNudge  normalized [-1, 1] nudge applied to blob 2's X position.
 *                   Used by nav bar selection to gently shift the sky band.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    blobNudge: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val context = LocalContext.current
    val animScale = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    val animEnabled = animScale > 0f

    val transition = if (animEnabled) {
        rememberInfiniteTransition(label = "aurora")
    } else null

    // ── Blob 1: Mint band, top-left ───────────────────────────────────────
    val b1x by animFloat(
        transition,
        0.08f,
        0.38f,
        AuroraMotion.BackgroundBlob1DurationMs
    )
    val b1y by animFloat(
        transition,
        0.05f,
        0.32f,
        AuroraMotion.BackgroundBlob1DurationMs
    )

    // ── Blob 2: Sky band, center ──────────────────────────────────────────
    val b2x by animFloat(
        transition,
        0.38f,
        0.70f,
        AuroraMotion.BackgroundBlob2DurationMs
    )
    val b2y by animFloat(
        transition,
        0.30f,
        0.58f,
        AuroraMotion.BackgroundBlob2DurationMs
    )

    // ── Blob 3: Lilac band, bottom-right ──────────────────────────────────
    val b3x by animFloat(
        transition,
        0.55f,
        0.90f,
        AuroraMotion.BackgroundBlob3DurationMs
    )
    val b3y by animFloat(
        transition,
        0.55f,
        0.82f,
        AuroraMotion.BackgroundBlob3DurationMs
    )

    // Nav nudge: blob 2 shifts ±8% of screen width toward selected tab
    val nudge = blobNudge.coerceIn(-1f, 1f) *
        AuroraMotion.BackgroundBlobNudgeMaxFraction
    val b2xNudged = (b2x + nudge).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraColors.BgPrimary)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // Band 1 — Mint, top-left, tilted -12°
            drawAuroraBand(
                cx        = size.width  * b1x,
                cy        = size.height * b1y,
                radiusX   = size.width  * 0.55f,
                radiusY   = size.height * 0.18f,
                rotation  = -12f,
                color     = AuroraColors.Mint,
                alpha     = AuroraMotion.AuroraBandMintAlpha
            )

            // Band 2 — Sky, center, tilted +8°
            drawAuroraBand(
                cx        = size.width  * b2xNudged,
                cy        = size.height * b2y,
                radiusX   = size.width  * 0.65f,
                radiusY   = size.height * 0.20f,
                rotation  = 8f,
                color     = AuroraColors.Sky,
                alpha     = AuroraMotion.AuroraBandSkyAlpha
            )

            // Band 3 — Lilac, bottom-right, tilted -6°
            drawAuroraBand(
                cx        = size.width  * b3x,
                cy        = size.height * b3y,
                radiusX   = size.width  * 0.50f,
                radiusY   = size.height * 0.16f,
                rotation  = -6f,
                color     = AuroraColors.Lilac,
                alpha     = AuroraMotion.AuroraBandLilacAlpha
            )
        }

        content()
    }
}

/**
 * Draw a single aurora band — a rotated, elongated radial gradient.
 *
 * The band is drawn by scaling the canvas so the circle appears elliptical:
 *   - scaleX = 1
 *   - scaleY = radiusY / radiusX
 * Then a radial gradient is drawn at the scaled center.
 *
 * The gradient has four stops: full alpha at center → half alpha at 45% →
 * faint at 75% → transparent at edge.
 */
private fun DrawScope.drawAuroraBand(
    cx       : Float,
    cy       : Float,
    radiusX  : Float,
    radiusY  : Float,
    rotation : Float,
    color    : Color,
    alpha    : Float
) {
    if (radiusX <= 0f || radiusY <= 0f) return

    val baseRadius = radiusX

    rotate(degrees = rotation, pivot = Offset(cx, cy)) {
        scale(
            scaleX = 1f,
            scaleY = radiusY / radiusX,
            pivot  = Offset(cx, cy)
        ) {
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0.00f to color.copy(alpha = alpha),
                        0.45f to color.copy(
                            alpha = alpha * AuroraMotion.AuroraBandMidAlphaFactor
                        ),
                        0.75f to color.copy(
                            alpha = alpha * AuroraMotion.AuroraBandOuterAlphaFactor
                        ),
                        1.00f to AuroraColors.Transparent
                    ),
                    center   = Offset(cx, cy),
                    radius   = baseRadius,
                    tileMode = TileMode.Clamp
                ),
                radius = baseRadius,
                center = Offset(cx, cy)
            )
        }
    }
}

/**
 * Helper: returns an animated float if [transition] is non-null,
 * or a static mid-value State if animations are disabled.
 */
@Composable
private fun animFloat(
    transition : InfiniteTransition?,
    from       : Float,
    to         : Float,
    durationMs : Int
): State<Float> {
    return if (transition != null) {
        transition.animateFloat(
            initialValue  = from,
            targetValue   = to,
            animationSpec = infiniteRepeatable(
                animation  = tween(durationMs, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_${from}_${to}"
        )
    } else {
        remember { mutableFloatStateOf((from + to) / 2f) }
    }
}