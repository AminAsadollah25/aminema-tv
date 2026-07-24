package com.amin.tvos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Cinematic palette
val Ink = Color(0xFF0B0B0F)
val SurfaceDark = Color(0xFF16161D)
val SurfaceElevated = Color(0xFF1F1F29)
val CinemaRed = Color(0xFFE50914)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9E9EA8)

private val DarkColors = darkColorScheme(
    primary = CinemaRed,
    onPrimary = Color.White,
    secondary = TextPrimary,
    background = Ink,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    error = CinemaRed
)

// TV-friendly, larger-than-phone typography
private val TvTypography = Typography(
    displayMedium = TextStyle(fontSize = 42.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp),
    headlineMedium = TextStyle(fontSize = 26.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 18.sp),
    bodyMedium = TextStyle(fontSize = 16.sp),
    labelLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
)

@Composable
fun AminTvTheme(content: @Composable () -> Unit) {
    // Always dark — cinema style
    MaterialTheme(
        colorScheme = DarkColors,
        typography = TvTypography,
        content = content
    )
}
