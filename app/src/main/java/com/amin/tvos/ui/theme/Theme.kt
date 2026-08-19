package com.amin.tvos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.amin.tvos.R

// Cinematic palette
val Ink = Color(0xFF0B0B0F)
val SurfaceDark = Color(0xFF16161D)
val SurfaceElevated = Color(0xFF1F1F29)
val CinemaRed = Color(0xFFE50914)
val TextPrimary = Color(0xFFF5F5F7)
val TextSecondary = Color(0xFF9E9EA8)

// A bundled typeface keeps Persian shaping consistent across Android TV boxes
// while also providing a cleaner Latin companion than the device default.
// Vazirmatn is open-source and covers both Persian and English, so mixed titles
// do not jump between unrelated fallback fonts.
private val AminemaFontFamily = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_semibold, FontWeight.Medium),
    Font(R.font.vazirmatn_semibold, FontWeight.SemiBold),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_bold, FontWeight.ExtraBold)
)

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
    displayMedium = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 42.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp,
        lineHeight = 50.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 34.sp
    ),
    titleLarge = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 29.sp
    ),
    titleMedium = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
        lineHeight = 25.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
        lineHeight = 28.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
        lineHeight = 24.sp
    ),
    labelLarge = TextStyle(
        fontFamily = AminemaFontFamily,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.1.sp,
        lineHeight = 22.sp
    )
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
