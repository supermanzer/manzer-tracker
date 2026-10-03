package com.supermanzer.manzertracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CoffeeDarkColorScheme = darkColorScheme(
    primary = CoffeeDarkPrimary,
    secondary = CoffeeDarkSecondary,
    background = CoffeeDarkBackground,
    surface = CoffeeDarkSurface,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val CoffeeLightColorScheme = lightColorScheme(
    primary = CoffeeLightPrimary,
    secondary = CoffeeLightSecondary,
    background = CoffeeLightBackground,
    surface = CoffeeLightSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun BrewBuddyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CoffeeDarkColorScheme else CoffeeLightColorScheme,
        typography = Typography,
        content = content
    )
}
