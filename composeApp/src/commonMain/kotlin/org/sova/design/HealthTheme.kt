package org.sova.design

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val HealthColorScheme: ColorScheme = lightColorScheme(
    primary = HealthColors.Accent,
    onPrimary = HealthColors.Surface,
    background = HealthColors.Background,
    onBackground = HealthColors.TextPrimary,
    surface = HealthColors.Surface,
    onSurface = HealthColors.TextPrimary,
    surfaceVariant = HealthColors.SurfaceSubtle,
    onSurfaceVariant = HealthColors.TextSecondary,
    outline = HealthColors.Border,
    error = HealthColors.Danger,
)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HealthColorScheme,
        typography = healthTypography(),
        content = content,
    )
}
