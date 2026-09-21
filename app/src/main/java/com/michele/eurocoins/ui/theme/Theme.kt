package com.michele.eurocoins.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = VerdigrisLight,
    onPrimary = SurfaceLight,
    secondary = BronzeLight,
    background = BackgroundLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = BackgroundLight,
    secondaryContainer = LilacLight,
    onSecondaryContainer = InkLight,
    onSurfaceVariant = InkLight,
    outline = OutlineLight,
)

private val DarkColors = darkColorScheme(
    primary = VerdigrisDark,
    onPrimary = InkLight,
    secondary = BronzeDark,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = BackgroundDark,
    onSurfaceVariant = InkDark,
    outline = OutlineDark,
)

@Composable
fun EuroCoinsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EuroCoinsTypography,
        content = content,
    )
}
