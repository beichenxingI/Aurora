package com.beichenxing.aurora

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.beichenxing.aurora.presentation.common.theme.AuroraTheme
import com.beichenxing.aurora.presentation.navigation.AuroraNavGraph
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Transparent status bar and navigation bar
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Let content draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AuroraTheme {
                AuroraNavGraph()
            }
        }
    }
}