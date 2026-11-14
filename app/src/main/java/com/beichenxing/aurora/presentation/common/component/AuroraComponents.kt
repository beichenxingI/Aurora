package com.beichenxing.aurora.presentation.common.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.beichenxing.aurora.presentation.common.theme.AuroraColors

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraButton
//  Phase A: solid Mint background
//  TODO Phase B: replace background with animated AuroraGradient brush
// ═════════════════════════════════════════════════════════════════════════════
@Composable
fun AuroraButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        contentPadding = contentPadding,
        colors = ButtonDefaults.buttonColors(
            // Phase A: solid accent
            // TODO Phase B: use Box + AuroraAnimatedGradient brush as background
            containerColor = AuroraColors.Mint,
            contentColor   = AuroraColors.BgPrimary,
            disabledContainerColor = AuroraColors.Border,
            disabledContentColor   = AuroraColors.TextMuted
        ),
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraOutlinedButton
//  Phase A: bordered with Mint outline
//  TODO Phase B: animated gradient border
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
            contentColor   = contentColor,
            disabledContentColor = AuroraColors.TextMuted
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            // TODO Phase B: replace with animated gradient border brush
            color = contentColor.copy(alpha = if (enabled) 0.6f else 0.2f)
        ),
        content = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraSurface
//  Phase A: solid BgSoft + Border outline
//  TODO Phase B: RenderEffect blur + GlassBg + GlassBorder
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
                // TODO Phase B: replace with GlassBorder
                color = AuroraColors.Border,
                shape = RoundedCornerShape(cornerRadius)
            ),
        shape    = RoundedCornerShape(cornerRadius),
        // TODO Phase B: replace with GlassBg + blur
        color    = AuroraColors.BgSoft,
        content  = content
    )
}

// ═════════════════════════════════════════════════════════════════════════════
//  AuroraIconTextButton
//  Icon + label vertical layout, used in action rows (pin, delete, etc.)
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