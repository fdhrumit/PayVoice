package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = PayVoicePrimary,
    onPrimary = Color.White,
    primaryContainer = PayVoicePrimaryContainer,
    onPrimaryContainer = PayVoiceOnPrimaryContainer,
    secondary = PayVoiceSecondary,
    onSecondary = Color.White,
    secondaryContainer = PayVoiceSecondaryContainer,
    onSecondaryContainer = PayVoiceOnSecondaryContainer,
    tertiary = PayVoiceIncomeGreen,
    onTertiary = Color.White,
    tertiaryContainer = PayVoiceIncomeGreenContainer,
    onTertiaryContainer = PayVoiceIncomeGreenDark,
    background = PayVoiceBackground,
    onBackground = PayVoiceOnSurface,
    surface = PayVoiceSurface,
    onSurface = PayVoiceOnSurface,
    surfaceVariant = PayVoiceSurfaceVariant,
    onSurfaceVariant = PayVoiceOnSurfaceVariant,
    outline = PayVoiceOutline,
    outlineVariant = PayVoiceOutlineVariant,
    error = PayVoiceError,
    errorContainer = PayVoiceErrorContainer,
    onError = Color.White,
    onErrorContainer = Color(0xFF410002)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD6B5FF),
    onPrimary = Color(0xFF410086),
    primaryContainer = PayVoicePrimary,
    onPrimaryContainer = Color(0xFFF1E5FC),
    secondary = Color(0xFFDAB6FF),
    onSecondary = Color(0xFF48146F),
    secondaryContainer = PayVoiceSecondary,
    onSecondaryContainer = Color(0xFFF5ECFD),
    tertiary = Color(0xFF6BDB9B),
    onTertiary = Color(0xFF00391F),
    background = Color(0xFF131118),
    onBackground = Color(0xFFE6E1EB),
    surface = Color(0xFF1B1921),
    onSurface = Color(0xFFE6E1EB),
    surfaceVariant = Color(0xFF282531),
    onSurfaceVariant = Color(0xFFCAC4D0),
    outline = Color(0xFF645E70)
)

@Composable
fun PayVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
