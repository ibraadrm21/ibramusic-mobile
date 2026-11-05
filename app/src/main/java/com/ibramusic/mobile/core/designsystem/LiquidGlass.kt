package com.ibramusic.mobile.core.designsystem

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Liquid Glass & Frosted Glass modifiers and components inspired by:
 * - PrismalAGSL / AndroidLiquidGlass (specular highlights, refraction gradients)
 * - Haze (frosted floating surfaces)
 * - ArchiveTune & PixelPlayer (expressive dark mode & ambient artwork glow)
 */

fun Modifier.liquidGlass(
    shape: Shape = RoundedCornerShape(16.dp),
    baseColor: Color = Color(0x18FFFFFF),
    specularTopColor: Color = Color(0x33FFFFFF),
    specularBottomColor: Color = Color(0x0AFFFFFF),
    borderWidth: Dp = 1.dp
): Modifier = this
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(
                specularTopColor,
                baseColor,
                specularBottomColor
            )
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(
                Color(0x38FFFFFF),
                Color(0x14FFFFFF),
                Color(0x08FFFFFF)
            )
        ),
        shape = shape
    )

fun Modifier.ambientCoverGlow(
    glowColor: Color = Color(0x40FFFFFF),
    radiusDp: Dp = 160.dp
): Modifier = this.drawBehind {
    val glowBrush = Brush.radialGradient(
        colors = listOf(
            glowColor,
            glowColor.copy(alpha = glowColor.alpha * 0.4f),
            Color.Transparent
        ),
        center = Offset(size.width / 2f, size.height / 2f),
        radius = size.width.coerceAtLeast(size.height) * 0.85f
    )
    drawCircle(
        brush = glowBrush,
        radius = size.width.coerceAtLeast(size.height) * 0.85f,
        center = Offset(size.width / 2f, size.height / 2f)
    )
}

/**
 * Modern compact Liquid Pill for tabs, filters, and categories.
 */
@Composable
fun LiquidPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    count: Int? = null
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x28FFFFFF) else Color(0x0CFFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "pill_bg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x40FFFFFF) else Color(0x14FFFFFF),
        animationSpec = tween(durationMillis = 200),
        label = "pill_border"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else Color(0xFF9CA3AF),
        animationSpec = tween(durationMillis = 200),
        label = "pill_text"
    )

    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(backgroundColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = textColor
                )
            )
            if (count != null && count > 0) {
                Text(
                    text = " $count",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        color = textColor.copy(alpha = 0.7f)
                    )
                )
            }
        }
    }
}

