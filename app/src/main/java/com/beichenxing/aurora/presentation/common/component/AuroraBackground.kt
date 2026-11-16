package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.draw.drawWithCache
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import com.beichenxing.aurora.presentation.common.theme.rememberSystemAnimationsEnabled

/**
 * Slow, restrained aurora backdrop.
 *
 * Two large gradient light bands drift diagonally at very low alpha.
 * No radial blobs, no bokeh, no brightness changes.
 * When the system animation scale is off, a static mid-frame is used.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val animationsEnabled = rememberSystemAnimationsEnabled()

    val primaryProgress: State<Float>
    val secondaryProgress: State<Float>

    if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "aurora_background")
        primaryProgress = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = AuroraMotion.BackgroundPrimaryDurationMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_primary_drift"
        )
        secondaryProgress = transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = AuroraMotion.BackgroundSecondaryDurationMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_secondary_drift"
        )
    } else {
        primaryProgress = remember { mutableFloatStateOf(0.5f) }
        secondaryProgress = remember { mutableFloatStateOf(0.5f) }
    }

    Box(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .drawWithCache {
                    val primaryBrush = Brush.linearGradient(
                        colors = listOf(
                            AuroraColors.Transparent,
                            AuroraColors.Mint.copy(
                                alpha = AuroraMotion.AuroraBandPrimaryAlpha
                            ),
                            AuroraColors.Sky.copy(
                                alpha = AuroraMotion.AuroraBandPrimaryAlpha
                            ),
                            AuroraColors.Transparent
                        ),
                        start = Offset(-size.width * 0.10f, size.height * 0.15f),
                        end = Offset(size.width * 1.10f, size.height * 1.05f)
                    )
                    val secondaryBrush = Brush.linearGradient(
                        colors = listOf(
                            AuroraColors.Transparent,
                            AuroraColors.Sky.copy(
                                alpha = AuroraMotion.AuroraBandSecondaryAlpha
                            ),
                            AuroraColors.Lilac.copy(
                                alpha = AuroraMotion.AuroraBandSecondaryAlpha
                            ),
                            AuroraColors.Transparent
                        ),
                        start = Offset(size.width * 1.10f, -size.height * 0.10f),
                        end = Offset(-size.width * 0.10f, size.height * 1.10f)
                    )

                    onDrawBehind {
                        drawRect(AuroraColors.BgPrimary)

                        val primary = primaryProgress.value - 0.5f
                        val primaryX =
                            primary * size.width * AuroraMotion.AuroraDriftXFraction
                        val primaryY =
                            primary * size.height * AuroraMotion.AuroraDriftYFraction
                        translate(left = primaryX, top = primaryY) {
                            drawRect(
                                brush = primaryBrush,
                                topLeft = Offset(
                                    -size.width * 0.20f,
                                    -size.height * 0.20f
                                ),
                                size = Size(
                                    size.width * 1.40f,
                                    size.height * 1.40f
                                )
                            )
                        }

                        val secondary = secondaryProgress.value - 0.5f
                        val secondaryX =
                            -secondary * size.width * AuroraMotion.AuroraDriftXFraction
                        val secondaryY =
                            secondary * size.height * AuroraMotion.AuroraDriftYFraction
                        translate(left = secondaryX, top = secondaryY) {
                            drawRect(
                                brush = secondaryBrush,
                                topLeft = Offset(
                                    -size.width * 0.20f,
                                    -size.height * 0.20f
                                ),
                                size = Size(
                                    size.width * 1.40f,
                                    size.height * 1.40f
                                )
                            )
                        }
                    }
                }
        ) {
            // Drawing is handled by drawWithCache.onDrawBehind.
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}