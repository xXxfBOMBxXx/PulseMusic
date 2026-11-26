package com.pulsemusic.player.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pulsemusic.player.model.AppThemeStyle

private val PulseTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.25).sp),
    titleLarge = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium)
)

@Composable
fun PulseMusicTheme(
    themeStyle: AppThemeStyle,
    dynamicArtTheme: Boolean,
    dominantPrimaryArgb: Int,
    dominantSecondaryArgb: Int,
    content: @Composable () -> Unit
) {
    val basePrimary: Color
    val baseSecondary: Color
    val bg: Color
    val surface: Color
    val surfaceVariant: Color

    when (themeStyle) {
        AppThemeStyle.MIDNIGHT_PULSE -> {
            basePrimary = Color(0xFF8C62FF)
            baseSecondary = Color(0xFF00E5FF)
            bg = Color(0xFF090B14)
            surface = Color(0xFF121526)
            surfaceVariant = Color(0xFF1C2038)
        }
        AppThemeStyle.AMOLED_BLACK -> {
            basePrimary = Color(0xFF9D7AFF)
            baseSecondary = Color(0xFF26E8FF)
            bg = Color(0xFF000000)
            surface = Color(0xFF0C0C10)
            surfaceVariant = Color(0xFF171720)
        }
        AppThemeStyle.AURORA_VIOLET -> {
            basePrimary = Color(0xFFD966FF)
            baseSecondary = Color(0xFF7C4DFF)
            bg = Color(0xFF0E0817)
            surface = Color(0xFF19102A)
            surfaceVariant = Color(0xFF26193E)
        }
        AppThemeStyle.OCEAN_CYAN -> {
            basePrimary = Color(0xFF00D8FF)
            baseSecondary = Color(0xFF38EF7D)
            bg = Color(0xFF061018)
            surface = Color(0xFF0E1D2A)
            surfaceVariant = Color(0xFF162C3D)
        }
    }

    val effectivePrimary = if (dynamicArtTheme) {
        lerp(basePrimary, Color(dominantPrimaryArgb), 0.35f)
    } else {
        basePrimary
    }
    val effectiveSecondary = if (dynamicArtTheme) {
        lerp(baseSecondary, Color(dominantSecondaryArgb), 0.30f)
    } else {
        baseSecondary
    }

    val colorScheme = darkColorScheme(
        primary = effectivePrimary,
        onPrimary = Color(0xFF07070B),
        primaryContainer = effectivePrimary.copy(alpha = 0.22f),
        onPrimaryContainer = Color.White,
        secondary = effectiveSecondary,
        onSecondary = Color(0xFF050A0E),
        secondaryContainer = effectiveSecondary.copy(alpha = 0.18f),
        onSecondaryContainer = Color.White,
        background = bg,
        onBackground = Color(0xFFF3F5FF),
        surface = surface,
        onSurface = Color(0xFFF3F5FF),
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = Color(0xFFA9B2CC),
        outline = Color(0xFF2D3553),
        error = Color(0xFFFF5A79)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = PulseTypography,
        content = content
    )
}
