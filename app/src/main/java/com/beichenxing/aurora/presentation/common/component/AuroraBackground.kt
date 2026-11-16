package com.beichenxing.aurora.presentation.common.component

import android.provider.Settings
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring

/**
 * Aurora Mesh Gradient Background.
 *
 * Renders a full-screen mesh gradient using bilinear interpolation across
 * a 4-corner color grid. The colors slowly drift between aurora-tinted dark
 * tones, producing the soft "color embedded in darkness" effect — no visible
 * light blobs, no hard edges, just a living color field.
 *
 * Visual design:
 * - Base: warm deep navy (#141C2B), not pure black
 * - Four corners hold aurora-tinted dark colors (mint / sky / lilac / warm)
 * - Colors interpolate smoothly across the full screen
 * - Each corner animates independently on a long cycle (40–70s)
 * - On nav change: a directional impulse nudges the color field,
 *   then springs back — giving the sensation of the background
 *   responding to navigation
 *
 * Mesh resolution: 24×24 cells — enough for smooth gradients,
 * cheap enough for 60fps on any modern device.
 *
 * @param navImpulse      Normalized [-1,1]. Positive = nav moved right.
 * @param impulseVersion  Increments each time an impulse should fire.
 */
@Composable
fun AuroraBackground(
    modifier       : Modifier = Modifier,
    navImpulse     : Float    = 0f,
    impulseVersion : Int      = 0,
    content        : @Composable BoxScope.() -> Unit
) {
    val context    = LocalContext.current
    val animScale  = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    val animEnabled = animScale > 0f

    val transition = if (animEnabled) {
        rememberInfiniteTransition(label = "mesh_aurora")
    } else null

    // ── Per-corner color phase animations ────────────────────────────────
    // Each corner oscillates between two dark aurora-tinted colors.
    // Different durations create slow, organic drift.

    // Top-left: MeshMint <-> MeshNeutral (55s)
    val tlPhase by animFloat(transition, 0f, 1f, 55_000)

    // Top-right: MeshSky <-> MeshWarm (68s)
    val trPhase by animFloat(transition, 1f, 0f, 68_000)

    // Bottom-left: MeshLilac <-> MeshNeutral (47s)
    val blPhase by animFloat(transition, 0f, 1f, 47_000)

    // Bottom-right: MeshSky <-> MeshMint (61s)
    val brPhase by animFloat(transition, 1f, 0f, 61_000)

    // Center: MeshWarm <-> MeshLilac (53s)
    val cPhase by animFloat(transition, 0f, 1f, 53_000)

    // ── Nav impulse as UV offset ──────────────────────────────────────────
    // A horizontal impulse shifts the mesh UV coordinates slightly,
    // making the color field appear to "slide" in the opposite direction.
    val impulseOffset = remember { Animatable(0f) }

    LaunchedEffect(impulseVersion) {
        if (navImpulse == 0f) return@LaunchedEffect
        val target = -navImpulse * 0.06f   // max 6% UV shift
        impulseOffset.snapTo(target)
        impulseOffset.animateTo(
            targetValue   = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness    = Spring.StiffnessLow
            )
        )
    }

    val uShift = impulseOffset.value

    // ── Compute current corner colors ─────────────────────────────────────
    val colorTL = lerpColor(AuroraColors.MeshMint,  AuroraColors.MeshNeutral, tlPhase)
    val colorTR = lerpColor(AuroraColors.MeshSky,   AuroraColors.MeshWarm,    trPhase)
    val colorBL = lerpColor(AuroraColors.MeshLilac, AuroraColors.MeshNeutral, blPhase)
    val colorBR = lerpColor(AuroraColors.MeshSky,   AuroraColors.MeshMint,    brPhase)
    val colorC  = lerpColor(AuroraColors.MeshWarm,  AuroraColors.MeshLilac,   cPhase)

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawMeshGradient(
                tl     = colorTL,
                tr     = colorTR,
                bl     = colorBL,
                br     = colorBR,
                center = colorC,
                uShift = uShift,
                cells  = 24
            )
        }
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Mesh gradient renderer
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Draw a mesh gradient across the full canvas.
 *
 * Uses a 5-point color grid (4 corners + 1 center) with bilinear
 * interpolation. The center point blends in to break the strict linearity
 * of a pure 4-corner gradient, adding more organic variation.
 *
 * @param tl, tr, bl, br  Corner colors
 * @param center          Center color (blended in with weight 0.35)
 * @param uShift          Horizontal UV offset from nav impulse
 * @param cells           Grid resolution (higher = smoother, more draw calls)
 */
private fun DrawScope.drawMeshGradient(
    tl    : Color,
    tr    : Color,
    bl    : Color,
    br    : Color,
    center: Color,
    uShift: Float,
    cells : Int
) {
    val cellW = size.width  / cells
    val cellH = size.height / cells

    for (row in 0 until cells) {
        for (col in 0 until cells) {
            // UV coordinates of cell center, shifted by nav impulse
            val u = ((col + 0.5f) / cells + uShift).coerceIn(0f, 1f)
            val v = (row  + 0.5f) / cells

            // Bilinear interpolation across 4 corners
            val cornerColor = bilerp(tl, tr, bl, br, u, v)

            // Distance from center (0 at center, 1 at corners)
            val du = u - 0.5f
            val dv = v - 0.5f
            val distFromCenter = (du * du + dv * dv) * 4f   // 0..1
            val centerWeight   = (1f - distFromCenter).coerceIn(0f, 1f) * 0.35f

            // Blend in center color
            val finalColor = lerpColor(cornerColor, center, centerWeight)

            drawRect(
                color   = finalColor,
                topLeft = Offset(col * cellW, row * cellH),
                size    = Size(cellW + 1f, cellH + 1f)   // +1 avoids hair-line gaps
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  Color helpers
// ─────────────────────────────────────────────────────────────────────────────

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tc = t.coerceIn(0f, 1f)
    return Color(
        red   = a.red   + (b.red   - a.red)   * tc,
        green = a.green + (b.green - a.green) * tc,
        blue  = a.blue  + (b.blue  - a.blue)  * tc,
        alpha = 1f
    )
}

private fun bilerp(
    tl: Color, tr: Color,
    bl: Color, br: Color,
    u : Float, v : Float
): Color {
    val top    = lerpColor(tl, tr, u)
    val bottom = lerpColor(bl, br, u)
    return lerpColor(top, bottom, v)
}

/** Animated float, or static mid-value when system animations disabled. */
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
            label = "mesh_${from}_${to}"
        )
    } else {
        remember { mutableFloatStateOf((from + to) / 2f) }
    }
}