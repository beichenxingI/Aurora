package com.beichenxing.aurora.presentation.common.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset

/**
 * Aurora brand color palette.
 * All UI code must reference these tokens — no hardcoded color values anywhere.
 *
 * Phase B upgrade points:
 *  - AuroraGradient will be replaced by an animated version (see AuroraAnimatedGradient)
 *  - Glass surfaces will use BlurMask + alpha compositing
 */
object AuroraColors {

    // ── Brand ─────────────────────────────────────────────────────────────────
    // Core aurora spectrum: used for accents, gradients, highlights
    val Mint   = Color(0xFF65D6BE)   // primary accent
    val Sky    = Color(0xFF79CDE0)   // secondary accent
    val Lilac  = Color(0xFFA99ADD)   // tertiary accent
    val Sage   = Color(0xFF91CDA5)   // subtle success / soft accent

    // ── Dark theme surfaces ───────────────────────────────────────────────────
    val BgPrimary  = Color(0xFF0F1B2A)   // night sky — main background
    val BgSurface  = Color(0xFF162534)   // cards and list items
    val BgSoft     = Color(0xFF1C2E3F)   // modals, bottom sheets, overlays
    val Border     = Color(0xFF243648)   // dividers and outlines

    // ── Text ──────────────────────────────────────────────────────────────────
    val TextPrimary   = Color(0xFFE4F3ED)
    val TextSecondary = Color(0xFFA9BDB4)
    val TextMuted     = Color(0xFF82988E)

    // ── Semantic ──────────────────────────────────────────────────────────────
    val Danger  = Color(0xFFC44F59)
    val Success = Color(0xFF278B69)
    val Warning = Color(0xFFA66B20)
    val Info    = Color(0xFF397DA5)

    // ── Glass ─────────────────────────────────────────────────────────────────
    // Phase B: replace with actual blur compositing
    val GlassBg     = Color(0x801C2E3F)   // 50 % BgSoft
    val GlassBorder = Color(0x26FFFFFF)   // 15 % white

    // ── Utility ───────────────────────────────────────────────────────────────
    val Transparent = Color(0x00000000)

    // ── Gradients ─────────────────────────────────────────────────────────────
    /**
     * Static aurora gradient used in Phase A.
     * Phase B: replaced by AuroraAnimatedGradient (animated flowing version).
     *
     * Direction: top-left → bottom-right, matching the app icon.
     */
    val AuroraGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(Mint, Sky, Lilac),
            start = Offset(0f, 0f),
            end = Offset(1000f, 1000f)
        )

    /**
     * Subtle gradient for card overlays and scrim effects.
     */
    val AuroraSubtleGradient: Brush
        get() = Brush.linearGradient(
            colors = listOf(
                Mint.copy(alpha = 0.15f),
                Sky.copy(alpha = 0.08f),
                Lilac.copy(alpha = 0.12f)
            ),
            start = Offset(0f, 0f),
            end = Offset(800f, 800f)
        )

    /**
     * Bottom-to-transparent scrim used under character card names.
     */
    val CardNameScrim: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color.Transparent,
                BgPrimary.copy(alpha = 0.85f),
                BgPrimary
            )
        )
}