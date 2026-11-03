package com.ibramusic.mobile.core.designsystem

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Blueprint Técnico 1:1 "ibraStream" Noir Tokens
val NoirBlack = Color(0xFF000000) // Pure Black (#000000)
val DarkCanvas = Color(0xFF000000) // Pure Black base
val BarBackground = Color(0xF50F0F0F) // rgba(15, 15, 15, 0.96)
val DarkGlass = Color(0xF50F0F0F)
val DarkCard = Color(0xFF0D0D0D) // rgba(255, 255, 255, 0.05) / #0d0d0d
val ChipContainer = Color(0x12FFFFFF) // rgba(255, 255, 255, 0.07)
val DarkGlassBorder = Color(0x0DFFFFFF) // Subtle border: rgba(255, 255, 255, 0.05)
val BorderActive = Color(0x1AFFFFFF) // Active border: rgba(255, 255, 255, 0.10)

// Text tokens
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFF9CA3AF) // text-gray-400
val TextTertiary = Color(0xFF6B7280) // text-gray-500

val Zinc50 = Color(0xFFFFFFFF)
val Zinc400 = Color(0xFF9CA3AF)
val Zinc600 = Color(0xFF6B7280)
val Zinc700 = Color(0xFF3F3F46)
val Zinc800 = Color(0xFF27272A)

// Accent tokens
val AccentRose = Color(0xFFF43F5E) // Favoritos (#f43f5e)
val AccentYellow = Color(0xFFEAB308) // Advertencias (#eab308)
val BrandEmerald = Color(0xFF10B981) // Sincronización (#10b981)
val BrandIndigo = Color(0xFF6366F1)

val AccentWhite = Color(0xFFFFFFFF)
val AccentDimmed = Color(0xFF9CA3AF)

// Hero Card Gradient: linear-gradient(145deg, rgba(25, 25, 25, 0.92) 0%, rgba(10, 10, 10, 0.98) 100%)
val HeroCardGradient = Brush.linearGradient(
    colors = listOf(
        Color(0xEB191919),
        Color(0xFA0A0A0A)
    )
)

val IbraDarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    primaryContainer = DarkCard,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    onSecondary = Color.Black,
    background = NoirBlack,
    onBackground = TextPrimary,
    surface = BarBackground,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkGlassBorder
)
