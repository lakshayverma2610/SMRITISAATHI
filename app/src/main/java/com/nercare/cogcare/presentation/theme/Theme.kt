package com.nercare.cogcare.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CogCareLightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = SecondaryGreen,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SecondaryGreen,
    onSecondary = TextPrimaryDark,
    secondaryContainer = TertiaryGreen,
    onSecondaryContainer = TextPrimaryDark,
    background = BackgroundCream,
    onBackground = TextPrimaryDark,
    surface = SurfaceWhite,
    onSurface = TextPrimaryDark,
    surfaceVariant = TertiaryGreen,
    onSurfaceVariant = TextSecondaryMuted,
    error = ErrorRed,
    onError = SurfaceWhite,
)

@Composable
fun CogCareTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CogCareLightColorScheme,
        typography = ElderlyFriendlyTypography,
        content = content
    )
}
