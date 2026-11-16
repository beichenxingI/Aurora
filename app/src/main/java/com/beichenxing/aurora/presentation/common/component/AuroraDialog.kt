package com.beichenxing.aurora.presentation.common.component

import android.os.Build
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import com.beichenxing.aurora.presentation.common.theme.rememberSystemAnimationsEnabled

/**
 * Glass dialog with platform blur on Android 12+ and a translucent scrim fallback
 * on older versions. The content width is capped for tablets and landscape.
 */
@Composable
fun AuroraDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedScale by animateFloatAsState(
        targetValue = if (visible) 1f else AuroraMotion.DialogEnterScaleFrom,
        animationSpec = spring(
            dampingRatio = AuroraMotion.NavSpringDampingRatio,
            stiffness = AuroraMotion.NavSpringStiffness
        ),
        label = "aurora_dialog_scale"
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = AuroraMotion.DialogEnterDurationMs),
        label = "aurora_dialog_alpha"
    )
    val scale = if (animationsEnabled) animatedScale else 1f
    val alpha = if (animationsEnabled) animatedAlpha else 1f

    val density = LocalDensity.current
    val dialogShape = RoundedCornerShape(AuroraMotion.DialogCornerRadius)

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        val dialogWindow = findDialogWindow(LocalView.current)
        SideEffect {
            dialogWindow?.setBackgroundDrawableResource(android.R.color.transparent)
            dialogWindow?.setDimAmount(0f)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialogWindow?.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
                dialogWindow?.attributes = dialogWindow.attributes.apply {
                    blurBehindRadius = with(density) {
                        AuroraMotion.DialogBlurRadius.roundToPx()
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AuroraColors.DialogScrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = modifier
                    .fillMaxWidth(AuroraMotion.DialogWidthFraction)
                    .widthIn(max = AuroraMotion.DialogMaxWidth)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        this.alpha = alpha
                    }
                    .shadow(
                        elevation = AuroraMotion.NavShadowRadius,
                        shape = dialogShape,
                        clip = false,
                        ambientColor = AuroraColors.Shadow,
                        spotColor = AuroraColors.Shadow
                    )
                    .clip(dialogShape)
                    .background(AuroraColors.GlassBg)
                    .border(
                        width = 1.dp,
                        color = AuroraColors.GlassBorder,
                        shape = dialogShape
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(content = content)

                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(AuroraMotion.GlassHighlightHeight)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    AuroraColors.Transparent,
                                    AuroraColors.GlassTopHighlight,
                                    AuroraColors.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

private fun findDialogWindow(view: View): Window? {
    var current: View? = view
    while (current != null) {
        if (current is DialogWindowProvider) return current.window
        current = current.parent as? View
    }
    return null
}