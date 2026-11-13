package com.beichenxing.aurora

import android.os.Bundle
import androidx.activity.ComponentActivity
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

        // Enable edge-to-edge: content draws behind status bar and nav bar
        enableEdgeToEdge()

        // Let the app draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            AuroraTheme {
                AuroraNavGraph()
            }
        }
    }
}