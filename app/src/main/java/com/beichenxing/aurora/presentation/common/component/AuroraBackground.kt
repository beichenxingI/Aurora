package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.EaseInOut
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion

/**
 * Aurora animated background.
 *
 * Three low-saturation radial light blobs drift slowly across a deep navy base.
 * Colors: Mint, Sky, Lilac — all at very low alpha so they read as a subtle
 * polar-aurora glow, not a neon gradient.
 *
 * Blob 1 (Mint):  45-second cycle, drifts diagonally top-left ↔ center
 * Blob 2 (Sky):   58-second cycle, drifts horizontally center ↔ right
 * Blob 3 (Lilac): 50-second cycle, drifts diagonally bottom-right ↔ center-left
 *
 * Max alpha per blob: 0.10 (Mint/Sky), 0.07 (Lilac) — intentionally dim.
 * No flicker, no brightness change, no looping color shift.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    // External nudge: nav selection can gently push blob 2 horizontally.
    // Value is a normalized offset [-1, 1]; 0 = center position.
    blobNudge: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    // Check system animation scale — respect "disable animations"
    val context = LocalContext.current
    val animScale = remember {
        android.provider.Settings.Global.getFloat(
            context.contentResolver,
            android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    val animEnabled = animScale > 0f

    // ── Blob animation drivers ────────────────────────────────────────────
    val transition = if (animEnabled) {
        rememberInfiniteTransition(label = "aurora_bg")
    } else null

    // Blob 1 — Mint, top-left drift
    val blob1X by if (transition != null) {
        transition.animateFloat(
            initialValue   = 0.05f,
            targetValue    = 0.35f,
            animationSpec  = infiniteRepeatable(
                animation    = tween(
                    AuroraMotion.BackgroundBlob1DurationMs,
                    easing = EaseInOut
                ),
                repeatMode   = RepeatMode.Reverse
            ),
            label = "blob1x"
        )
    } else remember { mutableFloatStateOf(0.18f) }

    val blob1Y by if (transition != null) {
        transition.animateFloat(
            initialValue  = 0.08f,
            targetValue   = 0.30f,
            animationSpec = infiniteRepeatable(
                animation   = tween(
                    AuroraMotion.BackgroundBlob1DurationMs,
                    easing = EaseInOut
                ),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "blob1y"
        )
    } else remember { mutableFloatStateOf(0.18f) }

    // Blob 2 — Sky, horizontal center drift + nav nudge
    val blob2X by if (transition != null) {
        transition.animateFloat(
            initialValue  = 0.42f,
            targetValue   = 0.68f,
            animationSpec = infiniteRepeatable(
                animation   = tween(
                    AuroraMotion.BackgroundBlob2DurationMs,
                    easing = EaseInOut
                ),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "blob2x"
        )
    } else remember { mutableFloatStateOf(0.55f) }

    val blob2Y by if (transition != null) {
        transition.animateFloat(
            initialValue  = 0.35f,
            targetValue   = 0.60f,
            animationSpec = infiniteRepeatable(
                animation   = tween(
                    AuroraMotion.BackgroundBlob2DurationMs,
                    easing = EaseInOut
                ),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "blob2y"
        )
    } else remember { mutableFloatStateOf(0.48f) }

    // Blob 3 — Lilac, bottom drift
    val blob3X by if (transition != null) {
        transition.animateFloat(
            initialValue  = 0.60f,
            targetValue   = 0.88f,
            animationSpec = infiniteRepeatable(
                animation   = tween(
                    AuroraMotion.BackgroundBlob3DurationMs,
                    easing = EaseInOut
                ),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "blob3x"
        )
    } else remember { mutableFloatStateOf(0.75f) }

    val blob3Y by if (transition != null) {
        transition.animateFloat(
            initialValue  = 0.60f,
            targetValue   = 0.85f,
            animationSpec = infiniteRepeatable(
                animation   = tween(
                    AuroraMotion.BackgroundBlob3DurationMs,
                    easing = EaseInOut
                ),
                repeatMode  = RepeatMode.Reverse
            ),
            label = "blob3y"
        )
    } else remember { mutableFloatStateOf(0.72f) }

    // Nav nudge applied to blob 2 (normalized to 0–1 screen coords)
    // Max nudge: 8% of screen width, clamped
    val nudgeClamped = blobNudge.coerceIn(-1f, 1f) *
        AuroraMotion.BackgroundBlobNudgeMaxFraction
    val blob2XNudged = (blob2X + nudgeClamped).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraColors.BgPrimary)
    ) {
        // Aurora canvas layer — drawn behind all content
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawAuroraBlob(
                centerFracX  = blob1X,
                centerFracY  = blob1Y,
                radiusFrac   = AuroraMotion.BackgroundBlob1RadiusFraction,
                color        = AuroraColors.Mint,
                alpha        = AuroraMotion.AuroraBandPrimaryAlpha
            )

            drawAuroraBlob(
                centerFracX  = blob2XNudged,
                centerFracY  = blob2Y,
                radiusFrac   = AuroraMotion.BackgroundBlob2RadiusFraction,
                color        = AuroraColors.Sky,
                alpha        = AuroraMotion.AuroraBandPrimaryAlpha *
                    AuroraMotion.BackgroundBlob2AlphaFactor
            )

            drawAuroraBlob(
                centerFracX  = blob3X,
                centerFracY  = blob3Y,
                radiusFrac   = AuroraMotion.BackgroundBlob3RadiusFraction,
                color        = AuroraColors.Lilac,
                alpha        = AuroraMotion.AuroraBandSecondaryAlpha
            )
        }

        // Page content
        content()
    }
}

/**
 * Draw a single soft radial blob.
 * [centerFracX] and [centerFracY] are fractions of canvas size (0–1).
 * [radiusFrac] is the blob radius as a fraction of canvas min dimension.
 */
private fun DrawScope.drawAuroraBlob(
    centerFracX : Float,
    centerFracY : Float,
    radiusFrac  : Float,
    color       : Color,
    alpha       : Float
) {
    val cx     = size.width  * centerFracX
    val cy     = size.height * centerFracY
    val radius = size.minDimension * radiusFrac

    drawCircle(
        brush = Brush.radialGradient(
            colors      = listOf(
                color.copy(alpha = alpha),
                color.copy(
                    alpha = alpha * AuroraMotion.BackgroundBlobFalloffMidFactor
                ),
                AuroraColors.Transparent
            ),
            center      = Offset(cx, cy),
            radius      = radius,
            tileMode    = TileMode.Clamp
        ),
        radius = radius,
        center = Offset(cx, cy)
    )
}