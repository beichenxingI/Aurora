package com.beichenxing.aurora.presentation.common.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Aurora Material3 dark color scheme.
 * Maps Aurora brand tokens to Material3 color roles.
 * All values come from AuroraColors — no hardcoded values here.
 */
private val AuroraDarkColorScheme = darkColorScheme(
    // Primary = Mint (main interactive elements)
    primary              = AuroraColors.Mint,
    onPrimary            = AuroraColors.BgPrimary,
    primaryContainer     = AuroraColors.BgSoft,
    onPrimaryContainer   = AuroraColors.Mint,

    // Secondary = Sky
    secondary            = AuroraColors.Sky,
    onSecondary          = AuroraColors.BgPrimary,
    secondaryContainer   = AuroraColors.BgSoft,
    onSecondaryContainer = AuroraColors.Sky,

    // Tertiary = Lilac
    tertiary             = AuroraColors.Lilac,
    onTertiary           = AuroraColors.BgPrimary,
    tertiaryContainer    = AuroraColors.BgSoft,
    onTertiaryContainer  = AuroraColors.Lilac,

    // Error
    error                = AuroraColors.Danger,
    onError              = AuroraColors.TextPrimary,

    // Surfaces
    background           = AuroraColors.BgPrimary,
    onBackground         = AuroraColors.TextPrimary,
    surface              = AuroraColors.BgSurface,
    onSurface            = AuroraColors.TextPrimary,
    surfaceVariant       = AuroraColors.BgSoft,
    onSurfaceVariant     = AuroraColors.TextSecondary,

    // Outline
    outline              = AuroraColors.Border,
    outlineVariant       = AuroraColors.Border,

    // Scrim
    scrim                = AuroraColors.BgPrimary,
)

@Composable
fun AuroraTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars     = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = AuroraDarkColorScheme,
        typography  = Typography,
        content     = content
    )
}