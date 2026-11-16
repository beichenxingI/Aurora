package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beichenxing.aurora.presentation.common.theme.AuroraColors
import com.beichenxing.aurora.presentation.common.theme.AuroraMotion
import com.beichenxing.aurora.presentation.common.theme.rememberSystemAnimationsEnabled
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeChild
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class AuroraNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Aurora floating glass pill navigation bar.
 *
 * - Real backdrop blur through Haze (with translucent fallback).
 * - One shared selection indicator, spring-animated x/width.
 * - A single restrained 45-degree sweep after a real route change.
 * - System animation scale is respected.
 */
@Composable
fun AuroraNavBar(
    items: List<AuroraNavItem>,
    currentRoute: String?,
    hazeState: HazeState,
    onItemClick: (AuroraNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationsEnabled = rememberSystemAnimationsEnabled()
    val density = LocalDensity.current
    val navBottomInset = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()

    val itemBounds = remember { mutableStateMapOf<String, Rect>() }
    val indicatorX = remember { Animatable(0f) }
    val indicatorWidth = remember { Animatable(0f) }
    val sweep = remember { Animatable(0f) }

    var measured by remember { mutableStateOf(false) }
    var lastRoute by remember { mutableStateOf<String?>(null) }

    val selectedBounds = currentRoute?.let { itemBounds[it] }
    val selectedHeight = selectedBounds?.height ?: 0f

    LaunchedEffect(currentRoute, selectedBounds) {
        val bounds = selectedBounds ?: return@LaunchedEffect

        if (!measured) {
            indicatorX.snapTo(bounds.left)
            indicatorWidth.snapTo(bounds.width)
            measured = true
            lastRoute = currentRoute
            return@LaunchedEffect
        }

        val routeChanged = lastRoute != null && lastRoute != currentRoute
        if (routeChanged) {
            sweep.snapTo(0f)
            lastRoute = currentRoute
        }

        if (animationsEnabled) {
            coroutineScope {
                launch {
                    indicatorX.animateTo(
                        targetValue = bounds.left,
                        animationSpec = spring(
                            dampingRatio = AuroraMotion.NavSpringDampingRatio,
                            stiffness = AuroraMotion.NavSpringStiffness
                        )
                    )
                }
                launch {
                    indicatorWidth.animateTo(
                        targetValue = bounds.width,
                        animationSpec = spring(
                            dampingRatio = AuroraMotion.NavSpringDampingRatio,
                            stiffness = AuroraMotion.NavSpringStiffness
                        )
                    )
                }
            }
            if (routeChanged) {
                sweep.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = AuroraMotion.NavSweepDurationMs,
                        easing = LinearEasing
                    )
                )
            }
        } else {
            indicatorX.snapTo(bounds.left)
            indicatorWidth.snapTo(bounds.width)
        }
    }

    Box(
        modifier = modifier.padding(
            bottom = AuroraMotion.NavBarBottomMargin + navBottomInset
        ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(AuroraMotion.NavBarWidthFraction)
                .widthIn(
                    min = AuroraMotion.NavBarMinWidth,
                    max = AuroraMotion.NavBarMaxWidth
                )
                .height(AuroraMotion.NavBarHeight)
                .shadow(
                    elevation = AuroraMotion.NavShadowRadius,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = AuroraColors.Shadow,
                    spotColor = AuroraColors.Shadow
                )
                .clip(CircleShape)
                .hazeChild(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = AuroraColors.Transparent,
                        tints = listOf(HazeTint(AuroraColors.GlassWhiteTint)),
                        blurRadius = AuroraMotion.GlassBlurRadius,
                        noiseFactor = 0f,
                        fallbackTint = HazeTint(AuroraColors.GlassBg)
                    )
                ) {
                }
                .border(
                    width = 1.dp,
                    color = AuroraColors.GlassBorder,
                    shape = CircleShape
                )
        ) {
            // Single shared selection indicator, behind the items.
            if (measured && selectedBounds != null && selectedHeight > 0f) {
                val indicatorTop = selectedBounds.top
                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = indicatorX.value.roundToInt(),
                                y = indicatorTop.roundToInt()
                            )
                        }
                        .size(
                            width = with(density) { indicatorWidth.value.toDp() },
                            height = with(density) { selectedHeight.toDp() }
                        )
                        .clip(CircleShape)
                        .background(AuroraColors.AuroraSubtleGradient)
                        .background(AuroraColors.GlassWhiteTint)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val progress = sweep.value
                        if (progress <= 0f) return@Canvas

                        val bandWidth =
                            size.width * AuroraMotion.NavSweepWidthFraction
                        val travel = size.width + bandWidth * 2f
                        val centerX = -bandWidth + travel * progress

                        val sweepBrush = Brush.linearGradient(
                            colors = listOf(
                                AuroraColors.Transparent,
                                AuroraColors.NavSweep,
                                AuroraColors.Transparent
                            ),
                            start = Offset(
                                centerX - bandWidth,
                                size.height
                            ),
                            end = Offset(
                                centerX + bandWidth,
                                0f
                            )
                        )
                        drawRect(brush = sweepBrush)
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    NavBarItem(
                        item = item,
                        isSelected = currentRoute == item.route,
                        animationsEnabled = animationsEnabled,
                        onBoundsChanged = { rect ->
                            itemBounds[item.route] = rect
                        },
                        onClick = { onItemClick(item) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // One-pixel top inner highlight (does not wrap the full border).
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .height(1.dp)
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

@Composable
private fun NavBarItem(
    item: AuroraNavItem,
    isSelected: Boolean,
    animationsEnabled: Boolean,
    onBoundsChanged: (Rect) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val targetScale = if (isSelected) 1.1f else 1.0f
    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(
            durationMillis = AuroraMotion.NavIconScaleDurationMs
        ),
        label = "nav_icon_scale_${item.route}"
    )
    val iconScale = if (animationsEnabled) animatedScale else targetScale

    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .onGloballyPositioned { coordinates ->
                onBoundsChanged(coordinates.boundsInParent())
            }
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                role = Role.Tab,
                onClick = onClick
            )
            .semantics {
                selected = isSelected
                contentDescription = item.label
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(
                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                contentDescription = item.label,
                modifier = Modifier
                    .size(20.dp)
                    .scale(iconScale),
                tint = if (isSelected) AuroraColors.TextPrimary
                       else AuroraColors.TextMuted
            )
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold
                             else FontWeight.Normal,
                color = if (isSelected) AuroraColors.TextPrimary
                        else AuroraColors.TextMuted
            )
        }
    }
}