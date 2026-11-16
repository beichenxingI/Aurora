package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import com.beichenxing.aurora.presentation.common.theme.rememberSystemAnimationsEnabled

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraButton
//  Slow animated aurora gradient, press scale, ripple, accessible click target.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuroraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    val animatedScale by animateFloatAsState(
        targetValue = if (pressed) AuroraMotion.ButtonPressScale else 1f,
        animationSpec = spring(
            dampingRatio = AuroraMotion.ButtonPressDampingRatio,
            stiffness = AuroraMotion.ButtonPressStiffness
        ),
        label = "aurora_button_press"
    )

    val gradientProgress: State<Float> = if (animationsEnabled && enabled) {
        val transition = rememberInfiniteTransition(label = "aurora_button_gradient")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = AuroraMotion.ButtonGradientDurationMs,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "aurora_button_gradient_progress"
        )
    } else {
        remember { mutableFloatStateOf(0.5f) }
    }

    val shape = RoundedCornerShape(12.dp)
    val indication = LocalIndication.current
    val scale = if (animationsEnabled) animatedScale else 1f

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .drawBehind {
                if (!enabled) {
                    drawRect(AuroraColors.Border)
                } else {
                    val shift =
                        size.width * AuroraMotion.ButtonGradientShiftFraction *
                            gradientProgress.value
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AuroraColors.Mint,
                                AuroraColors.Sky,
                                AuroraColors.Lilac
                            ),
                            start = Offset(
                                size.width * AuroraMotion.ButtonGradientStartOffsetFraction + shift,
                                0f
                            ),
                            end = Offset(
                                size.width * AuroraMotion.ButtonGradientEndOffsetFraction + shift,
                                size.height
                            )
                        )
                    )
                }
            }
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = indication,
                role = Role.Button,
                onClick = onClick
            )
            .padding(contentPadding)
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (enabled) AuroraColors.BgPrimary
                                    else AuroraColors.TextMuted
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content
            )
        }
    }
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraOutlinedButton
//  Phase B keeps this static to avoid visual over-animation.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuroraOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentColor: Color = AuroraColors.Mint,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = contentColor,
            disabledContentColor = AuroraColors.TextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = contentColor.copy(
                alpha = if (enabled) AuroraMotion.OutlinedBorderEnabledAlpha
                        else AuroraMotion.OutlinedBorderDisabledAlpha
            )
        ),
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraSurface
//  Low-cost translucent glass surface (no backdrop blur for list cards).
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuroraSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 16.dp,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
            .border(
                width = 1.dp,
                color = AuroraColors.GlassBorder,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape = RoundedCornerShape(cornerRadius),
        color = AuroraColors.GlassBg,
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraIconTextButton
//  Icon + label row button.
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuroraIconTextButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = AuroraColors.Mint
) {
    AuroraOutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        contentColor = tint
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = tint
        )
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            fontWeight = FontWeight.Medium
        )
    }
}