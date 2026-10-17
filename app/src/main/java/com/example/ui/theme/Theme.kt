package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = CidPrimaryBlue,
    onPrimary = Color.White,
    secondary = CidSecondaryCyan,
    onSecondary = Color.Black,
    tertiary = CidPayGold,
    background = CidDarkBackground,
    onBackground = Color.White,
    surface = CidDarkSurface,
    onSurface = Color.White,
    surfaceVariant = CidDarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = CidPrimaryBlue,
    onPrimary = Color.White,
    secondary = Color(0xFF0284C7),
    onSecondary = Color.White,
    tertiary = CidPayGold,
    background = CidLightBackground,
    onBackground = Color(0xFF0F172A),
    surface = CidLightSurface,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = CidLightSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569)
)

@Composable
fun CIDTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    CIDTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, content = content)
}

