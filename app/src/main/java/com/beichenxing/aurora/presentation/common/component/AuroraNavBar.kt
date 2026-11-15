package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beichenxing.aurora.presentation.common.theme.AuroraColors

data class AuroraNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

/**
 * Aurora floating pill navigation bar.
 *
 * Shape: compact pill, width wraps content (not full-width).
 * Selection: inner pill highlight on selected item.
 * Height: ~56dp.
 *
 * Phase A: solid BgSoft + Border + Mint highlight
 * TODO Phase B:
 *   - Background: RenderEffect blur (Android 12+) + GlassBg
 *   - Selection pill: AuroraAnimatedGradient brush
 *   - Entrance: slide up animation on first composition
 */
@Composable
fun AuroraNavBar(
    items: List<AuroraNavItem>,
    currentRoute: String?,
    onItemClick: (AuroraNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    // Outer pill container — wraps content width, centered
    Box(
        modifier = modifier.padding(bottom = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .wrapContentWidth()
                .clip(RoundedCornerShape(100.dp))
                // TODO Phase B: replace with blur + GlassBg
                .background(AuroraColors.BgSoft)
                .border(
                    width = 1.dp,
                    // TODO Phase B: GlassBorder
                    color = AuroraColors.Border,
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    item: AuroraNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(250),
        label = "scale_${item.route}"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                // Inner pill for selected item
                // TODO Phase B: AuroraAnimatedGradient brush
                if (isSelected) AuroraColors.Mint.copy(alpha = 0.20f)
                else AuroraColors.Transparent
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(
                horizontal = if (isSelected) 20.dp else 18.dp,
                vertical = 10.dp
            ),
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
                tint = if (isSelected) AuroraColors.Mint else AuroraColors.TextMuted
            )
            Text(
                text = item.label,
                fontSize = 10.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isSelected) AuroraColors.Mint else AuroraColors.TextMuted
            )
        }
    }
}