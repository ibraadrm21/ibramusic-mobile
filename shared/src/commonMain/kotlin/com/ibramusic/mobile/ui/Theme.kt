package com.ibramusic.mobile.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NoirBlack = Color(0xFF0A0A0A)
val DarkSurface = Color(0xFF121212)
val DarkCard = Color(0xFF181818)
val DarkElevated = Color(0xFF222222)
val BrandEmerald = Color(0xFF10B981)
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextTertiary = Color(0xFF6B7280)
val BorderActive = Color(0x33FFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = BrandEmerald,
    background = NoirBlack,
    surface = DarkSurface,
    onPrimary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun IbraMusicSharedTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

