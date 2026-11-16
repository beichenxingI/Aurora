package com.beichenxing.aurora.presentation.common.component

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Aurora light-curtain background.
 *
 * Renders multiple diagonal light bands (like real aurora borealis curtains)
 * using linearGradient strips at various angles.
 *
 * Layout:
 *   - 7 bands total: 3 Mint, 2 Sky, 2 Lilac
 *   - Each band is a full-screen-height diagonal strip
 *   - Bands drift at different speeds (parallax feel)
 *   - On nav selection change: impulse shifts all bands in the opposite direction,
 *     then spring-decays back to base drift
 *
 * @param navImpulse  Normalized [-1, 1] directional impulse injected by nav changes.
 *                    Positive = user moved right (bands shift left), negative = user moved left.
 *                    Set to 0f when no nav change.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    navImpulse: Float = 0f,
    impulseVersion: Int = 0,     // 新增这个参数
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

    // ── Slow base drift (infinite, very slow) ────────────────────────────
    val transition = if (animEnabled) {
        rememberInfiniteTransition(label = "aurora")
    } else null

    // Three independent drift offsets for parallax
    // Each animates 0 -> 1 (fractional screen width) over a long cycle
    val drift1 by animFloat(transition, 0f, 0.12f, 55_000)
    val drift2 by animFloat(transition, 0f, 0.09f, 70_000)
    val drift3 by animFloat(transition, 0f, 0.15f, 45_000)

    // ── Impulse offset (spring-animated) ─────────────────────────────────
    // When nav changes, inject a directional impulse then spring back to 0.
    val impulseOffset = remember { Animatable(0f) }

    LaunchedEffect(impulseVersion) {
        if (navImpulse == 0f) return@LaunchedEffect
        // Snap to impulse position (bands jump toward opposite direction)
        val target = -navImpulse * 0.14f  // max 14% screen width shift
        impulseOffset.snapTo(target)
        // Spring decay back to 0 (like a sloshing fluid settling)
        impulseOffset.animateTo(
            targetValue   = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val impulse = impulseOffset.value

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AuroraColors.BgPrimary)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {

            // ── Band definitions ──────────────────────────────────────────
            // posX: center position as fraction of screen width
            // Each group uses a different drift speed for parallax

            // Mint group (fastest drift)
            drawAuroraBand(
                color   = AuroraColors.Mint,
                angle   = 115f,
                posX    = 0.10f + drift1 + impulse,
                bandW   = 0.22f,
                alpha   = 0.28f
            )
            drawAuroraBand(
                color   = AuroraColors.Mint,
                angle   = 118f,
                posX    = 0.42f + drift1 * 0.7f + impulse * 0.8f,
                bandW   = 0.18f,
                alpha   = 0.20f
            )
            drawAuroraBand(
                color   = AuroraColors.Mint,
                angle   = 112f,
                posX    = 0.75f + drift1 * 0.5f + impulse * 0.6f,
                bandW   = 0.16f,
                alpha   = 0.15f
            )

            // Sky group (medium drift)
            drawAuroraBand(
                color   = AuroraColors.Sky,
                angle   = 108f,
                posX    = 0.25f + drift2 + impulse * 0.9f,
                bandW   = 0.26f,
                alpha   = 0.24f
            )
            drawAuroraBand(
                color   = AuroraColors.Sky,
                angle   = 111f,
                posX    = 0.62f + drift2 * 0.8f + impulse * 0.7f,
                bandW   = 0.20f,
                alpha   = 0.18f
            )

            // Lilac group (slowest drift)
            drawAuroraBand(
                color   = AuroraColors.Lilac,
                angle   = 105f,
                posX    = 0.50f + drift3 + impulse * 0.85f,
                bandW   = 0.24f,
                alpha   = 0.20f
            )
            drawAuroraBand(
                color   = AuroraColors.Lilac,
                angle   = 120f,
                posX    = 0.85f + drift3 * 0.6f + impulse * 0.5f,
                bandW   = 0.19f,
                alpha   = 0.14f
            )
        }

        content()
    }
}

/**
 * Draw one aurora light curtain band.
 *
 * The band is a diagonal strip spanning the full canvas height.
 * It is rendered as a linearGradient perpendicular to the band's angle:
 *   transparent → peak color → transparent
 *
 * @param color   Band color (from AuroraColors)
 * @param angle   Band orientation in degrees (90 = vertical, 115 = tilted left)
 * @param posX    Center of band as fraction of canvas width [0..1], can exceed bounds
 * @param bandW   Width of band as fraction of canvas width
 * @param alpha   Peak alpha at band center
 */
private fun DrawScope.drawAuroraBand(
    color : Color,
    angle : Float,
    posX  : Float,
    bandW : Float,
    alpha : Float
) {
    val cx    = size.width * posX
    val halfW = size.width * bandW * 0.5f
    val h     = size.height

    // Direction perpendicular to the band (the gradient direction)
    val rad   = Math.toRadians((angle - 90.0))
    val perpX = cos(rad).toFloat()
    val perpY = sin(rad).toFloat()

    // Gradient start/end points (perpendicular to band direction)
    val gradStart = Offset(cx - perpX * halfW, h * 0.5f - perpY * halfW)
    val gradEnd   = Offset(cx + perpX * halfW, h * 0.5f + perpY * halfW)

    drawRect(
        brush   = Brush.linearGradient(
            colorStops = arrayOf(
                0.00f to color.copy(alpha = 0f),
                0.30f to color.copy(alpha = alpha * 0.5f),
                0.50f to color.copy(alpha = alpha),
                0.70f to color.copy(alpha = alpha * 0.5f),
                1.00f to color.copy(alpha = 0f)
            ),
            start = gradStart,
            end   = gradEnd
        ),
        topLeft = Offset(0f, 0f),
        size    = Size(size.width, h)
    )
}

/** Returns animated float state, or static mid-value if animations disabled. */
@Composable
private fun animFloat(
    transition : androidx.compose.animation.core.InfiniteTransition?,
    from       : Float,
    to         : Float,
    durationMs : Int
): androidx.compose.runtime.State<Float> {
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