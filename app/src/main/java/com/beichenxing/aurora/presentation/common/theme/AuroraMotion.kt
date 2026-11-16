package com.beichenxing.aurora.presentation.common.theme

import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Centralized visual/motion tokens for Phase B.
 * No screen or component should hardcode these durations, alphas or radii.
 */
object AuroraMotion {
    const val BackgroundPrimaryDurationMs = 36_000
    const val BackgroundSecondaryDurationMs = 48_000
    const val ButtonGradientDurationMs = 12_000
    const val NavSweepDurationMs = 600
    const val NavIconScaleDurationMs = 250

    const val NavSpringDampingRatio = Spring.DampingRatioNoBouncy
    const val NavSpringStiffness = Spring.StiffnessMediumLow
    const val ButtonPressDampingRatio = Spring.DampingRatioNoBouncy
    const val ButtonPressStiffness = Spring.StiffnessMedium

    val GlassBlurRadius = 20.dp
    val DialogBlurRadius = 24.dp
    val NavShadowRadius = 28.dp

    const val GlassWhiteTintAlpha = 0.10f
    const val GlassBorderAlpha = 0.15f
    const val GlassTopHighlightAlpha = 0.12f
    const val NavSweepPeakAlpha = 0.08f

    const val AuroraBandPrimaryAlpha = 0.075f
    const val AuroraBandSecondaryAlpha = 0.045f

    const val DialogScrimAlpha = 0.72f
    const val ShadowAlpha = 0.42f
    const val LoadingScrimAlpha = 0.40f
    const val ActionTintAlpha = 0.12f
    const val CardOverlayAlpha = 0.60f
    const val OutlinedBorderEnabledAlpha = 0.60f
    const val OutlinedBorderDisabledAlpha = 0.20f
    const val AuroraSubtleMintAlpha = 0.15f
    const val AuroraSubtleSkyAlpha = 0.08f
    const val AuroraSubtleLilacAlpha = 0.12f
    const val CardNameScrimMidAlpha = 0.85f
    const val ButtonGradientStartOffsetFraction = -0.20f
    const val ButtonGradientEndOffsetFraction = 1.20f

    const val ButtonPressScale = 0.98f
    const val ButtonGradientShiftFraction = 0.30f
    const val DialogEnterScaleFrom = 0.96f
    const val DialogEnterDurationMs = 250

    val DialogMaxWidth = 520.dp
    val DialogWidthFraction = 0.88f
    val DialogCornerRadius = 24.dp
    val GlassHighlightHeight = 1.dp
    val NavBarWidthFraction = 0.80f
    val NavBarMinWidth = 260.dp
    val NavBarMaxWidth = 420.dp
    val NavBarHeight = 64.dp
    val NavBarBottomMargin = 16.dp

    const val NavSweepWidthFraction = 0.24f
    const val AuroraDriftXFraction = 0.12f
    const val AuroraDriftYFraction = 0.08f
}

/**
 * Reads the system "Animator duration scale" setting once.
 * When animations are disabled the visual layer falls back to a static frame.
 */
@Composable
fun rememberSystemAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }
}