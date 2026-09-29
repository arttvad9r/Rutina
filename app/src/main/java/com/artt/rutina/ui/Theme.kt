package com.artt.rutina.ui

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

// Спокойная, «бумажная» палитра: мягкий зелёный акцент и никаких ярких пятен.
// ВАЖНО: заполнены ВСЕ роли, которые используют компоненты M3 (чипы, меню, диалоги).
// Незаполненные роли берутся из базовой схемы M3 — она фиолетовая, и акцент ломается.
private val Moss = Color(0xFF4C7A5A)
private val MossDark = Color(0xFF9CC7A7)
private val Clay = Color(0xFFB4715A)
private val Ink = Color(0xFF23262B)
private val Paper = Color(0xFFFBFBF9)

private val LightColors = lightColorScheme(
    primary = Moss,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEBDF),
    onPrimaryContainer = Color(0xFF1F3325),
    inversePrimary = MossDark,

    secondary = Clay,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEADFD8),
    onSecondaryContainer = Color(0xFF3A241C),

    tertiary = Color(0xFF5B6B7A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFDFE6EC),
    onTertiaryContainer = Color(0xFF1E2830),

    background = Paper,
    onBackground = Ink,

    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEBEBE5),
    // было 5A5F66 — контраст к белому 6.0:1, на границе; затемнено для читаемости подписей
    onSurfaceVariant = Color(0xFF4C5158),

    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF7F7F4),
    surfaceContainer = Color(0xFFF3F3EF),
    surfaceContainerHigh = Color(0xFFEDEDE8),
    surfaceContainerHighest = Color(0xFFE7E7E2),

    outline = Color(0xFF7A7F7C),
    outlineVariant = Color(0xFFC4C8C2),

    error = Color(0xFF9E4B3E),
    onError = Color.White,
    errorContainer = Color(0xFFF5DDD8),
    onErrorContainer = Color(0xFF3E1A14),

    inverseSurface = Color(0xFF2F3336),
    inverseOnSurface = Color(0xFFF1F1ED),
    scrim = Color(0xFF000000),
)

private val DarkColors = darkColorScheme(
    primary = MossDark,
    onPrimary = Color(0xFF13251A),
    primaryContainer = Color(0xFF2C4434),
    onPrimaryContainer = Color(0xFFDCEBDF),
    inversePrimary = Moss,

    secondary = Color(0xFFD9A08C),
    onSecondary = Color(0xFF3A241C),
    secondaryContainer = Color(0xFF4A3A33),
    onSecondaryContainer = Color(0xFFEADFD8),

    tertiary = Color(0xFFAEBECD),
    onTertiary = Color(0xFF1E2830),
    tertiaryContainer = Color(0xFF343F49),
    onTertiaryContainer = Color(0xFFDFE6EC),

    background = Color(0xFF15171A),
    onBackground = Color(0xFFE8E9E6),

    surface = Color(0xFF1C1F23),
    onSurface = Color(0xFFE8E9E6),
    surfaceVariant = Color(0xFF272B30),
    onSurfaceVariant = Color(0xFFC2C6C2),

    surfaceContainerLowest = Color(0xFF16191C),
    surfaceContainerLow = Color(0xFF1C1F23),
    surfaceContainer = Color(0xFF212529),
    surfaceContainerHigh = Color(0xFF272B30),
    surfaceContainerHighest = Color(0xFF2E3338),

    outline = Color(0xFF6E7370),
    outlineVariant = Color(0xFF3A3F42),

    error = Color(0xFFE0A199),
    onError = Color(0xFF3E1A14),
    errorContainer = Color(0xFF5A2A22),
    onErrorContainer = Color(0xFFF5DDD8),

    inverseSurface = Color(0xFFE8E9E6),
    inverseOnSurface = Color(0xFF2F3336),
    scrim = Color(0xFF000000),
)

// Компактная типографика: всё должно укладываться на экран без прокрутки.
private val AppTypography = Typography(
    displaySmall = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.SemiBold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 19.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 21.sp, fontWeight = FontWeight.Medium),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 21.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(fontSize = 13.5.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 11.5.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = 10.5.sp, lineHeight = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun RutinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}
