package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SecondaryContainerViolet, // D0BCFF
    onPrimary = OnPrimaryPurpleContainer, // 21005D
    primaryContainer = PrimaryPurple, // 6750A4
    onPrimaryContainer = PrimaryContainerPurple, // EADDFF
    secondary = SecondaryViolet,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = DarkOnSurface,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryPurple, // 6750A4
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = PrimaryContainerPurple, // EADDFF
    onPrimaryContainer = OnPrimaryPurpleContainer, // 21005D
    secondary = SecondaryViolet,
    secondaryContainer = SecondaryContainerViolet, // D0BCFF
    onSecondaryContainer = OnSecondaryVioletContainer, // 21005D
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic colors to ensure our stylized geometric palette is actively forced
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
