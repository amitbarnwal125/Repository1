package com.swara.music.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Saffron, rose and deep plum: warm "film poster" colours.
private val Dark = darkColorScheme(
    primary = Color(0xFFFFB25E),
    onPrimary = Color(0xFF3B1E00),
    primaryContainer = Color(0xFF5C3100),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFFF8AC0),
    onSecondary = Color(0xFF4C0B2E),
    secondaryContainer = Color(0xFF6B1D47),
    onSecondaryContainer = Color(0xFFFFD8E7),
    tertiary = Color(0xFF7FD8C6),
    background = Color(0xFF140B18),
    onBackground = Color(0xFFF1E3EE),
    surface = Color(0xFF140B18),
    onSurface = Color(0xFFF1E3EE),
    surfaceVariant = Color(0xFF2B1D31),
    onSurfaceVariant = Color(0xFFD4C1D0),
    surfaceContainer = Color(0xFF1F1424),
    surfaceContainerHigh = Color(0xFF2A1D2F),
    surfaceContainerHighest = Color(0xFF35273A),
    surfaceContainerLow = Color(0xFF1A101E),
    outline = Color(0xFF9D8B9A),
    outlineVariant = Color(0xFF4D3D52),
    error = Color(0xFFFFB4AB),
)

private val Light = lightColorScheme(
    primary = Color(0xFF9A4F00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCBE),
    onPrimaryContainer = Color(0xFF311300),
    secondary = Color(0xFF9C2A6B),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD8E7),
    onSecondaryContainer = Color(0xFF3E0024),
    tertiary = Color(0xFF006B5D),
    background = Color(0xFFFFF8F6),
    onBackground = Color(0xFF221A1F),
    surface = Color(0xFFFFF8F6),
    onSurface = Color(0xFF221A1F),
    surfaceVariant = Color(0xFFF3DDE8),
    onSurfaceVariant = Color(0xFF524250),
    surfaceContainer = Color(0xFFF8EBF0),
    surfaceContainerHigh = Color(0xFFF2E4EA),
    surfaceContainerHighest = Color(0xFFECDEE4),
    surfaceContainerLow = Color(0xFFFCF0F4),
    outline = Color(0xFF847280),
    outlineVariant = Color(0xFFD6C1CF),
)

private val base = Typography()
private val AppTypography = base.copy(
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
)

@Composable
fun SwaraTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        typography = AppTypography,
        content = content,
    )
}

/** Deterministic warm gradients for tracks without cover art. */
val ArtPalettes = listOf(
    listOf(Color(0xFFE8711A), Color(0xFF8E1F5E)),
    listOf(Color(0xFFB8336A), Color(0xFF3F1D63)),
    listOf(Color(0xFF1F7A6D), Color(0xFF173A5E)),
    listOf(Color(0xFFD4A017), Color(0xFFB0412E)),
    listOf(Color(0xFF6A3093), Color(0xFFA044FF)),
    listOf(Color(0xFFC0392B), Color(0xFF5B1A2C)),
    listOf(Color(0xFF2E86AB), Color(0xFF6C2E8C)),
    listOf(Color(0xFF8C5E3C), Color(0xFF3A2416)),
)

@Suppress("unused")
private val unusedStyle = TextStyle.Default
