package com.beichenxing.aurora.presentation.common.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Aurora uses a fixed dark color scheme
private val AuroraDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8C69),           // accent orange
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF3A2A20),
    onPrimaryContainer = Color(0xFFFFB89A),
    secondary = Color(0xFFA0A0A0),
    onSecondary = Color(0xFF1A1A1A),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFF0F0F0),
    surface = Color(0xFF242424),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF2E2E2E),
    onSurfaceVariant = Color(0xFFA0A0A0),
    outline = Color(0xFF3A3A3A),
    scrim = Color(0xFF000000)
)

@Composable
fun AuroraTheme(
    content: @Composable () -> Unit
) {
    // Always dark for now; theme switcher can be added later
    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        // Use dynamic dark colors on Android 12+ for Material You
        dynamicDarkColorScheme(LocalContext.current)
    } else {
        AuroraDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Status bar icons: white (light icons on dark background)
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}