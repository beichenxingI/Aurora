package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.launch

data class AuroraNavItem(
    val route: String,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Aurora floating pill navigation bar.
 *
 * - Outer capsule: full-width pill, hazeChild for glass effect
 * - Selected indicator: single shared capsule that spring-animates
 *   to target item position and width
 * - Sweep: single one-shot diagonal sweep after indicator arrives
 */
@Composable
fun AuroraNavBar(
    items: List<AuroraNavItem>,
    currentRoute: String?,
    hazeState: HazeState,
    onItemClick: (AuroraNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // Per-item measured bounds (x offset and width in px, relative to Row)
    val itemOffsets = remember { mutableMapOf<String, Float>() }
    val itemWidths  = remember { mutableMapOf<String, Float>() }
    var rowHeight   by remember { mutableFloatStateOf(0f) }
    var measured    by remember { mutableStateOf(false) }

    // Spring-animated indicator position and width
    val indicatorX     = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }

    // Sweep animation (0f = not started, 1f = done)
    val sweepProgress = remember { Animatable(0f) }
    var lastRoute     by remember { mutableStateOf<String?>(null) }
    var firstLayout   by remember { mutableStateOf(true) }

    val navHeight = AuroraMotion.NavBarHeight

    // Move indicator when route changes
    LaunchedEffect(currentRoute, measured) {
        if (!measured || currentRoute == null) return@LaunchedEffect

        val targetX = itemOffsets[currentRoute] ?: return@LaunchedEffect
        val targetW = itemWidths[currentRoute]  ?: return@LaunchedEffect

        if (firstLayout) {
            // Snap to position on first layout, no animation
            indicatorX.snapTo(targetX)
            indicatorWidth.snapTo(targetW)
            firstLayout = false
            lastRoute = currentRoute
            return@LaunchedEffect
        }

        if (currentRoute == lastRoute) return@LaunchedEffect

        // Spring animate to new position
        launch {
            indicatorX.animateTo(
                targetValue = targetX,
                animationSpec = spring(
                    dampingRatio = AuroraMotion.NavSpringDampingRatio,
                    stiffness    = AuroraMotion.NavSpringStiffness
                )
            )
        }
        launch {
            indicatorWidth.animateTo(
                targetValue = targetW,
                animationSpec = spring(
                    dampingRatio = AuroraMotion.NavSpringDampingRatio,
                    stiffness    = AuroraMotion.NavSpringStiffness
                )
            )
        }

        // Sweep: wait for indicator to mostly arrive, then play once
        launch {
            sweepProgress.snapTo(0f)
            kotlinx.coroutines.delay(AuroraMotion.NavSweepDelayMs)
            sweepProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = AuroraMotion.NavSweepDurationMs)
            )
        }

        lastRoute = currentRoute
    }

    Box(
        modifier = modifier.padding(
            start  = AuroraMotion.NavBarFloatingHorizontalMargin,
            end    = AuroraMotion.NavBarFloatingHorizontalMargin,
            bottom = AuroraMotion.NavBarFloatingBottomMargin
        )
    ) {
        // Outer pill with haze glass effect
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(navHeight)
                .clip(RoundedCornerShape(100.dp))
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor  = AuroraColors.GlassBg,
                        tint             = dev.chrisbanes.haze.HazeTint(
                            color = AuroraColors.GlassWhiteTint
                        ),
                        blurRadius       = AuroraMotion.GlassBlurRadius
                    )
                )
                .border(
                    width = 1.dp,
                    color = AuroraColors.GlassBorder,
                    shape = RoundedCornerShape(100.dp)
                )
        ) {
            // ── Sliding indicator (drawn behind items) ────────────────────
            if (measured) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val x = indicatorX.value
                    val w = indicatorWidth.value
                    val h = size.height

                    // Indicator pill
                    drawRoundRect(
                        brush       = Brush.linearGradient(
                            colors = listOf(
                                AuroraColors.Mint.copy(
                                    alpha = AuroraMotion.NavIndicatorMintAlpha
                                ),
                                AuroraColors.Sky.copy(
                                    alpha = AuroraMotion.NavIndicatorSkyAlpha
                                )
                            ),
                            start = Offset(x, 0f),
                            end   = Offset(x + w, h)
                        ),
                        topLeft     = Offset(x, 0f),
                        size        = Size(w, h),
                        cornerRadius = CornerRadius(h / 2f)
                    )

                    // One-shot sweep highlight clipped to indicator
                    val sweep = sweepProgress.value
                    if (sweep > 0f && sweep < 1f) {
                        clipRect(left = x, top = 0f, right = x + w, bottom = h) {
                            val sweepCenter =
                                x - w * AuroraMotion.NavSweepLeadFraction +
                                    sweep * (w * AuroraMotion.NavSweepTravelFraction)
                            val sweepW = w * AuroraMotion.NavSweepBandFraction
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AuroraColors.Transparent,
                                        AuroraColors.NavSweep,
                                        AuroraColors.Transparent
                                    ),
                                    start = Offset(sweepCenter - sweepW, 0f),
                                    end   = Offset(sweepCenter + sweepW, h)
                                ),
                                topLeft      = Offset(x, 0f),
                                size         = Size(w, h),
                                cornerRadius = CornerRadius(h / 2f)
                            )
                        }
                    }
                }
            }

            // ── Nav items ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .onGloballyPositioned { coords ->
                        rowHeight = coords.size.height.toFloat()
                    },
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val isSelected = currentRoute == item.route
                    val iconScale by animateFloatAsState(
                        targetValue    = if (isSelected) AuroraMotion.NavIconSelectedScale else 1f,
                        animationSpec  = spring(stiffness = AuroraMotion.NavIconSpringStiffness),
                        label          = "iconScale_${item.route}"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onGloballyPositioned { coords ->
                                val pos = coords.positionInParent()
                                itemOffsets[item.route] = pos.x
                                itemWidths[item.route]  = coords.size.width.toFloat()
                                if (itemOffsets.size == items.size &&
                                    itemWidths.size  == items.size
                                ) {
                                    measured = true
                                }
                            }
                            .clickable(
                                indication     = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick        = { onItemClick(item) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector        = if (isSelected) item.selectedIcon
                                                     else item.unselectedIcon,
                                contentDescription = item.label,
                                modifier           = Modifier
                                    .size(22.dp)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                tint               = if (isSelected) AuroraColors.Mint
                                                     else AuroraColors.TextMuted
                            )
                            Text(
                                text       = item.label,
                                fontSize   = 10.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold
                                             else FontWeight.Normal,
                                color      = if (isSelected) AuroraColors.TextPrimary
                                             else AuroraColors.TextMuted
                            )
                        }
                    }
                }
            }
        }
    }
}