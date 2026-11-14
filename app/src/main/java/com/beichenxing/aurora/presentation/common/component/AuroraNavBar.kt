package com.beichenxing.aurora.presentation.common.component

import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
 * Aurora floating capsule navigation bar.
 *
 * Phase A: solid BgSoft background with Border outline + animated selection indicator
 * TODO Phase B:
 *   - Replace background with RenderEffect blur (Android 12+) + GlassBg
 *   - Replace selection capsule solid color with AuroraAnimatedGradient brush
 *   - Add enter animation (slide up from bottom on first composition)
 */
@Composable
fun AuroraNavBar(
    items: List<AuroraNavItem>,
    currentRoute: String?,
    onItemClick: (AuroraNavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Capsule container
        // TODO Phase B: add BlurMask / RenderEffect here
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(AuroraColors.BgSoft)  // TODO Phase B: GlassBg
                .border(
                    width = 1.dp,
                    color = AuroraColors.Border,   // TODO Phase B: GlassBorder
                    shape = RoundedCornerShape(100.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = currentRoute == item.route
                NavBarItem(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onItemClick(item) }
                )
            }
        }
    }
}

@Composable
private fun NavBarItem(
    item: AuroraNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    // Animate icon scale on selection
    val iconScale by animateFloatAsState(
        targetValue = if (isSelected) 1.1f else 1.0f,
        animationSpec = tween(durationMillis = 250),
        label = "iconScale_${item.route}"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .background(
                // Selection highlight capsule
                // TODO Phase B: replace with AuroraAnimatedGradient brush
                color = if (isSelected) AuroraColors.Mint.copy(alpha = 0.18f)
                        else AuroraColors.Transparent
            )
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                    .size(22.dp)
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